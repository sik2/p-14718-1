package com.back.global.outbox.repository;

import com.back.global.outbox.entity.OutboxEvent;
import com.back.global.outbox.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = :status ORDER BY o.createDate ASC")
    List<OutboxEvent> findByStatusOrderByCreateDate(
        @Param("status") OutboxStatus status,
        Pageable pageable
    );

    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.status = 'SENT' AND o.sentDate < :before")
    int deleteSentEventsBefore(@Param("before") LocalDateTime before);
}
