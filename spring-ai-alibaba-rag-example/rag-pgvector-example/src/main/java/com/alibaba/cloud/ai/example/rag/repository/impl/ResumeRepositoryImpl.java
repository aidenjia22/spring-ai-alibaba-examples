/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.example.rag.repository.impl;

import com.alibaba.cloud.ai.example.rag.model.Resume;
import com.alibaba.cloud.ai.example.rag.model.ResumeStatus;
import com.alibaba.cloud.ai.example.rag.repository.ResumeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 简历数据访问实现类
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@Repository
public class ResumeRepositoryImpl implements ResumeRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ResumeRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Resume save(Resume resume) {
        if (existsById(resume.getResumeId())) {
            return update(resume);
        } else {
            return insert(resume);
        }
    }

    private Resume insert(Resume resume) {
        String sql = """
            INSERT INTO resumes (resume_id, candidate_name, original_file_name, file_type, 
                               status, upload_time, metadata) 
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
            """;
        
        String metadataJson = null;
        if (resume.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(resume.getMetadata());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize metadata", e);
            }
        }

        jdbcTemplate.update(sql,
            resume.getResumeId(),
            resume.getCandidateName(),
            resume.getOriginalFileName(),
            resume.getFileType(),
            resume.getStatus() != null ? resume.getStatus().name() : ResumeStatus.UPLOADED.name(),
            Timestamp.valueOf(resume.getUploadTime() != null ? resume.getUploadTime() : LocalDateTime.now()),
            metadataJson
        );
        
        return resume;
    }

    private Resume update(Resume resume) {
        String sql = """
            UPDATE resumes 
            SET candidate_name = ?, original_file_name = ?, file_type = ?, 
                status = ?, metadata = ?::jsonb, updated_at = CURRENT_TIMESTAMP
            WHERE resume_id = ?
            """;
        
        String metadataJson = null;
        if (resume.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(resume.getMetadata());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize metadata", e);
            }
        }

        jdbcTemplate.update(sql,
            resume.getCandidateName(),
            resume.getOriginalFileName(),
            resume.getFileType(),
            resume.getStatus() != null ? resume.getStatus().name() : ResumeStatus.UPLOADED.name(),
            metadataJson,
            resume.getResumeId()
        );
        
        return resume;
    }

    @Override
    public Optional<Resume> findById(String resumeId) {
        String sql = "SELECT * FROM resumes WHERE resume_id = ?";
        List<Resume> resumes = jdbcTemplate.query(sql, new ResumeRowMapper(), resumeId);
        return resumes.isEmpty() ? Optional.empty() : Optional.of(resumes.get(0));
    }

    @Override
    public List<Resume> findAll() {
        String sql = "SELECT * FROM resumes ORDER BY upload_time DESC";
        return jdbcTemplate.query(sql, new ResumeRowMapper());
    }

    @Override
    public List<Resume> findByCandidateNameContaining(String candidateName) {
        String sql = "SELECT * FROM resumes WHERE candidate_name ILIKE ? ORDER BY upload_time DESC";
        return jdbcTemplate.query(sql, new ResumeRowMapper(), "%" + candidateName + "%");
    }

    @Override
    public List<Resume> findByStatus(String status) {
        String sql = "SELECT * FROM resumes WHERE status = ? ORDER BY upload_time DESC";
        return jdbcTemplate.query(sql, new ResumeRowMapper(), status);
    }

    @Override
    public boolean existsById(String resumeId) {
        String sql = "SELECT COUNT(*) FROM resumes WHERE resume_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, resumeId);
        return count != null && count > 0;
    }

    @Override
    public void deleteById(String resumeId) {
        String sql = "DELETE FROM resumes WHERE resume_id = ?";
        jdbcTemplate.update(sql, resumeId);
    }

    @Override
    public List<Resume> findRecentResumes(int limit) {
        String sql = "SELECT * FROM resumes ORDER BY upload_time DESC LIMIT ?";
        return jdbcTemplate.query(sql, new ResumeRowMapper(), limit);
    }

    /**
     * Resume行映射器
     */
    private class ResumeRowMapper implements RowMapper<Resume> {
        @Override
        public Resume mapRow(ResultSet rs, int rowNum) throws SQLException {
            Resume resume = new Resume();
            resume.setResumeId(rs.getString("resume_id"));
            resume.setCandidateName(rs.getString("candidate_name"));
            resume.setOriginalFileName(rs.getString("original_file_name"));
            resume.setFileType(rs.getString("file_type"));
            
            String statusStr = rs.getString("status");
            if (statusStr != null) {
                resume.setStatus(ResumeStatus.valueOf(statusStr));
            }
            
            Timestamp uploadTimestamp = rs.getTimestamp("upload_time");
            if (uploadTimestamp != null) {
                resume.setUploadTime(uploadTimestamp.toLocalDateTime());
            }
            
            String metadataJson = rs.getString("metadata");
            if (metadataJson != null && !metadataJson.isEmpty()) {
                try {
                    Map<String, Object> metadata = objectMapper.readValue(metadataJson, 
                        new TypeReference<Map<String, Object>>() {});
                    resume.setMetadata(metadata);
                } catch (JsonProcessingException e) {
                    // 忽略JSON解析错误，保持metadata为null
                }
            }
            
            return resume;
        }
    }
}