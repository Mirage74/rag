package com.balex.rag.controller;

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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import jakarta.validation.Valid;
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
    @Operation(
            summary = "Upload user documents with progress streaming",
            description = "Uploads documents and streams processing progress as Server-Sent Events (SSE)"
    )
    public Flux<String> uploadDocumentsWithProgress(
            @RequestPart("files") @Valid List<MultipartFile> files
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        Integer userId = apiUtils.getUserIdFromAuthentication();

        return userDocumentService.processUploadedFilesWithProgress(files, userId.longValue())
                .map(progress -> String.format(
                        "data: {\"percent\": %d, \"processedFiles\": %d, \"totalFiles\": %d, \"currentFile\": \"%s\", \"status\": \"%s\"}\n\n",
                        progress.getPercent(),
                        progress.getProcessedFiles(),
                        progress.getTotalFiles(),
                        progress.getCurrentFile(),
                        progress.getStatus()
                ));
    }
}