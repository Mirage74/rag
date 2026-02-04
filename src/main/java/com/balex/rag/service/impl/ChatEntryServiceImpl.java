package com.balex.rag.service.impl;

import com.balex.rag.model.entity.Chat;
import com.balex.rag.model.entity.ChatEntry;
import com.balex.rag.model.enums.Role;
import com.balex.rag.repo.ChatEntryRepository;
import com.balex.rag.repo.ChatRepository;
import com.balex.rag.service.ChatEntryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatEntryServiceImpl implements ChatEntryService {

    private final ChatEntryRepository chatEntryRepository;
    private final ChatRepository chatRepository;

    @Override
    public List<ChatEntry> getEntriesByChatId(Long chatId) {
        return chatEntryRepository.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    @Override
    @Transactional
    public ChatEntry addUserEntry(Long chatId, String content) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found with id: " + chatId));

        ChatEntry entry = ChatEntry.builder()
                .chat(chat)
                .content(content)
                .role(Role.USER)
                .build();

        return chatEntryRepository.save(entry);
    }
}