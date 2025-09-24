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

package com.alibaba.cloud.ai.example.rag.service;

import com.alibaba.cloud.ai.example.rag.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 简历删除服务
 * 处理简历及相关数据的完整删除
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@Service
public class ResumeDeleteService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeDeleteService.class);

    private final ResumeRepository resumeRepository;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeDeleteService(ResumeRepository resumeRepository,
                               VectorStore vectorStore,
                               JdbcTemplate jdbcTemplate, ResumeAnalysisService resumeAnalysisService) {
        this.resumeRepository = resumeRepository;
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.resumeAnalysisService = resumeAnalysisService;
    }

    /**
     * 完整删除简历及其所有相关数据
     * 使用事务确保数据一致性
     * 
     * @param resumeId 简历ID
     * @throws RuntimeException 如果删除过程中出现错误
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteResumeCompletely(String resumeId) {
        try {
            // 验证简历是否存在
            if (!resumeRepository.existsById(resumeId)) {
                logger.error("删除失败 - 简历不存在: {}", resumeId);
                throw new IllegalArgumentException("简历不存在: " + resumeId);
            }

            logger.info("开始完整删除简历 - resumeId: {}", resumeId);

            // 1. 删除向量数据库中的相关文档
            logger.info("步骤1: 删除向量数据库文档 - resumeId: {}", resumeId);
            deleteVectorStoreDocuments(resumeId);

            // 2. 删除document_embeddings表中的记录（如果存在）
            logger.info("步骤2: 删除document_embeddings表记录 - resumeId: {}", resumeId);
            deleteDocumentEmbeddings(resumeId);

            resumeAnalysisService.deleteAnalysis(resumeId);


            // 3. 删除数据库中的简历记录
            // 由于设置了ON DELETE CASCADE，以下表的相关记录会自动删除：
            // - personal_info
            // - work_experiences
            // - education_info
            // - resume_skills
            // - resume_analysis
            // - job_matching_results
            logger.info("步骤3: 删除数据库简历记录（含级联删除） - resumeId: {}", resumeId);
            resumeRepository.deleteById(resumeId);

            logger.info("简历删除完成 - resumeId: {}", resumeId);

        } catch (Exception e) {
            logger.error("删除简历失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            throw new RuntimeException("删除简历失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除向量数据库中的文档
     */
    private void deleteVectorStoreDocuments(String resumeId) {
        try {
            logger.info("开始从向量数据库删除文档 - resumeId: {}", resumeId);
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            Filter.Expression filter = builder.eq("resumeId", resumeId).build();
            vectorStore.delete(filter);
            logger.info("已从向量数据库删除文档 - resumeId: {}", resumeId);
        } catch (Exception e) {
            logger.error("从向量数据库删除文档时出错 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 删除document_embeddings表中的记录
     */
    private void deleteDocumentEmbeddings(String resumeId) {
        try {
            String sql = "DELETE FROM document_embeddings WHERE document_id = ? AND document_type = 'RESUME'";
            int deletedRows = jdbcTemplate.update(sql, resumeId);
            logger.info("从 document_embeddings 表删除了 {} 条记录 - resumeId: {}", deletedRows, resumeId);
        } catch (Exception e) {
            logger.warn("从 document_embeddings 表删除记录时出错 - resumeId: {}, 错误: {} (可能表不存在或无相关记录)", 
                resumeId, e.getMessage());
            // 这里不抛出异常，因为可能表不存在或者没有相关记录
        }
    }

    /**
     * 检查简历是否存在
     */
    public boolean resumeExists(String resumeId) {
        return resumeRepository.existsById(resumeId);
    }

    /**
     * 获取简历的相关数据统计（用于删除前确认）
     */
    public ResumeDataSummary getResumeDataSummary(String resumeId) {
        if (!resumeExists(resumeId)) {
            logger.warn("获取数据统计失败 - 简历不存在: {}", resumeId);
            return null;
        }

        logger.info("开始统计简历相关数据 - resumeId: {}", resumeId);
        ResumeDataSummary summary = new ResumeDataSummary();
        summary.setResumeId(resumeId);

        try {
            // 统计各表的相关记录数
            int personalInfoCount = countRecords("personal_info", resumeId);
            int workExperienceCount = countRecords("work_experiences", resumeId);
            int educationCount = countRecords("education_info", resumeId);
            int skillsCount = countRecords("resume_skills", resumeId);
            int analysisCount = countRecords("resume_analysis", resumeId);
            int matchingResultsCount = countRecords("job_matching_results", resumeId);
            int embeddingsCount = countEmbeddings(resumeId);
            
            summary.setPersonalInfoCount(personalInfoCount);
            summary.setWorkExperienceCount(workExperienceCount);
            summary.setEducationCount(educationCount);
            summary.setSkillsCount(skillsCount);
            summary.setAnalysisCount(analysisCount);
            summary.setMatchingResultsCount(matchingResultsCount);
            summary.setEmbeddingsCount(embeddingsCount);
            
            logger.info("数据统计完成 - resumeId: {}, 个人信息: {}, 工作经历: {}, 教育: {}, 技能: {}, 分析: {}, 匹配: {}, 嵌入: {}, 总计: {}", 
                resumeId, personalInfoCount, workExperienceCount, educationCount, 
                skillsCount, analysisCount, matchingResultsCount, embeddingsCount, summary.getTotalRecords());
        } catch (Exception e) {
            logger.error("获取简历数据统计时出错 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
        }

        return summary;
    }

    private int countRecords(String tableName, String resumeId) {
        try {
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE resume_id = ?";
            int count = jdbcTemplate.queryForObject(sql, Integer.class, resumeId);
            logger.debug("表 {} 中 resumeId {} 的记录数: {}", tableName, resumeId, count);
            return count;
        } catch (Exception e) {
            logger.warn("统计表 {} 记录数时出错 - resumeId: {}, 错误: {}", tableName, resumeId, e.getMessage());
            return 0;
        }
    }

    private int countEmbeddings(String resumeId) {
        try {
            String sql = "SELECT COUNT(*) FROM document_embeddings WHERE document_id = ? AND document_type = 'RESUME'";
            int count = jdbcTemplate.queryForObject(sql, Integer.class, resumeId);
            logger.debug("document_embeddings 表中 resumeId {} 的记录数: {}", resumeId, count);
            return count;
        } catch (Exception e) {
            logger.warn("统计 document_embeddings 表记录数时出错 - resumeId: {}, 错误: {}", resumeId, e.getMessage());
            return 0;
        }
    }

    /**
     * 简历数据摘要类
     */
    public static class ResumeDataSummary {
        private String resumeId;
        private int personalInfoCount;
        private int workExperienceCount;
        private int educationCount;
        private int skillsCount;
        private int analysisCount;
        private int matchingResultsCount;
        private int embeddingsCount;

        // Getters and Setters
        public String getResumeId() { return resumeId; }
        public void setResumeId(String resumeId) { this.resumeId = resumeId; }

        public int getPersonalInfoCount() { return personalInfoCount; }
        public void setPersonalInfoCount(int personalInfoCount) { this.personalInfoCount = personalInfoCount; }

        public int getWorkExperienceCount() { return workExperienceCount; }
        public void setWorkExperienceCount(int workExperienceCount) { this.workExperienceCount = workExperienceCount; }

        public int getEducationCount() { return educationCount; }
        public void setEducationCount(int educationCount) { this.educationCount = educationCount; }

        public int getSkillsCount() { return skillsCount; }
        public void setSkillsCount(int skillsCount) { this.skillsCount = skillsCount; }

        public int getAnalysisCount() { return analysisCount; }
        public void setAnalysisCount(int analysisCount) { this.analysisCount = analysisCount; }

        public int getMatchingResultsCount() { return matchingResultsCount; }
        public void setMatchingResultsCount(int matchingResultsCount) { this.matchingResultsCount = matchingResultsCount; }

        public int getEmbeddingsCount() { return embeddingsCount; }
        public void setEmbeddingsCount(int embeddingsCount) { this.embeddingsCount = embeddingsCount; }

        public int getTotalRecords() {
            return personalInfoCount + workExperienceCount + educationCount + 
                   skillsCount + analysisCount + matchingResultsCount + embeddingsCount;
        }

        @Override
        public String toString() {
            return String.format(
                "ResumeDataSummary{resumeId='%s', personal=%d, work=%d, education=%d, skills=%d, analysis=%d, matching=%d, embeddings=%d, total=%d}",
                resumeId, personalInfoCount, workExperienceCount, educationCount, 
                skillsCount, analysisCount, matchingResultsCount, embeddingsCount, getTotalRecords()
            );
        }
    }
}