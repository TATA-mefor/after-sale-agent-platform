package io.github.tatame.aftersale.policy.rag.application;

public interface EmbeddingClient {

    EmbeddingResponse embed(EmbeddingRequest request);
}
