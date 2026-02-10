package com.back.global.outbox.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "OUTBOX_EVENT", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status, createDate"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregateType, aggregateId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createDate;

    private LocalDateTime sentDate;

    @Column(nullable = false)
    private int retryCount = 0;

    private String lastErrorMessage;

    @Version
    private Long version;

    public static OutboxEvent create(String aggregateType, String aggregateId,
                                     String eventType, String topic, String payload) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.topic = topic;
        event.payload = payload;
        event.createDate = LocalDateTime.now();
        event.status = OutboxStatus.PENDING;
        return event;
    }

    public void markAsProcessing() {
        this.status = OutboxStatus.PROCESSING;
    }

    public void markAsSent() {
        this.status = OutboxStatus.SENT;
        this.sentDate = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.retryCount++;
        this.lastErrorMessage = errorMessage;
        if (this.retryCount >= 5) {
            this.status = OutboxStatus.FAILED;
        } else {
            this.status = OutboxStatus.PENDING;
        }
    }
}
