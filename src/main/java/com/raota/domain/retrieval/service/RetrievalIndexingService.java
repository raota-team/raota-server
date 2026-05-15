package com.raota.domain.retrieval.service;

import com.raota.domain.ramenShop.model.RamenShop;
import com.raota.domain.ramenShop.repository.RamenShopRepository;
import com.raota.domain.retrieval.document.shop.RamenShopProfileDocumentFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetrievalIndexingService {

    private final RamenShopRepository ramenShopRepository;
    private final RamenShopProfileDocumentFactory ramenShopProfileDocumentFactory;
    private final VectorStore vectorStore;

    public void indexAllShops() {
        List<RamenShop> shops = ramenShopRepository.findAll();
        List<Document> documents = new ArrayList<>();

        for (RamenShop shop : shops) {
            documents.addAll(ramenShopProfileDocumentFactory.create(shop));
        }

        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
    }

    public void indexShop(Long shopId) {
        RamenShop shop = ramenShopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("라멘샵을 찾을 수 없습니다. id=" + shopId));

        List<Document> documents = ramenShopProfileDocumentFactory.create(shop);
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
    }
}
