package com.learningos.rag.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "kb_doc_chunk",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_kb_doc_chunk_document_version_hash",
                columnNames = {"document_id", "document_version", "chunk_hash"}
        ),
        indexes = @Index(
                name = "idx_kb_doc_chunk_document_version_hash",
                columnList = "document_id, document_version, chunk_hash"
        )
)
public class KbDocChunk {

    @Id
    private String id;

    @Column(nullable = false)
    private String kbId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private KbDocument document;

    @Column(name = "document_id", nullable = false)
    private String documentId;

    @Column(nullable = false)
    private Integer documentVersion = 1;

    @Column(nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, length = 12000)
    private String content;

    private Integer pageNum;

    private String sectionTitle;

    @Column(nullable = false, length = 64)
    private String chunkHash;

    @Column(length = 4000)
    private String metadataJson = "{}";

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null || id.isBlank()) {
            id = "chk_" + UUID.randomUUID().toString().replace("-", "");
        }
        if (chunkHash == null || chunkHash.isBlank()) {
            chunkHash = fallbackChunkHash();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    private String fallbackChunkHash() {
        String input = String.join("|",
                nullToEmpty(documentId),
                String.valueOf(documentVersion),
                nullToEmpty(sectionTitle),
                nullToEmpty(content)
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKbId() {
        return kbId;
    }

    public void setKbId(String kbId) {
        this.kbId = kbId;
    }

    public KbDocument getDocument() {
        return document;
    }

    public void setDocument(KbDocument document) {
        this.document = document;
        this.documentId = document == null ? this.documentId : document.getId();
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Integer getDocumentVersion() {
        return documentVersion;
    }

    public void setDocumentVersion(Integer documentVersion) {
        this.documentVersion = documentVersion;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    public String getChunkHash() {
        return chunkHash;
    }

    public void setChunkHash(String chunkHash) {
        this.chunkHash = chunkHash;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
