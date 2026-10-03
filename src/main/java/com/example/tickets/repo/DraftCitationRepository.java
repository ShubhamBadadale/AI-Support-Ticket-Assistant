package com.example.tickets.repo;

import com.example.tickets.domain.DraftCitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DraftCitationRepository extends JpaRepository<DraftCitation, Long> {
    List<DraftCitation> findByDraftId(Long draftId);
}
