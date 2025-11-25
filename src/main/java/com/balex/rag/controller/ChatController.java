package com.balex.rag.controller;

import com.balex.rag.model.constants.ApiLogMessage;
import com.balex.rag.model.entity.Chat;
import com.balex.rag.service.ChatService;
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
@RequestMapping("${end.points.chat}")
public class ChatController {

    private final ChatService chatService;

    @GetMapping("")
    public ResponseEntity<List<Chat>> mainPage() {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());
        List<Chat> response = chatService.getAllChats();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<Chat> showChat(@PathVariable Long chatId) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());
        Chat response = chatService.getChat(chatId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/new")
    public ResponseEntity<Chat> newChat(@RequestParam String title) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());
        Chat chat = chatService.createNewChat(title);
        return ResponseEntity.ok(chat);
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteChat(@PathVariable Long chatId) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());
        chatService.deleteChat(chatId);
        return ResponseEntity.noContent().build();
    }
}

