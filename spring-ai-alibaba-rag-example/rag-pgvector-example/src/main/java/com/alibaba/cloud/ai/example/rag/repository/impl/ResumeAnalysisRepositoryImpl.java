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

import com.alibaba.cloud.ai.example.rag.model.ResumeAnalysis;
import com.alibaba.cloud.ai.example.rag.model.AssessmentScore;
import com.alibaba.cloud.ai.example.rag.repository.ResumeAnalysisRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 简历分析结果数据访问实现类
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@Repository
public class ResumeAnalysisRepositoryImpl implements ResumeAnalysisRepository {

    private static final Logger logger = LoggerFactory.getLogger(ResumeAnalysisRepositoryImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ResumeAnalysisRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ResumeAnalysis save(ResumeAnalysis analysis) {
        logger.info("保存简历分析结果 - resumeId: {}", analysis.getResumeId());
        
        if (existsByResumeId(analysis.getResumeId())) {
            return update(analysis);
        } else {
            return insert(analysis);
        }
    }

    private ResumeAnalysis insert(ResumeAnalysis analysis) {
        String sql = """
            INSERT INTO resume_analysis (resume_id, summary, strengths, improvements,
                                       experience_score, skill_score, education_score,
                                       overall_score, recommendation, analysis_time) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        try {
            // 验证 resumeId 不为空
            if (analysis.getResumeId() == null || analysis.getResumeId().trim().isEmpty()) {
                throw new IllegalArgumentException("resumeId 不能为空或null");
            }
            
            // 转换数组参数
            Object[] strengthsArray = analysis.getStrengths() != null ? 
                analysis.getStrengths().toArray(new String[0]) : new String[0];
            Object[] improvementsArray = analysis.getImprovements() != null ? 
                analysis.getImprovements().toArray(new String[0]) : new String[0];

            // 获取评分信息
            AssessmentScore score = analysis.getAssessmentScore();
            Integer experienceScore = score != null ? score.getExperienceScore() : null;
            Integer skillScore = score != null ? score.getSkillScore() : null;
            Integer educationScore = score != null ? score.getEducationScore() : null;
            Integer overallScore = score != null ? score.getOverallScore() : null;
            String recommendation = score != null ? score.getRecommendation() : null;

            logger.info("准备插入分析结果 - resumeId: {}, 综合评分: {}, 优势数量: {}, 改进建议数量: {}", 
                analysis.getResumeId(), overallScore, 
                analysis.getStrengths() != null ? analysis.getStrengths().size() : 0,
                analysis.getImprovements() != null ? analysis.getImprovements().size() : 0);

            jdbcTemplate.update(sql,
                analysis.getResumeId(),
                analysis.getSummary(),
                strengthsArray,
                improvementsArray,
                experienceScore,
                skillScore,
                educationScore,
                overallScore,
                recommendation
            );

            logger.info("简历分析结果插入成功 - resumeId: {}, 综合评分: {}", 
                analysis.getResumeId(), overallScore);
            
            return analysis;
            
        } catch (Exception e) {
            // 检查是否是外键约束违反错误
            if (e.getMessage() != null && e.getMessage().contains("resume_analysis_resume_id_fkey")) {
                logger.error("外键约束违反 - resumeId: {} 在 resumes 表中不存在。请检查简历记录是否已被删除或resumeId是否正确。错误详情: {}", 
                    analysis.getResumeId(), e.getMessage());
                throw new IllegalArgumentException(
                    String.format("简历记录不存在 (resumeId: %s)。请确认简历是否已上传且未被删除。", 
                        analysis.getResumeId()), e);
            }
            
            logger.error("插入简历分析结果失败 - resumeId: {}, 错误类型: {}, 错误信息: {}", 
                analysis.getResumeId(), e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("保存分析结果失败: " + e.getMessage(), e);
        }
    }

    private ResumeAnalysis update(ResumeAnalysis analysis) {
        String sql = """
            UPDATE resume_analysis 
            SET summary = ?, strengths = ?, improvements = ?,
                experience_score = ?, skill_score = ?, education_score = ?,
                overall_score = ?, recommendation = ?, 
                updated_at = CURRENT_TIMESTAMP
            WHERE resume_id = ?
            """;

        try {
            // 转换数组参数
            Object[] strengthsArray = analysis.getStrengths() != null ? 
                analysis.getStrengths().toArray(new String[0]) : new String[0];
            Object[] improvementsArray = analysis.getImprovements() != null ? 
                analysis.getImprovements().toArray(new String[0]) : new String[0];

            // 获取评分信息
            AssessmentScore score = analysis.getAssessmentScore();
            Integer experienceScore = score != null ? score.getExperienceScore() : null;
            Integer skillScore = score != null ? score.getSkillScore() : null;
            Integer educationScore = score != null ? score.getEducationScore() : null;
            Integer overallScore = score != null ? score.getOverallScore() : null;
            String recommendation = score != null ? score.getRecommendation() : null;

            jdbcTemplate.update(sql,
                analysis.getSummary(),
                strengthsArray,
                improvementsArray,
                experienceScore,
                skillScore,
                educationScore,
                overallScore,
                recommendation,
                analysis.getResumeId()
            );

            logger.info("简历分析结果更新成功 - resumeId: {}, 综合评分: {}", 
                analysis.getResumeId(), overallScore);
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("更新简历分析结果失败 - resumeId: {}, 错误: {}", 
                analysis.getResumeId(), e.getMessage(), e);
            throw new RuntimeException("更新分析结果失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<ResumeAnalysis> findByResumeId(String resumeId) {
        logger.debug("查询简历分析结果 - resumeId: {}", resumeId);
        
        String sql = "SELECT * FROM resume_analysis WHERE resume_id = ?";
        List<ResumeAnalysis> results = jdbcTemplate.query(sql, new ResumeAnalysisRowMapper(), resumeId);
        
        if (results.isEmpty()) {
            logger.debug("未找到分析结果 - resumeId: {}", resumeId);
            return Optional.empty();
        } else {
            logger.debug("找到分析结果 - resumeId: {}", resumeId);
            return Optional.of(results.get(0));
        }
    }

    @Override
    public List<ResumeAnalysis> findAll() {
        String sql = "SELECT * FROM resume_analysis ORDER BY analysis_time DESC";
        List<ResumeAnalysis> results = jdbcTemplate.query(sql, new ResumeAnalysisRowMapper());
        logger.info("查询所有分析结果完成 - 结果数量: {}", results.size());
        return results;
    }

    @Override
    public List<ResumeAnalysis> findByOverallScoreRange(int minScore, int maxScore) {
        String sql = "SELECT * FROM resume_analysis WHERE overall_score >= ? AND overall_score <= ? ORDER BY overall_score DESC";
        List<ResumeAnalysis> results = jdbcTemplate.query(sql, new ResumeAnalysisRowMapper(), minScore, maxScore);
        logger.info("按评分范围查询分析结果完成 - 范围: {}-{}, 结果数量: {}", minScore, maxScore, results.size());
        return results;
    }

    @Override
    public boolean existsByResumeId(String resumeId) {
        String sql = "SELECT COUNT(*) FROM resume_analysis WHERE resume_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, resumeId);
        boolean exists = count != null && count > 0;
        logger.debug("检查分析结果存在性 - resumeId: {}, 存在: {}", resumeId, exists);
        return exists;
    }

    @Override
    public void deleteByResumeId(String resumeId) {
        String sql = "DELETE FROM resume_analysis WHERE resume_id = ?";
        int deletedRows = jdbcTemplate.update(sql, resumeId);
        logger.info("删除分析结果完成 - resumeId: {}, 删除行数: {}", resumeId, deletedRows);
    }

    @Override
    public List<ResumeAnalysis> findRecentAnalysis(int limit) {
        String sql = "SELECT * FROM resume_analysis ORDER BY analysis_time DESC LIMIT ?";
        List<ResumeAnalysis> results = jdbcTemplate.query(sql, new ResumeAnalysisRowMapper(), limit);
        logger.info("查询最新分析结果完成 - 限制数量: {}, 实际结果数量: {}", limit, results.size());
        return results;
    }

    /**
     * ResumeAnalysis行映射器
     */
    private class ResumeAnalysisRowMapper implements RowMapper<ResumeAnalysis> {
        @Override
        public ResumeAnalysis mapRow(ResultSet rs, int rowNum) throws SQLException {
            ResumeAnalysis analysis = new ResumeAnalysis();
            analysis.setResumeId(rs.getString("resume_id"));
            analysis.setSummary(rs.getString("summary"));

            // 处理数组字段
            Array strengthsArray = rs.getArray("strengths");
            if (strengthsArray != null) {
                String[] strengths = (String[]) strengthsArray.getArray();
                analysis.setStrengths(Arrays.asList(strengths));
            }

            Array improvementsArray = rs.getArray("improvements");
            if (improvementsArray != null) {
                String[] improvements = (String[]) improvementsArray.getArray();
                analysis.setImprovements(Arrays.asList(improvements));
            }

            // 构建评分对象
            AssessmentScore score = new AssessmentScore();
            score.setExperienceScore(rs.getObject("experience_score", Integer.class));
            score.setSkillScore(rs.getObject("skill_score", Integer.class));
            score.setEducationScore(rs.getObject("education_score", Integer.class));
            score.setOverallScore(rs.getObject("overall_score", Integer.class));
            score.setRecommendation(rs.getString("recommendation"));
            analysis.setAssessmentScore(score);

            return analysis;
        }
    }
}