package io.github.tatame.aftersale.policy.rag.infrastructure.springai;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;

final class EmbeddingModelSpringAiEmbeddingGateway implements SpringAiEmbeddingGateway {

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    EmbeddingModelSpringAiEmbeddingGateway(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.embeddingModelProvider = embeddingModelProvider;
    }

    @Override
    public List<Double> embed(String text) {
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            throw new IllegalStateException("Spring AI EmbeddingModel bean is not available");
        }
        float[] values = embeddingModel.embed(text);
        List<Double> vector = new ArrayList<>(values.length);
        for (float value : values) {
            vector.add((double) value);
        }
        return vector;
    }
}
