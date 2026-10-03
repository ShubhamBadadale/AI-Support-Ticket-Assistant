package com.example.tickets.repo;

import com.example.tickets.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AiJobRepository extends JpaRepository<AiJob, Long> {
    List<AiJob> findByStatusInOrderByCreatedAtAsc(List<JobStatus> statuses);
    List<AiJob> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
