package com.balex.rag.controller;

import com.balex.rag.model.UploadProgress;
import com.balex.rag.model.constants.ApiLogMessage;
import com.balex.rag.service.UserDocumentService;
import com.balex.rag.utils.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;

import java.time.Duration;
import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("${end.points.document}")
public class DocumentUploadStreamController {

    private final UserDocumentService userDocumentService;
    private final ApiUtils apiUtils;

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document processing progress stream",
                    content = @Content(mediaType = "text/event-stream",
                            examples = @ExampleObject(
                                    value = "data: {\"percent\": 33, \"processedFiles\": 1, \"totalFiles\": 3, \"currentFile\": \"doc1.txt\"}\n\n"
                            )))
    })
    @PostMapping(value = "/upload-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadDocumentsWithProgress(
            @RequestPart("files") @Valid List<MultipartFile> files
    ) {
        Integer userId = apiUtils.getUserIdFromAuthentication();
        return userDocumentService.processUploadedFilesWithSse(files, userId.longValue());
    }
}