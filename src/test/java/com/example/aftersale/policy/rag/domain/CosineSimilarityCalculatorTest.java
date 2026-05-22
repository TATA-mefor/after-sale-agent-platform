package com.example.aftersale.policy.rag.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CosineSimilarityCalculatorTest {

    private final CosineSimilarityCalculator calculator = new CosineSimilarityCalculator();

    @Test
    void cosineSimilarityRanksAlignedVectorsHighest() {
        double aligned = calculator.similarity(List.of(1.0d, 0.0d, 0.0d), List.of(0.9d, 0.1d, 0.0d));
        double distant = calculator.similarity(List.of(1.0d, 0.0d, 0.0d), List.of(0.0d, 1.0d, 0.0d));

        assertThat(aligned).isGreaterThan(distant);
        assertThat(aligned).isBetween(0.0d, 1.0d);
        assertThat(distant).isZero();
    }

    @Test
    void rejectsEmptyOrMismatchedVectorsClearly() {
        assertThatThrownBy(() -> calculator.similarity(List.of(), List.of(1.0d)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("left vector must not be empty");

        assertThatThrownBy(() -> calculator.similarity(List.of(1.0d), List.of(1.0d, 0.0d)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensions must match");
    }

    @Test
    void zeroVectorReturnsZeroScore() {
        assertThat(calculator.similarity(List.of(0.0d, 0.0d, 0.0d), List.of(1.0d, 0.0d, 0.0d)))
                .isZero();
    }
}
