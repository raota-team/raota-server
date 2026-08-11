package com.raota.agent.application.retrieval;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raota.community.domain.model.Post;
import com.raota.community.domain.repository.PostRepository;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.ramenshop.domain.repository.RamenShopRepository;
import com.raota.agent.domain.retrieval.document.RetrievalDocumentFilters;
import com.raota.agent.domain.retrieval.document.RetrievalDocumentSource;
import com.raota.agent.domain.retrieval.document.RetrievalDocumentType;
import com.raota.agent.domain.retrieval.document.RetrievalMetadataKeys;
import com.raota.agent.domain.retrieval.document.factory.PostReviewChunkDocumentFactory;
import com.raota.agent.domain.retrieval.document.factory.RamenShopProfileDocumentFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oracle.jdbc.OracleType;
import oracle.sql.VECTOR;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RetrievalIndexingService {

    private static final int EXTERNAL_REVIEW_BATCH_SIZE = 64;
    private static final String EXTERNAL_REVIEW_UPSERT_SQL = """
            merge into SPRING_AI_VECTORS target
            using (select ? id, ? content, ? metadata, ? embedding from dual) source
            on (target.id = source.id)
            when matched then update set target.content = source.content, target.metadata = source.metadata, target.embedding = source.embedding
            when not matched then insert (target.id, target.content, target.metadata, target.embedding) values (source.id, source.content, source.metadata, source.embedding)
            """;
    private static final String SHOP_REVIEW_DOCUMENTS_SQL = """
            select
                content,
                json_serialize(metadata returning varchar2(32767)) as metadata_json
            from SPRING_AI_VECTORS
            where json_value(metadata, '$.shopId' returning varchar2(64)) = ?
              and json_value(metadata, '$.documentType' returning varchar2(64)) = ?
              and json_value(metadata, '$.source' returning varchar2(64)) in (?, ?)
            order by json_value(metadata, '$.createdAt' returning varchar2(64)) desc nulls last
            fetch first ? rows only
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OracleJsonFactory oracleJsonFactory = new OracleJsonFactory();

    private final RamenShopRepository ramenShopRepository;
    private final RamenShopProfileDocumentFactory ramenShopProfileDocumentFactory;
    private final PostRepository postRepository;
    private final PostReviewChunkDocumentFactory postReviewChunkDocumentFactory;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate oracleVectorJdbcTemplate;

    public RetrievalIndexingService(
            RamenShopRepository ramenShopRepository,
            RamenShopProfileDocumentFactory ramenShopProfileDocumentFactory,
            PostRepository postRepository,
            PostReviewChunkDocumentFactory postReviewChunkDocumentFactory,
            VectorStore vectorStore,
            EmbeddingModel embeddingModel,
            @Qualifier("oracleVectorJdbcTemplate") JdbcTemplate oracleVectorJdbcTemplate
    ) {
        this.ramenShopRepository = ramenShopRepository;
        this.ramenShopProfileDocumentFactory = ramenShopProfileDocumentFactory;
        this.postRepository = postRepository;
        this.postReviewChunkDocumentFactory = postReviewChunkDocumentFactory;
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.oracleVectorJdbcTemplate = oracleVectorJdbcTemplate;
    }

    public void indexAllShops() {
        List<RamenShop> shops = ramenShopRepository.findAllByPublishedTrue();

        for (RamenShop shop : shops) {
            addDocuments(ramenShopProfileDocumentFactory.create(shop));
        }
    }

    public void indexShop(Long shopId) {
        RamenShop shop = ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("라멘샵을 찾을 수 없습니다. id=" + shopId));

        addDocuments(ramenShopProfileDocumentFactory.create(shop));
    }

    public void deleteAllShops() {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        var filter = builder.and(
                builder.eq(
                        RetrievalMetadataKeys.DOCUMENT_TYPE,
                        RetrievalDocumentType.SHOP_PROFILE.name()
                ),
                builder.eq(
                        RetrievalMetadataKeys.SOURCE,
                        RetrievalDocumentSource.RAMEN_SHOP.name()
                )
        ).build();

        vectorStore.delete(filter);
    }

    public void deleteShop(Long shopId) {
        if (shopId == null) {
            return;
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        var filter = builder.and(
                builder.and(
                        builder.eq(
                                RetrievalMetadataKeys.DOCUMENT_TYPE,
                                RetrievalDocumentType.SHOP_PROFILE.name()
                        ),
                        builder.eq(
                                RetrievalMetadataKeys.SOURCE,
                                RetrievalDocumentSource.RAMEN_SHOP.name()
                        )
                ),
                builder.eq(
                        RetrievalMetadataKeys.SHOP_ID,
                        String.valueOf(shopId)
                )
        ).build();

        vectorStore.delete(filter);
    }

    public void indexPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

        RamenShop shop = post.getRamenShopId() == null
                ? null
                : ramenShopRepository.findById(post.getRamenShopId()).orElse(null);

        deletePost(postId);

        List<Document> documents = postReviewChunkDocumentFactory.create(post, shop);
        addDocuments(documents);
    }

    public void deletePost(Long postId) {
        if (postId == null) {
            return;
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        var filter = builder.and(
                builder.and(
                        builder.eq(
                                RetrievalMetadataKeys.DOCUMENT_TYPE,
                                RetrievalDocumentType.REVIEW_CHUNK.name()
                        ),
                        builder.eq(
                                RetrievalMetadataKeys.SOURCE,
                                RetrievalDocumentSource.COMMUNITY_POST.name()
                        )
                ),
                builder.eq(
                        RetrievalMetadataKeys.SOURCE_ID,
                        String.valueOf(postId)
                )
        ).build();

        vectorStore.delete(filter);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ExternalReviewIndexResult reindexCatchtableReviews(Path jsonlPath) {
        ExternalReviewDocuments externalReviewDocuments = readExternalReviewDocuments(
                jsonlPath,
                RetrievalDocumentSource.CATCHTABLE
        );

        addExternalReviewDocuments(externalReviewDocuments.documents());

        return new ExternalReviewIndexResult(
                RetrievalDocumentSource.CATCHTABLE.name(),
                externalReviewDocuments.documents().size(),
                externalReviewDocuments.skippedCount()
        );
    }

    public void deleteExternalReviews(RetrievalDocumentSource source) {
        if (source == null) {
            return;
        }

        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        var filter = builder.and(
                builder.eq(
                        RetrievalMetadataKeys.DOCUMENT_TYPE,
                        RetrievalDocumentType.EXTERNAL_REVIEW_CHUNK.name()
                ),
                builder.eq(
                        RetrievalMetadataKeys.SOURCE,
                        source.name()
                )
        ).build();

        vectorStore.delete(filter);
    }

    public List<RetrievalDocumentResult> searchShopReviewDocuments(
            Long shopId,
            String query,
            int topK,
            double similarityThreshold
    ) {
        if (shopId == null) {
            throw new IllegalArgumentException("shopId가 필요합니다.");
        }

        List<RetrievalDocumentResult> exactShopDocuments = findShopReviewDocumentsByMetadata(shopId, topK);
        if (!exactShopDocuments.isEmpty()) {
            return exactShopDocuments;
        }

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query == null || query.isBlank() ? "라멘 리뷰 맛 국물 면 메뉴 분위기" : query)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .filterExpression(RetrievalDocumentFilters.externalReviewChunksForShop(shopId))
                        .build()
        );

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .filter(document -> hasShopId(document, shopId))
                .map(document -> new RetrievalDocumentResult(
                        document.getText(),
                        document.getScore(),
                        document.getMetadata()
                ))
                .toList();
    }

    private List<RetrievalDocumentResult> findShopReviewDocumentsByMetadata(Long shopId, int topK) {
        int limit = Math.max(1, topK);
        return oracleVectorJdbcTemplate.query(
                SHOP_REVIEW_DOCUMENTS_SQL,
                (resultSet, rowNum) -> new RetrievalDocumentResult(
                        resultSet.getString("content"),
                        null,
                        readMetadataJson(resultSet.getString("metadata_json"))
                ),
                String.valueOf(shopId),
                RetrievalDocumentType.EXTERNAL_REVIEW_CHUNK.name(),
                RetrievalDocumentSource.CATCHTABLE.name(),
                RetrievalDocumentSource.NAVER_REVIEW.name(),
                limit
        );
    }

    private Map<String, Object> readMetadataJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private boolean hasShopId(Document document, Long shopId) {
        Object metadataShopId = document.getMetadata().get(RetrievalMetadataKeys.SHOP_ID);
        return String.valueOf(shopId).equals(stringValue(metadataShopId));
    }

    private void addDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        for (Document document : documents) {
            vectorStore.add(List.of(document));
        }
    }

    private void addExternalReviewDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        oracleVectorJdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);

            try (var sessionStatement = connection.createStatement();
                    PreparedStatement statement = connection.prepareStatement(EXTERNAL_REVIEW_UPSERT_SQL)) {
                sessionStatement.execute("ALTER SESSION DISABLE PARALLEL DML");

                for (int start = 0; start < documents.size(); start += EXTERNAL_REVIEW_BATCH_SIZE) {
                    int end = Math.min(start + EXTERNAL_REVIEW_BATCH_SIZE, documents.size());
                    List<Document> batch = documents.subList(start, end);
                    List<float[]> embeddings = embeddingModel.embed(batch.stream()
                            .map(Document::getText)
                            .toList());

                    for (int index = 0; index < batch.size(); index++) {
                        upsertExternalReviewDocument(statement, batch.get(index), embeddings.get(index));
                    }
                }
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }

            return null;
        });
    }

    private void upsertExternalReviewDocument(PreparedStatement statement, Document document, float[] embedding)
            throws SQLException {
        statement.setString(1, document.getId());
        statement.setString(2, document.getText());
        statement.setObject(3, toOracleJson(document.getMetadata()), OracleType.JSON.getVendorTypeNumber());
        statement.setObject(4, toOracleVector(embedding), OracleType.VECTOR.getVendorTypeNumber());
        statement.executeUpdate();
    }

    private ExternalReviewDocuments readExternalReviewDocuments(Path jsonlPath, RetrievalDocumentSource source) {
        if (jsonlPath == null) {
            throw new IllegalArgumentException("JSONL 파일 경로가 필요합니다.");
        }
        if (!Files.isRegularFile(jsonlPath)) {
            throw new IllegalArgumentException("JSONL 파일을 찾을 수 없습니다. path=" + jsonlPath);
        }

        List<Document> documents = new ArrayList<>();
        int skippedCount = 0;

        try (var lines = Files.lines(jsonlPath)) {
            for (String line : lines.toList()) {
                if (line == null || line.isBlank()) {
                    skippedCount++;
                    continue;
                }

                Document document = toExternalReviewDocument(line, source);
                if (document == null) {
                    skippedCount++;
                    continue;
                }
                documents.add(document);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("JSONL 파일을 읽을 수 없습니다. path=" + jsonlPath, e);
        }

        return new ExternalReviewDocuments(List.copyOf(documents), skippedCount);
    }

    private Document toExternalReviewDocument(String line, RetrievalDocumentSource source) {
        Map<String, Object> rawMetadata;
        try {
            rawMetadata = objectMapper.readValue(line, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("외부 리뷰 JSONL 라인을 파싱할 수 없습니다.", e);
        }

        String text = stringValue(rawMetadata.get("text"));
        String sourceId = stringValue(rawMetadata.get(RetrievalMetadataKeys.SOURCE_ID));
        if (text.isBlank() || sourceId.isBlank()) {
            return null;
        }

        Map<String, Object> metadata = new HashMap<>(rawMetadata);
        metadata.put(RetrievalMetadataKeys.DOCUMENT_TYPE, RetrievalDocumentType.EXTERNAL_REVIEW_CHUNK.name());
        metadata.put(RetrievalMetadataKeys.SOURCE, source.name());
        metadata.put(RetrievalMetadataKeys.SOURCE_ID, sourceId);

        return new Document(sourceId, text, metadata);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().trim();
    }

    private byte[] toOracleJson(Map<String, Object> metadata) throws SQLException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OracleJsonGenerator generator = oracleJsonFactory.createJsonBinaryGenerator(out)) {
            generator.writeStartObject();
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                writeOracleJsonValue(generator, entry.getKey(), entry.getValue());
            }
            generator.writeEnd();
        }
        return out.toByteArray();
    }

    private void writeOracleJsonValue(OracleJsonGenerator generator, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String stringValue) {
            generator.write(key, stringValue);
        } else if (value instanceof Integer integerValue) {
            generator.write(key, integerValue);
        } else if (value instanceof Long longValue) {
            generator.write(key, longValue);
        } else if (value instanceof Float floatValue) {
            generator.write(key, floatValue);
        } else if (value instanceof Double doubleValue) {
            generator.write(key, doubleValue);
        } else if (value instanceof Boolean booleanValue) {
            generator.write(key, booleanValue);
        } else if (value instanceof List<?> listValue) {
            generator.writeStartArray(key);
            for (Object item : listValue) {
                if (item != null) {
                    generator.write(item.toString());
                }
            }
            generator.writeEnd();
        } else {
            generator.write(key, value.toString());
        }
    }

    private VECTOR toOracleVector(float[] embedding) throws SQLException {
        double[] values = new double[embedding.length];
        double sum = 0.0;
        for (int index = 0; index < embedding.length; index++) {
            values[index] = embedding[index];
            sum += values[index] * values[index];
        }

        double norm = Math.sqrt(sum);
        if (norm > 0.0) {
            for (int index = 0; index < values.length; index++) {
                values[index] = values[index] / norm;
            }
        }

        return VECTOR.ofFloat64Values(values);
    }

    private record ExternalReviewDocuments(List<Document> documents, int skippedCount) {
    }

    public record ExternalReviewIndexResult(String source, int indexedCount, int skippedCount) {
    }

    public record RetrievalDocumentResult(String text, Double score, Map<String, Object> metadata) {
    }

}
