package com.balex.rag.repo;

import com.balex.rag.model.entity.ChatEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatEntryRepository extends JpaRepository<ChatEntry, Long> {

    List<ChatEntry> findByChatIdOrderByCreatedAtAsc(Long chatId);
}
