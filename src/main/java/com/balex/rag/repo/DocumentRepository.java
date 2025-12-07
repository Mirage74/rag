package com.balex.rag.repo;

import com.balex.rag.model.LoadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<LoadedDocument, Long> {

    boolean existsByFilenameAndContentHash(String filename, String contentHash);

    List<LoadedDocument> findByUserId(Integer userId);

}

