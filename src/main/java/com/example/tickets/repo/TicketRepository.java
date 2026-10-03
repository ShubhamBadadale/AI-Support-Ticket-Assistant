package com.example.tickets.repo;

import com.example.tickets.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketNumber(String ticketNumber);
    Page<Ticket> findByCreatedById(Long userId, Pageable pageable);
    @Query("select t from Ticket t where (:status is null or t.status = :status) and (:category is null or t.confirmedCategory = :category) and (:urgency is null or t.confirmedUrgency = :urgency) and (:assigneeId is null or (t.assignee is not null and t.assignee.id = :assigneeId)) and (:q is null or lower(t.subject) like lower(concat('%', :q, '%')) or t.ticketNumber = :qn)")
    Page<Ticket> search(TicketStatus status, Category category, Urgency urgency, Long assigneeId, String q, String qn, Pageable pageable);
}
