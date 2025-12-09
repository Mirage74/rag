package com.balex.rag.repo.impl;

import com.balex.rag.repo.VectorStoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class VectorStoreRepositoryImpl implements VectorStoreRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void deleteBySourceIn(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return;
        }

        String placeholders = String.join(",", sources.stream()
                .map(s -> "?")
                .toList());

        String sql = "DELETE FROM vector_store WHERE metadata->>'source' IN (" + placeholders + ")";

        jdbcTemplate.update(sql, sources.toArray());
    }

    @Override
    public void deleteByUserId(Long userId) {
        String sql = "DELETE FROM vector_store WHERE (metadata->>'user_id')::bigint = ?";
        jdbcTemplate.update(sql, userId);
    }
}