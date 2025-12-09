package com.balex.rag.repo;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VectorStoreRepository {

    void deleteBySourceIn(List<String> sources);

    void deleteByUserId(Long userId);
}