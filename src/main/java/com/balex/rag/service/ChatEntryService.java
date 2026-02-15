package com.balex.rag.service;

import com.balex.rag.model.entity.ChatEntry;

import java.util.List;

public interface ChatEntryService {

    List<ChatEntry> getEntriesByChatId(Long chatId);

    ChatEntry addUserEntry(Long chatId, String content, boolean onlyContext, int topK, double topP);
}