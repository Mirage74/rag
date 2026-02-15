package com.balex.rag.controller;

import com.balex.rag.config.RagDefaultsProperties;
import com.balex.rag.model.constants.ApiLogMessage;
import com.balex.rag.model.dto.UserEntryRequest;
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
    private final RagDefaultsProperties ragDefaults;

    @PostMapping("/{chatId}")
    public ResponseEntity<ChatEntry> addUserEntry(
            @PathVariable Long chatId,
            @RequestBody UserEntryRequest request) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        boolean onlyContext = request.onlyContext() != null ? request.onlyContext() : ragDefaults.onlyContext();
        int topK = request.topK() != null ? request.topK() : ragDefaults.topK();
        double topP = request.topP() != null ? request.topP() : ragDefaults.topP();

        ChatEntry entry = chatEntryService.addUserEntry(chatId, request.content(), onlyContext, topK, topP);
        return ResponseEntity.ok(entry);
    }
}