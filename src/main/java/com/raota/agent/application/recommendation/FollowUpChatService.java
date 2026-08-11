package com.raota.agent.application.recommendation;

import com.raota.agent.application.recommendation.dto.AiFollowUpChatResult;
import com.raota.agent.application.ramenshop.search.RamenShopReader;
import com.raota.ramenshop.domain.model.RamenShop;
import com.raota.agent.domain.retrieval.document.RetrievalDocumentFilters;
import com.raota.agent.domain.retrieval.document.RetrievalMetadataKeys;
import com.raota.agent.presentation.recommendation.request.AiChatRequest;
import com.raota.agent.presentation.recommendation.response.AiChatResponse;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class FollowUpChatService {
    private static final String CONTEXT_TYPE_SUMMARY = "summary";
    private static final String CONTEXT_TYPE_COMPARE = "compare";
    private static final int RECENT_MESSAGE_LIMIT = 6;

    private final RamenShopReader ramenShopReader;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final Resource followUpChatTemplate;

    public FollowUpChatService(
            RamenShopReader ramenShopReader,
            VectorStore vectorStore,
            ChatClient.Builder chatClientBuilder,
            @Value("classpath:/prompts/system-persona.st") Resource systemPersona,
            @Value("classpath:/prompts/follow-up-chat.st") Resource followUpChatTemplate
    ) {
        this.ramenShopReader = ramenShopReader;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.defaultSystem(systemPersona).build();
        this.followUpChatTemplate = followUpChatTemplate;
    }

    public AiChatResponse followUpChat(AiChatRequest request) {
        validateChatRequest(request);

        String contextType = normalizeContextType(request.contextType());
        List<RamenShop> shops = ramenShopReader.getRamenShops(request.shopIds());
        List<Document> documents = collectChatDocuments(contextType, shops, request.messages());

        if (documents.isEmpty()) {
            return fallbackResponse();
        }

        AiFollowUpChatResult aiResult = generateChatResult(contextType, shops, documents, request.messages());

        return buildChatResponse(aiResult);
    }

    private void validateChatRequest(AiChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("추가 질문 요청은 필수입니다.");
        }

        String contextType = normalizeContextType(request.contextType());
        if (!CONTEXT_TYPE_SUMMARY.equals(contextType) && !CONTEXT_TYPE_COMPARE.equals(contextType)) {
            throw new IllegalArgumentException("contextType은 summary 또는 compare만 가능합니다.");
        }

        if (request.shopIds() == null || request.shopIds().isEmpty()) {
            throw new IllegalArgumentException("연관 매장 ID는 필수입니다.");
        }

        if (CONTEXT_TYPE_SUMMARY.equals(contextType) && request.shopIds().size() != 1) {
            throw new IllegalArgumentException("summary 맥락에서는 매장 ID를 1개만 전달해야 합니다.");
        }

        if (CONTEXT_TYPE_COMPARE.equals(contextType) && request.shopIds().size() != 2) {
            throw new IllegalArgumentException("compare 맥락에서는 매장 ID를 2개 전달해야 합니다.");
        }

        if (request.shopIds().stream().anyMatch(id -> id == null)) {
            throw new IllegalArgumentException("연관 매장 ID는 null일 수 없습니다.");
        }

        if (request.shopIds().stream().distinct().count() != request.shopIds().size()) {
            throw new IllegalArgumentException("연관 매장 ID는 중복될 수 없습니다.");
        }

        if (request.messages() == null || request.messages().isEmpty()) {
            throw new IllegalArgumentException("대화 메시지는 필수입니다.");
        }

        if (request.messages().stream()
                .anyMatch(message -> message == null || !ramenShopReader.hasText(message.content()))) {
            throw new IllegalArgumentException("대화 메시지 내용은 필수입니다.");
        }
    }

    private List<Document> collectChatDocuments(
            String contextType,
            List<RamenShop> shops,
            List<AiChatRequest.ChatMessage> messages
    ) {
        String query = buildChatQuery(contextType, shops, messages);
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(10)
                        .similarityThreshold(0.4)
                        .filterExpression(buildChatFilter(builder, shops))
                        .build()
        );
    }

    private Filter.Expression buildChatFilter(FilterExpressionBuilder builder, List<RamenShop> shops) {
        var shopFilter = builder.group(filterOp(RetrievalDocumentFilters.shopProfileOrExternalReviewsForShop(shops.getFirst().getId())));
        if (shops.size() == 2) {
            shopFilter = builder.or(
                    shopFilter,
                    builder.group(filterOp(RetrievalDocumentFilters.shopProfileOrExternalReviewsForShop(shops.get(1).getId())))
            );
        }

        return builder.group(shopFilter).build();
    }

    private FilterExpressionBuilder.Op filterOp(Filter.Expression expression) {
        return new FilterExpressionBuilder.Op(expression);
    }

    private String buildChatQuery(
            String contextType,
            List<RamenShop> shops,
            List<AiChatRequest.ChatMessage> messages
    ) {
        String shopNames = shops.stream()
                .map(RamenShop::getName)
                .collect(Collectors.joining(" "));
        String latestQuestion = findLatestUserMessage(messages);

        return "%s %s %s".formatted(contextType, shopNames, latestQuestion);
    }

    private String findLatestUserMessage(List<AiChatRequest.ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            AiChatRequest.ChatMessage message = messages.get(i);
            if ("user".equals(normalizeRole(message.role()))) {
                return message.content().trim();
            }
        }

        return messages.getLast().content().trim();
    }

    private AiFollowUpChatResult generateChatResult(
            String contextType,
            List<RamenShop> shops,
            List<Document> documents,
            List<AiChatRequest.ChatMessage> messages
    ) {
        return chatClient.prompt()
                .user(user -> user.text(followUpChatTemplate)
                        .param("contextType", contextType)
                        .param("shopContext", buildShopContext(shops))
                        .param("documentContext", buildDocumentContext(documents))
                        .param("messageContext", buildMessageContext(messages)))
                .call()
                .entity(AiFollowUpChatResult.class);
    }

    private AiChatResponse buildChatResponse(AiFollowUpChatResult aiResult) {
        if (aiResult == null || !ramenShopReader.hasText(aiResult.content())) {
            return fallbackResponse();
        }

        return new AiChatResponse(
                new AiChatResponse.ChatMessageResponse("ai", aiResult.content().trim())
        );
    }

    private AiChatResponse fallbackResponse() {
        return new AiChatResponse(
                new AiChatResponse.ChatMessageResponse(
                        "ai",
                        "확인 가능한 리뷰와 매장 정보가 충분하지 않아 답변하기 어렵습니다. 검색 가능한 리뷰 데이터가 쌓인 뒤 다시 질문해 주세요."
                )
        );
    }

    private String buildShopContext(List<RamenShop> shops) {
        return shops.stream()
                .map(this::formatShop)
                .collect(Collectors.joining("\n"));
    }

    private String formatShop(RamenShop shop) {
        return """
            - 매장ID: %s
              매장명: %s
              주소: %s
              태그: %s
              설명: %s
            """.formatted(
                shop.getId(),
                shop.getName(),
                ramenShopReader.addressTextOrDefault(shop),
                ramenShopReader.tagsTextOrDefault(shop),
                ramenShopReader.descriptionTextOrDefault(shop)
        );
    }

    private String buildDocumentContext(List<Document> documents) {
        return documents.stream()
                .map(this::formatDocument)
                .collect(Collectors.joining("\n"));
    }

    private String formatDocument(Document document) {
        Object shopId = document.getMetadata().get(RetrievalMetadataKeys.SHOP_ID);
        Object documentType = document.getMetadata().get(RetrievalMetadataKeys.DOCUMENT_TYPE);
        Object source = document.getMetadata().get(RetrievalMetadataKeys.SOURCE);

        return """
            - 매장ID: %s
              문서유형: %s
              출처: %s
              내용: %s
            """.formatted(
                shopId == null ? "UNKNOWN" : shopId,
                documentType == null ? "UNKNOWN" : documentType,
                source == null ? "UNKNOWN" : source,
                document.getText()
        );
    }

    private String buildMessageContext(List<AiChatRequest.ChatMessage> messages) {
        int skipCount = Math.max(0, messages.size() - RECENT_MESSAGE_LIMIT);

        return messages.stream()
                .skip(skipCount)
                .map(message -> "%s: %s".formatted(normalizeRole(message.role()), message.content().trim()))
                .collect(Collectors.joining("\n"));
    }

    private String normalizeContextType(String contextType) {
        if (!ramenShopReader.hasText(contextType)) {
            return "";
        }

        return contextType.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRole(String role) {
        if (!ramenShopReader.hasText(role)) {
            return "user";
        }

        String normalizedRole = role.trim().toLowerCase(Locale.ROOT);
        if ("ai".equals(normalizedRole) || "assistant".equals(normalizedRole)) {
            return "ai";
        }

        return "user";
    }

}
