package com.balex.rag.service;

import com.balex.rag.model.entity.Chat;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    Chat createNewChat(String title);

    List<Chat> getAllChats();

    Chat getChat(Long chatId);

    void deleteChat(Long chatId);

    SseEmitter proceedInteractionWithStreaming(Long chatId, String userPrompt);

}
