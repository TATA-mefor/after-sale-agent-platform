package io.github.tatame.aftersale.policy.rag.infrastructure.springai;

import java.util.List;

public interface SpringAiEmbeddingGateway {

    List<Double> embed(String text);
}
