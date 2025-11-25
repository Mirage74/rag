package com.balex.rag.controller;

import com.balex.rag.model.constants.ApiLogMessage;
import com.balex.rag.model.response.RagResponse;
import com.balex.rag.service.UserDocumentService;
import com.balex.rag.utils.ApiUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("${end.points.document}")
public class DocumentUploadController {

    private final UserDocumentService userDocumentService;
    private final ApiUtils apiUtils;

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents successfully uploaded",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{ \"message\": \"\", \"payload\": \"Documents uploaded\", \"success\": true }"
                            )))
    })
    @PostMapping("/upload")
    @Operation(
            summary = "Upload user documents",
            description = "Uploads one or more documents for the authenticated user and stores them in the vector store"
    )
    public ResponseEntity<RagResponse<String>> uploadDocuments(
            @RequestPart("files") @Valid List<MultipartFile> files
    ) {
        log.trace(ApiLogMessage.NAME_OF_CURRENT_METHOD.getValue(), ApiUtils.getMethodName());

        Integer userId = apiUtils.getUserIdFromAuthentication();
        userDocumentService.processUploadedFiles(files, userId.longValue());

        RagResponse<String> result = RagResponse.createSuccessful("Documents uploaded");
        return ResponseEntity.ok(result);
    }
}
