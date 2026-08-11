package com.raota.agent.domain.retrieval.document;

import java.util.List;
import org.springframework.ai.document.Document;

public interface RetrievalDocumentFactory<T> {

    List<Document> create(T source);
}
