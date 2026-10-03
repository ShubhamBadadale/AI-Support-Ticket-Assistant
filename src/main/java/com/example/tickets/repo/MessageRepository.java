package com.example.tickets.repo;

import com.example.tickets.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
