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

import com.alibaba.cloud.ai.example.rag.model.JobMatchingResult;
import com.alibaba.cloud.ai.example.rag.repository.JobMatchingResultRepository;
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
import java.util.Map;
import java.util.Optional;

/**
 * 简历岗位匹配结果数据访问实现类
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@Repository
public class JobMatchingResultRepositoryImpl implements JobMatchingResultRepository {

    private static final Logger logger = LoggerFactory.getLogger(JobMatchingResultRepositoryImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JobMatchingResultRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public JobMatchingResult save(JobMatchingResult result) {
        logger.info("保存匹配结果 - resumeId: {}, jobId: {}, 匹配分数: {}", 
                   result.getResumeId(), result.getJobId(), result.getMatchScore());
        
        if (existsByResumeIdAndJobId(result.getResumeId(), result.getJobId())) {
            return update(result);
        } else {
            return insert(result);
        }
    }

    private JobMatchingResult insert(JobMatchingResult result) {
        String sql = """
            INSERT INTO job_matching_results (resume_id, job_id, match_score, match_level,
                                            summary, detail_matches, advantages, gaps, 
                                            recommendations, analysis_time, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        try {
            // 验证必填字段
            if (result.getResumeId() == null || result.getJobId() == null) {
                throw new IllegalArgumentException("resumeId和jobId不能为空");
            }
            
            // 转换数组参数
            Object[] advantagesArray = result.getAdvantages() != null ? 
                result.getAdvantages().toArray(new String[0]) : new String[0];
            Object[] gapsArray = result.getGaps() != null ? 
                result.getGaps().toArray(new String[0]) : new String[0];
            Object[] recommendationsArray = result.getRecommendations() != null ? 
                result.getRecommendations().toArray(new String[0]) : new String[0];

            // 转换详细匹配信息为JSON
            String detailMatchesJson = null;
            if (result.getDetailMatches() != null) {
                detailMatchesJson = objectMapper.writeValueAsString(result.getDetailMatches());
            }

            // 设置分析时间
            Timestamp analysisTime = result.getAnalysisTime() != null ? 
                Timestamp.valueOf(result.getAnalysisTime()) : Timestamp.valueOf(LocalDateTime.now());

            logger.debug("准备插入匹配结果 - resumeId: {}, jobId: {}, 匹配等级: {}, 优势数量: {}, 差距数量: {}, 建议数量: {}", 
                        result.getResumeId(), result.getJobId(), result.getMatchLevel(),
                        result.getAdvantages() != null ? result.getAdvantages().size() : 0,
                        result.getGaps() != null ? result.getGaps().size() : 0,
                        result.getRecommendations() != null ? result.getRecommendations().size() : 0);

            jdbcTemplate.update(sql,
                result.getResumeId(),
                result.getJobId(),
                result.getMatchScore(),
                result.getMatchLevel(),
                result.getSummary(),
                detailMatchesJson,
                advantagesArray,
                gapsArray,
                recommendationsArray,
                analysisTime
            );

            logger.info("匹配结果插入成功 - resumeId: {}, jobId: {}, 匹配分数: {}", 
                       result.getResumeId(), result.getJobId(), result.getMatchScore());
            
            return result;
            
        } catch (JsonProcessingException e) {
            logger.error("序列化详细匹配信息失败 - resumeId: {}, jobId: {}, 错误: {}", 
                        result.getResumeId(), result.getJobId(), e.getMessage(), e);
            throw new RuntimeException("序列化匹配详情失败: " + e.getMessage(), e);
        } catch (Exception e) {
            // 检查是否是外键约束违反错误
            if (e.getMessage() != null && e.getMessage().contains("fkey")) {
                logger.error("外键约束违反 - resumeId: {} 或 jobId: {} 不存在。错误详情: {}", 
                            result.getResumeId(), result.getJobId(), e.getMessage());
                throw new IllegalArgumentException(
                    String.format("简历或岗位记录不存在 (resumeId: %s, jobId: %s)", 
                                 result.getResumeId(), result.getJobId()), e);
            }
            
            logger.error("插入匹配结果失败 - resumeId: {}, jobId: {}, 错误类型: {}, 错误信息: {}", 
                        result.getResumeId(), result.getJobId(), e.getClass().getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("保存匹配结果失败: " + e.getMessage(), e);
        }
    }

    private JobMatchingResult update(JobMatchingResult result) {
        String sql = """
            UPDATE job_matching_results 
            SET match_score = ?, match_level = ?, summary = ?, detail_matches = ?::jsonb,
                advantages = ?, gaps = ?, recommendations = ?, analysis_time = ?, 
                updated_at = CURRENT_TIMESTAMP
            WHERE resume_id = ? AND job_id = ?
            """;

        try {
            // 转换数组参数
            Object[] advantagesArray = result.getAdvantages() != null ? 
                result.getAdvantages().toArray(new String[0]) : new String[0];
            Object[] gapsArray = result.getGaps() != null ? 
                result.getGaps().toArray(new String[0]) : new String[0];
            Object[] recommendationsArray = result.getRecommendations() != null ? 
                result.getRecommendations().toArray(new String[0]) : new String[0];

            // 转换详细匹配信息为JSON
            String detailMatchesJson = null;
            if (result.getDetailMatches() != null) {
                detailMatchesJson = objectMapper.writeValueAsString(result.getDetailMatches());
            }

            // 设置分析时间
            Timestamp analysisTime = result.getAnalysisTime() != null ? 
                Timestamp.valueOf(result.getAnalysisTime()) : Timestamp.valueOf(LocalDateTime.now());

            jdbcTemplate.update(sql,
                result.getMatchScore(),
                result.getMatchLevel(),
                result.getSummary(),
                detailMatchesJson,
                advantagesArray,
                gapsArray,
                recommendationsArray,
                analysisTime,
                result.getResumeId(),
                result.getJobId()
            );

            logger.info("匹配结果更新成功 - resumeId: {}, jobId: {}, 匹配分数: {}", 
                       result.getResumeId(), result.getJobId(), result.getMatchScore());
            
            return result;
            
        } catch (JsonProcessingException e) {
            logger.error("序列化详细匹配信息失败 - resumeId: {}, jobId: {}, 错误: {}", 
                        result.getResumeId(), result.getJobId(), e.getMessage(), e);
            throw new RuntimeException("序列化匹配详情失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("更新匹配结果失败 - resumeId: {}, jobId: {}, 错误: {}", 
                        result.getResumeId(), result.getJobId(), e.getMessage(), e);
            throw new RuntimeException("更新匹配结果失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<JobMatchingResult> findByResumeIdAndJobId(String resumeId, String jobId) {
        logger.debug("查询匹配结果 - resumeId: {}, jobId: {}", resumeId, jobId);
        
        String sql = "SELECT * FROM job_matching_results WHERE resume_id = ? AND job_id = ?";
        List<JobMatchingResult> results = jdbcTemplate.query(sql, new JobMatchingResultRowMapper(), resumeId, jobId);
        
        if (results.isEmpty()) {
            logger.debug("未找到匹配结果 - resumeId: {}, jobId: {}", resumeId, jobId);
            return Optional.empty();
        } else {
            logger.debug("找到匹配结果 - resumeId: {}, jobId: {}", resumeId, jobId);
            return Optional.of(results.get(0));
        }
    }

    @Override
    public List<JobMatchingResult> findByResumeId(String resumeId) {
        String sql = "SELECT * FROM job_matching_results WHERE resume_id = ? ORDER BY match_score DESC";
        List<JobMatchingResult> results = jdbcTemplate.query(sql, new JobMatchingResultRowMapper(), resumeId);
        logger.info("按简历ID查询匹配结果完成 - resumeId: {}, 结果数量: {}", resumeId, results.size());
        return results;
    }

    @Override
    public List<JobMatchingResult> findByJobId(String jobId) {
        String sql = "SELECT * FROM job_matching_results WHERE job_id = ? ORDER BY match_score DESC";
        List<JobMatchingResult> results = jdbcTemplate.query(sql, new JobMatchingResultRowMapper(), jobId);
        logger.info("按岗位ID查询匹配结果完成 - jobId: {}, 结果数量: {}", jobId, results.size());
        return results;
    }

    @Override
    public List<JobMatchingResult> findByMatchLevel(String matchLevel) {
        String sql = "SELECT * FROM job_matching_results WHERE match_level = ? ORDER BY match_score DESC";
        List<JobMatchingResult> results = jdbcTemplate.query(sql, new JobMatchingResultRowMapper(), matchLevel);
        logger.info("按匹配等级查询结果完成 - matchLevel: {}, 结果数量: {}", matchLevel, results.size());
        return results;
    }

    @Override
    public List<JobMatchingResult> findByMatchScoreGreaterThan(double minScore) {
        String sql = "SELECT * FROM job_matching_results WHERE match_score > ? ORDER BY match_score DESC";
        List<JobMatchingResult> results = jdbcTemplate.query(sql, new JobMatchingResultRowMapper(), minScore);
        logger.info("按最低分数查询结果完成 - minScore: {}, 结果数量: {}", minScore, results.size());
        return results;
    }

    @Override
    public void deleteByResumeIdAndJobId(String resumeId, String jobId) {
        String sql = "DELETE FROM job_matching_results WHERE resume_id = ? AND job_id = ?";
        int deletedRows = jdbcTemplate.update(sql, resumeId, jobId);
        logger.info("删除匹配结果完成 - resumeId: {}, jobId: {}, 删除行数: {}", resumeId, jobId, deletedRows);
    }

    @Override
    public void deleteByResumeId(String resumeId) {
        String sql = "DELETE FROM job_matching_results WHERE resume_id = ?";
        int deletedRows = jdbcTemplate.update(sql, resumeId);
        logger.info("删除简历的所有匹配结果完成 - resumeId: {}, 删除行数: {}", resumeId, deletedRows);
    }

    @Override
    public void deleteByJobId(String jobId) {
        String sql = "DELETE FROM job_matching_results WHERE job_id = ?";
        int deletedRows = jdbcTemplate.update(sql, jobId);
        logger.info("删除岗位的所有匹配结果完成 - jobId: {}, 删除行数: {}", jobId, deletedRows);
    }

    /**
     * 检查匹配结果是否存在
     */
    private boolean existsByResumeIdAndJobId(String resumeId, String jobId) {
        String sql = "SELECT COUNT(*) FROM job_matching_results WHERE resume_id = ? AND job_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, resumeId, jobId);
        boolean exists = count != null && count > 0;
        logger.debug("检查匹配结果存在性 - resumeId: {}, jobId: {}, 存在: {}", resumeId, jobId, exists);
        return exists;
    }

    /**
     * JobMatchingResult行映射器
     */
    private class JobMatchingResultRowMapper implements RowMapper<JobMatchingResult> {
        @Override
        public JobMatchingResult mapRow(ResultSet rs, int rowNum) throws SQLException {
            JobMatchingResult result = new JobMatchingResult();
            result.setResumeId(rs.getString("resume_id"));
            result.setJobId(rs.getString("job_id"));
            result.setMatchScore(rs.getDouble("match_score"));
            result.setMatchLevel(rs.getString("match_level"));
            result.setSummary(rs.getString("summary"));

            // 处理时间字段
            Timestamp analysisTime = rs.getTimestamp("analysis_time");
            if (analysisTime != null) {
                result.setAnalysisTime(analysisTime.toLocalDateTime());
            }

            // 处理数组字段
            Array advantagesArray = rs.getArray("advantages");
            if (advantagesArray != null) {
                String[] advantages = (String[]) advantagesArray.getArray();
                result.setAdvantages(Arrays.asList(advantages));
            }

            Array gapsArray = rs.getArray("gaps");
            if (gapsArray != null) {
                String[] gaps = (String[]) gapsArray.getArray();
                result.setGaps(Arrays.asList(gaps));
            }

            Array recommendationsArray = rs.getArray("recommendations");
            if (recommendationsArray != null) {
                String[] recommendations = (String[]) recommendationsArray.getArray();
                result.setRecommendations(Arrays.asList(recommendations));
            }

            // 处理JSON字段
            String detailMatchesJson = rs.getString("detail_matches");
            if (detailMatchesJson != null && !detailMatchesJson.trim().isEmpty()) {
                try {
                    JobMatchingResult.MatchDetail detailMatch = objectMapper.readValue(detailMatchesJson, JobMatchingResult.MatchDetail.class);
                    result.setDetailMatches(detailMatch);
                } catch (Exception e) {
                    logger.warn("解析详细匹配信息失败 - resumeId: {}, jobId: {}, JSON: {}, 错误: {}",
                               result.getResumeId(), result.getJobId(), detailMatchesJson, e.getMessage());
                }
            }

            return result;
        }
    }
}