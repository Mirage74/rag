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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatEntryServiceImpl implements ChatEntryService {

    private final ChatEntryRepository chatEntryRepository;
    private final ChatRepository chatRepository;
    private final ChatClient chatClient;

    @Override
    public List<ChatEntry> getEntriesByChatId(Long chatId) {
        return chatEntryRepository.findByChatIdOrderByCreatedAtAsc(chatId);
    }

    @Override
    @Transactional
    public ChatEntry addUserEntry(Long chatId, String content, boolean onlyContext, int topK, double topP) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found with id: " + chatId));

        ChatEntry userEntry = ChatEntry.builder()
                .chat(chat)
                .content(content)
                .role(Role.USER)
                .build();
        chatEntryRepository.save(userEntry);

        String systemPrompt = onlyContext
                ? """
                  The question may be about a CONSEQUENCE of a fact from Context.
                  ALWAYS connect: Context fact → question.
                  No connection, even indirect = answer ONLY: "The request is not related to the uploaded context."
                  Connection exists = answer using ONLY the context.
                  Do NOT use any knowledge outside the provided context.
                  """
                : """
                  The question may be about a CONSEQUENCE of a fact from Context.
                  ALWAYS connect: Context fact → question.
                  If context contains relevant information, use it in your answer.
                  If context does not contain relevant information, answer using your general knowledge.
                  """;

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(content)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(chatId)))
                .options(OllamaOptions.builder()
                        .topK(topK)
                        .topP(topP)
                        .build())
                .call()
                .content();

        ChatEntry assistantEntry = ChatEntry.builder()
                .chat(chat)
                .content(response)
                .role(Role.ASSISTANT)
                .build();

        return chatEntryRepository.save(assistantEntry);
    }
}