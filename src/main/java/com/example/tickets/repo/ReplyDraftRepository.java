package com.example.tickets.repo;

import com.example.tickets.domain.ReplyDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReplyDraftRepository extends JpaRepository<ReplyDraft, Long> {
    List<ReplyDraft> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
    Optional<ReplyDraft> findByIdempotencyKeyAndTicketId(String key, Long ticketId);
}
