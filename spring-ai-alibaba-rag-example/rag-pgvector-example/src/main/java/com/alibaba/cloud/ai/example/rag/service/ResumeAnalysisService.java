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

import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.example.rag.model.ResumeAnalysis;
import com.alibaba.cloud.ai.example.rag.model.Resume;
import com.alibaba.cloud.ai.example.rag.model.ResumeStatus;
import com.alibaba.cloud.ai.example.rag.model.AssessmentScore;
import com.alibaba.cloud.ai.example.rag.repository.ResumeRepository;
import com.alibaba.cloud.ai.example.rag.repository.ResumeAnalysisRepository;
import com.alibaba.cloud.ai.model.RerankModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 简历分析服务
 * 提供基于RAG的简历智能分析功能
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@Service
public class ResumeAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeAnalysisService.class);

    @Value("classpath:/prompts/resume-analysis.st")
    private Resource analysisPromptResource;

    @Value("classpath:/prompts/resume-chat.st")
    private Resource chatPromptResource;

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final RerankModel rerankModel;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;

    public ResumeAnalysisService(VectorStore vectorStore, ChatModel chatModel, 
                               RerankModel rerankModel, ObjectMapper objectMapper,
                               ResumeRepository resumeRepository,
                               ResumeAnalysisRepository resumeAnalysisRepository) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.rerankModel = rerankModel;
        this.objectMapper = objectMapper;
        this.chatClient = ChatClient.builder(chatModel).build();
        this.resumeRepository = resumeRepository;
        this.resumeAnalysisRepository = resumeAnalysisRepository;
    }

    /**
     * 分析简历
     * 
     * @param resumeId 简历ID
     * @return 分析结果
     */
    public ResumeAnalysis analyzeResume(String resumeId) {
        logger.info("开始分析简历 - resumeId: {}", resumeId);
        
        try {
            // 1. 检查简历是否存在
            Resume resume = resumeRepository.findById(resumeId).orElse(null);
            if (resume == null) {
                logger.warn("分析失败 - 简历不存在: {}", resumeId);
                return null;
            }

            logger.info("开始生成简历分析 - resumeId: {}, 候选人: {}", resumeId, resume.getCandidateName());

            // 2. 检查是否已有分析结果
            Optional<ResumeAnalysis> existingAnalysis = resumeAnalysisRepository.findByResumeId(resumeId);
            if (existingAnalysis.isPresent()) {
                logger.info("返回已存在的分析结果 - resumeId: {}", resumeId);
                return existingAnalysis.get();
            }
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            Filter.Expression filter = builder.eq("resumeId", resumeId).build();

            RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                    .documentRetriever(VectorStoreDocumentRetriever.builder()
                            .vectorStore(vectorStore)
                            .filterExpression(filter)
                            .build())
                    .build();

            logger.info("开始检索简历文档 - resumeId: {}", resumeId);

            // 4. 获取分析提示词模板
            String analysisPrompt = analysisPromptResource.getContentAsString(StandardCharsets.UTF_8);
            
            logger.info("开始调用AI进行简历分析 - resumeId: {}, prompt长度: {} 字符", 
                resumeId, analysisPrompt.length());

            // 5. 使用RAG进行简历分析
            ResumeAnalysis analysis = ChatClient.builder(chatModel)
                .defaultAdvisors(retrievalAugmentationAdvisor)
                .build()
                .prompt()
                .user("请对这份简历进行全面分析")
                .call()
                .entity(ResumeAnalysis.class);
            
            logger.info("AI分析完成 - resumeId: {}, 结果长度: {} 字符", resumeId, 
                analysis != null ? analysis.getAssessmentScore() != null ? analysis.getAssessmentScore().getOverallScore() : "null" : "null");


            // 7. 保存分析结果到数据库
            logger.info("开始保存分析结果到数据库 - resumeId: {}", resumeId);
            analysis.setResumeId(resumeId);
            ResumeAnalysis savedAnalysis = resumeAnalysisRepository.save(analysis);

            // 8. 更新简历状态
            resume.setStatus(ResumeStatus.ANALYZED);
            resumeRepository.save(resume);
            logger.info("简历状态已更新 - resumeId: {}, 新状态: {}", resumeId, ResumeStatus.ANALYZED);

            logger.info("简历分析完成 - resumeId: {}, 综合评分: {}", resumeId, 
                savedAnalysis.getAssessmentScore() != null ? 
                    savedAnalysis.getAssessmentScore().getOverallScore() : "null");
            return savedAnalysis;
            
        } catch (IOException e) {
            logger.error("读取分析提示词模板失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            throw new RuntimeException("读取分析提示词模板失败", e);
        } catch (Exception e) {
            logger.error("简历分析失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            throw new RuntimeException("简历分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 简历问答
     * 
     * @param resumeId 简历ID
     * @param question 问题
     * @return 回答
     */
    public String chatAboutResume(String resumeId, String question) {
        try {
            // 1. 构建搜索请求
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            Filter.Expression filter = builder.eq("resumeId", resumeId).build();
            
            SearchRequest searchRequest = SearchRequest.builder()
                .topK(5)
                .similarityThreshold(0.5)
                .filterExpression(filter)
                .build();

            // 2. 获取聊天提示词模板
            String chatPrompt = chatPromptResource.getContentAsString(StandardCharsets.UTF_8);
            
            // 3. 基于RAG回答问题
            return ChatClient.builder(chatModel)
                .defaultAdvisors(new RetrievalRerankAdvisor(vectorStore, rerankModel, searchRequest, 
                    new SystemPromptTemplate(chatPrompt), 0.1))
                .build()
                .prompt()
                .user(question)
                .call()
                .content();
                
        } catch (IOException e) {
            throw new RuntimeException("读取聊天提示词模板失败", e);
        } catch (Exception e) {
            throw new RuntimeException("简历问答失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取简历摘要
     * 
     * @param resumeId 简历ID
     * @return 摘要
     */
    public String getResumeSummary(String resumeId) {
        try {
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            Filter.Expression filter = builder.eq("resumeId", resumeId).build();
            
            SearchRequest searchRequest = SearchRequest.builder()
                .topK(10)
                .similarityThreshold(0.1)
                .filterExpression(filter)
                .build();

            String summaryPrompt = """
                请基于以下简历内容，生成一份100字以内的简历摘要，突出候选人的核心优势和特点。
                
                要求：
                1. 简洁明了，突出重点
                2. 包含关键技能和经验
                3. 体现候选人特色
                """;
            
            return ChatClient.builder(chatModel)
                .defaultAdvisors(new RetrievalRerankAdvisor(vectorStore, rerankModel, searchRequest, 
                    new SystemPromptTemplate(summaryPrompt), 0.1))
                .build()
                .prompt()
                .user("请生成这份简历的摘要")
                .call()
                .content();
                
        } catch (Exception e) {
            throw new RuntimeException("生成简历摘要失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析分析结果
     * 简化版本，实际应用中可以使用JSON解析或结构化输出
     */
    private ResumeAnalysis parseAnalysisResult(String resumeId, String analysisResult) {
        logger.info("开始解析AI分析结果 - resumeId: {}", resumeId);
        
        try {
            ResumeAnalysis analysis = new ResumeAnalysis(resumeId);
            analysis.setSummary(analysisResult);
            
            // 创建默认评分对象
            AssessmentScore score = new AssessmentScore();
            score.setExperienceScore(75);
            score.setSkillScore(80);
            score.setEducationScore(85);
            score.setOverallScore(80);
            score.setRecommendation("推荐");
            analysis.setAssessmentScore(score);
            
            // 设置默认优势和改进建议
            analysis.setStrengths(Arrays.asList("经验丰富", "技能全面"));
            analysis.setImprovements(Arrays.asList("可考虑提升项目管理能力"));
            
            logger.info("分析结果解析完成 - resumeId: {}, 综合评分: {}", 
                resumeId, score.getOverallScore());
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("解析分析结果失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            
            // 返回基本分析结果
            ResumeAnalysis analysis = new ResumeAnalysis(resumeId);
            analysis.setSummary("分析完成：" + analysisResult);
            return analysis;
        }
    }

    /**
     * 删除简历分析结果
     *
     * @param resumeId 简历ID
     *
     */
    public void deleteAnalysis(String resumeId) {
        logger.info("删除简历分析结果 - resumeId: {}", resumeId);
        resumeAnalysisRepository.deleteByResumeId(resumeId);
    }

    /**
     * 获取已存在的简历分析结果
     * 
     * @param resumeId 简历ID
     * @return 分析结果，如果不存在则返回null
     */
    public ResumeAnalysis getExistingAnalysis(String resumeId) {
        logger.info("获取已存在的分析结果 - resumeId: {}", resumeId);
        return resumeAnalysisRepository.findByResumeId(resumeId).orElse(null);
    }

}