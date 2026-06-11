package com.learningos.rag.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "kb_chat_message")
public class KbChatMessage {
    @Id
    private String id;
}
