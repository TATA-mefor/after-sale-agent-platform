package com.example.aftersale.policy.rag.infrastructure.springai;

import java.util.List;

public interface SpringAiEmbeddingGateway {

    List<Double> embed(String text);
}
