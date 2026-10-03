package com.example.tickets.repo;

import com.example.tickets.domain.InternalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InternalNoteRepository extends JpaRepository<InternalNote, Long> {
    List<InternalNote> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
