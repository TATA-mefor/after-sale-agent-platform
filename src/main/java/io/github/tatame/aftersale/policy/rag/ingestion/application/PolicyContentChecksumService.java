package io.github.tatame.aftersale.policy.rag.ingestion.application;

import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionChunk;
import io.github.tatame.aftersale.policy.rag.ingestion.domain.PolicyIngestionDocument;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

public class PolicyContentChecksumService {

    public PolicyChecksum checksumDocument(PolicyIngestionDocument document) {
        return checksumContent(Objects.requireNonNull(document, "document must not be null").rawText());
    }

    public PolicyChecksum checksumChunk(PolicyIngestionChunk chunk) {
        return checksumContent(Objects.requireNonNull(chunk, "chunk must not be null").content());
    }

    public PolicyChecksum checksumContent(String content) {
        String normalized = normalizeContent(content);
        byte[] digest = messageDigest().digest(normalized.getBytes(StandardCharsets.UTF_8));
        return new PolicyChecksum(ChecksumAlgorithm.SHA_256, HexFormat.of().formatHex(digest));
    }

    String normalizeContent(String content) {
        Objects.requireNonNull(content, "content must not be null");
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        return normalized;
    }

    private static MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 checksum algorithm is unavailable", exception);
        }
    }
}
