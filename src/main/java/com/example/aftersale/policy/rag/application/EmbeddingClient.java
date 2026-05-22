package com.example.aftersale.policy.rag.application;

public interface EmbeddingClient {

    EmbeddingResponse embed(EmbeddingRequest request);
}
