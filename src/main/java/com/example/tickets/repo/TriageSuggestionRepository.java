package com.example.tickets.repo;

import com.example.tickets.domain.TriageSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TriageSuggestionRepository extends JpaRepository<TriageSuggestion, Long> {
    List<TriageSuggestion> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
