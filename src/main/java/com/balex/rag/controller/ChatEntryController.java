package com.balex.rag.controller;

import com.balex.rag.model.constants.ApiLogMessage;
import com.balex.rag.model.entity.ChatEntry;
import com.balex.rag.service.ChatEntryService;
import com.balex.rag.utils.ApiUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("${end.points.entry}")
public class ChatEntryController {

    private final ChatEntryService chatEntryService;

    @GetMapping("/{chatId}")
    public ResponseEntity<List<ChatEntry>> getEntries(@PathVariable Long chatId) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());
        List<ChatEntry> entries = chatEntryService.getEntriesByChatId(chatId);
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/{chatId}")
    public ResponseEntity<ChatEntry> addUserEntry(
            @PathVariable Long chatId,
            @RequestParam String content) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());
        ChatEntry entry = chatEntryService.addUserEntry(chatId, content);
        return ResponseEntity.ok(entry);
    }
}