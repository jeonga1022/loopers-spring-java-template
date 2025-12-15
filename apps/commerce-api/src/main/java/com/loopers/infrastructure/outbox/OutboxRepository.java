package com.loopers.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findByStatusOrderByCreatedAtAsc(OutboxStatus status);

    default List<Outbox> findPending() {
        return findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
    }
}
