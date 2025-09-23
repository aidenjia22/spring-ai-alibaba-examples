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
import com.alibaba.cloud.ai.example.rag.model.Job;
import com.alibaba.cloud.ai.example.rag.model.JobMatchingResult;
import com.alibaba.cloud.ai.example.rag.repository.JobRepository;
import com.alibaba.cloud.ai.example.rag.repository.JobMatchingResultRepository;
import com.alibaba.cloud.ai.model.RerankModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
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
import java.util.*;

/**
 * 简历岗位匹配服务
 * 基于RAG技术实现智能简历岗位匹配
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@Service
public class JobMatchingService {

    private static final Logger logger = LoggerFactory.getLogger(JobMatchingService.class);

    @Value("classpath:/prompts/job-matching.st")
    private Resource jobMatchingPromptResource;

    @Value("classpath:/prompts/job-recommendation.st")
    private Resource jobRecommendationPromptResource;

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final RerankModel rerankModel;
    private final ObjectMapper objectMapper;
    private final JobRepository jobRepository;
    private final JobMatchingResultRepository jobMatchingResultRepository;

    public JobMatchingService(VectorStore vectorStore, ChatModel chatModel, RerankModel rerankModel, 
                             JobRepository jobRepository, JobMatchingResultRepository jobMatchingResultRepository) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.rerankModel = rerankModel;
        this.objectMapper = new ObjectMapper();
        this.jobRepository = jobRepository;
        this.jobMatchingResultRepository = jobMatchingResultRepository;
    }

    /**
     * 为简历匹配岗位
     * 
     * @param resumeId 简历ID
     * @param jobId 岗位ID
     * @return 匹配结果
     */
    public JobMatchingResult matchResumeWithJob(String resumeId, String jobId) {
        logger.info("开始简历岗位匹配 - resumeId: {}, jobId: {}", resumeId, jobId);

        try {
            // 从数据库获取岗位信息
            Job job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                logger.warn("岗位不存在 - jobId: {}", jobId);
                throw new RuntimeException("岗位不存在: " + jobId);
            }
            
            // 构建搜索请求，获取简历内容
            FilterExpressionBuilder builder = new FilterExpressionBuilder();
            Filter.Expression filter = builder.eq("resumeId", resumeId).build();

            // 先验证是否能检索到简历内容
            logger.info("开始验证简历内容检索 - resumeId: {}", resumeId);
            List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .topK(5)
                .filterExpression(filter)
                .build());
            
            if (documents.isEmpty()) {
                logger.warn("未找到简历向量数据 - resumeId: {}", resumeId);
                // 返回默认结果
                JobMatchingResult defaultResult = new JobMatchingResult();
                defaultResult.setResumeId(resumeId);
                defaultResult.setJobId(jobId);
                defaultResult.setMatchScore(50.0);
                defaultResult.setMatchLevel("需验证");
                defaultResult.setSummary("未能找到该简历的向量数据，可能简历尚未上传或处理完成。请确认简历已成功上传并等待处理完成。");
                return defaultResult;
            }
            logger.info("成功检索到简历文档片段数: {} - resumeId: {}", documents.size(), resumeId);
            // 手动获取简历内容用于分析，确保获取到正确的简历信息
            StringBuilder resumeContent = new StringBuilder();
            for (Document doc : documents) {
                resumeContent.append(doc.getText()).append("\n");
            }
            logger.debug("提取到的简历内容长度: {} - resumeId: {}", resumeContent.length(), resumeId);
            // 获取匹配提示词模板
            String matchingPrompt = jobMatchingPromptResource.getContentAsString(StandardCharsets.UTF_8);
            // 构建岗位信息字符串
            String jobInfo = buildJobInfoString(job);
            logger.debug("执行RAG匹配分析 - 岗位信息: {}", jobInfo);
            Optional<JobMatchingResult> existingResult = jobMatchingResultRepository.findByResumeIdAndJobId(resumeId, jobId);
            if (existingResult.isPresent()) {
                logger.info("使用已缓存的匹配结果 - resumeId: {}, jobId: {}, matchScore: {}",
                           resumeId, jobId, existingResult.get().getMatchScore());
                return existingResult.get();
            }
            // 直接使用简历内容和岗位信息进行匹配分析（不使用RAG检索）
            String userPrompt = String.format("请根据以下简历内容对此岗位进行匹配度分析。\n\n简历内容：\n%s\n\n岗位信息：\n%s", 
                resumeContent, jobInfo);
            try {
                // 使用entity方法获取结构化结果
                JobMatchingResult result = ChatClient.builder(chatModel)
                    .defaultSystem(matchingPrompt)
                    .build()
                    .prompt()
                    .user(userPrompt)
                    .call()
                    .entity(JobMatchingResult.class);
                logger.debug("AI返回结构化结果: {}", result);
                // 设置基本信息
                result.setResumeId(resumeId);
                result.setJobId(jobId);
                result.setAnalysisTime(java.time.LocalDateTime.now());
                logger.info("简历岗位匹配完成 - resumeId: {}, jobId: {}, matchScore: {}", 
                           resumeId, jobId, result.getMatchScore());
                // 保存匹配结果到数据库
                try {
                    JobMatchingResult savedResult = jobMatchingResultRepository.save(result);
                    logger.info("匹配结果已保存到数据库 - resumeId: {}, jobId: {}, matchScore: {}", 
                               resumeId, jobId, savedResult.getMatchScore());
                    return savedResult;
                } catch (Exception e) {
                    logger.warn("保存匹配结果失败 - resumeId: {}, jobId: {}, 错误: {}", 
                               resumeId, jobId, e.getMessage());
                    return result;
                }
                        
            } catch (Exception e) {
                logger.error("匹配分析完全失败，返回默认结果 - resumeId: {}, jobId: {}, 错误: {}",
                           resumeId, jobId, e.getMessage(), e);
                JobMatchingResult defaultResult = new JobMatchingResult();
                defaultResult.setResumeId(resumeId);
                defaultResult.setJobId(jobId);
                defaultResult.setMatchScore(65.0);
                defaultResult.setMatchLevel("一般");
                defaultResult.setSummary("由于系统分析异常，无法提供详细匹配分析。建议手动检查简历与岗位要求的匹配度。");
                defaultResult.setAnalysisTime(java.time.LocalDateTime.now());
                return defaultResult;
            }
        } catch (IOException e) {
            logger.error("读取匹配提示词模板失败 - resumeId: {}, jobId: {}", resumeId, jobId, e);
            throw new RuntimeException("读取匹配提示词模板失败", e);
        } catch (Exception e) {
            logger.error("简历岗位匹配失败 - resumeId: {}, jobId: {}, 错误: {}", resumeId, jobId, e.getMessage(), e);
            throw new RuntimeException("简历岗位匹配失败: " + e.getMessage(), e);
        }
    }

    

    /**
     * 为简历推荐合适的岗位
     * 
     * @param resumeId 简历ID
     * @param topK 返回推荐岗位数量
     * @return 岗位推荐列表
     */
    public List<JobMatchingResult> recommendJobsForResume(String resumeId, int topK) {
        logger.info("开始为简历推荐岗位 - resumeId: {}, topK: {}", resumeId, topK);
        
        List<JobMatchingResult> recommendations = new ArrayList<>();
        
        try {
            // 从数据库查询活跃岗位
            List<Job> activeJobs = jobRepository.findActiveJobs();
            logger.debug("找到活跃岗位数量: {}", activeJobs.size());
            
            // 为每个活跃岗位计算匹配度
            for (Job job : activeJobs) {
                try {
                    // 检查是否已有匹配结果
                    Optional<JobMatchingResult> existingResult = jobMatchingResultRepository.findByResumeIdAndJobId(resumeId, job.getJobId());
                    if (existingResult.isPresent()) {
                        recommendations.add(existingResult.get());
                        logger.debug("使用已有匹配结果 - jobId: {}, 匹配分数: {}", 
                                   job.getJobId(), existingResult.get().getMatchScore());
                    } else {
                        JobMatchingResult result = matchResumeWithJob(resumeId, job.getJobId());
                        recommendations.add(result);
                        logger.debug("新匹配完成 - jobId: {}, 匹配分数: {}", 
                                   job.getJobId(), result.getMatchScore());
                    }
                } catch (Exception e) {
                    logger.warn("岗位匹配失败，忽略该岗位 - jobId: {}, 错误: {}", 
                               job.getJobId(), e.getMessage());
                }
            }
            
            // 按匹配分数排序并返回前 topK 个结果
            List<JobMatchingResult> sortedRecommendations = recommendations.stream()
                .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
                .limit(topK)
                .collect(java.util.stream.Collectors.toList());
            
            logger.info("简历岗位推荐完成 - resumeId: {}, 推荐数量: {}, 最高匹配分数: {}",
                       resumeId, sortedRecommendations.size(), 
                       sortedRecommendations.isEmpty() ? 0 : sortedRecommendations.get(0).getMatchScore());
            
            return sortedRecommendations;
            
        } catch (Exception e) {
            logger.error("简历岗位推荐失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            return recommendations; // 返回已有的部分结果
        }
    }

    /**
     * 获取所有岗位列表
     */
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    /**
     * 根据ID获取岗位
     */
    public Job getJobById(String jobId) {
        return jobRepository.findById(jobId).orElse(null);
    }

    /**
     * 添加新岗位
     */
    public Job addJob(Job job) {
        if (job.getJobId() == null) {
            job.setJobId(UUID.randomUUID().toString());
        }
        return jobRepository.save(job);
    }

    /**
     * 更新岗位
     */
    public Job updateJob(Job job) {
        job.setUpdateTime(java.time.LocalDateTime.now());
        if (jobRepository.existsById(job.getJobId())) {
            return jobRepository.save(job);
        }
        throw new RuntimeException("岗位不存在: " + job.getJobId());
    }

    /**
     * 删除岗位
     */
    public boolean deleteJob(String jobId) {
        if (jobRepository.existsById(jobId)) {
            jobRepository.deleteById(jobId);
            return true;
        }
        return false;
    }
    
    /**
     * 从数据库获取匹配结果
     * 
     * @param resumeId 简历ID
     * @param jobId 岗位ID
     * @return 匹配结果
     */
    public Optional<JobMatchingResult> getMatchingResultFromDatabase(String resumeId, String jobId) {
        logger.debug("从数据库查询匹配结果 - resumeId: {}, jobId: {}", resumeId, jobId);
        return jobMatchingResultRepository.findByResumeIdAndJobId(resumeId, jobId);
    }
    
    /**
     * 获取简历的所有匹配结果
     * 
     * @param resumeId 简历ID
     * @return 匹配结果列表
     */
    public List<JobMatchingResult> getAllMatchingResultsForResume(String resumeId) {
        logger.debug("获取简历的所有匹配结果 - resumeId: {}", resumeId);
        return jobMatchingResultRepository.findByResumeId(resumeId);
    }
    
    /**
     * 获取岗位的所有匹配结果
     * 
     * @param jobId 岗位ID
     * @return 匹配结果列表
     */
    public List<JobMatchingResult> getAllMatchingResultsForJob(String jobId) {
        logger.debug("获取岗位的所有匹配结果 - jobId: {}", jobId);
        return jobMatchingResultRepository.findByJobId(jobId);
    }
    
    /**
     * 删除匹配结果
     * 
     * @param resumeId 简历ID
     * @param jobId 岗位ID
     */
    public void deleteMatchingResult(String resumeId, String jobId) {
        logger.info("删除匹配结果 - resumeId: {}, jobId: {}", resumeId, jobId);
        jobMatchingResultRepository.deleteByResumeIdAndJobId(resumeId, jobId);
    }

    /**
     * 构建岗位信息字符串
     */
    private String buildJobInfoString(Job job) {
        StringBuilder sb = new StringBuilder();
        sb.append("岗位标题: ").append(job.getTitle()).append("\n");
        sb.append("公司名称: ").append(job.getCompany()).append("\n");
        
        if (job.getDepartment() != null) {
            sb.append("部门: ").append(job.getDepartment()).append("\n");
        }
        if (job.getLocation() != null) {
            sb.append("工作地点: ").append(job.getLocation()).append("\n");
        }
        if (job.getEmploymentType() != null) {
            sb.append("工作类型: ").append(job.getEmploymentType()).append("\n");
        }
        if (job.getExperience() != null) {
            sb.append("经验要求: ").append(job.getExperience()).append("\n");
        }
        if (job.getEducation() != null) {
            sb.append("学历要求: ").append(job.getEducation()).append("\n");
        }
        if (job.getSalaryRange() != null) {
            sb.append("薪资范围: ").append(job.getSalaryRange()).append("\n");
        }
        if (job.getDescription() != null) {
            sb.append("岗位描述: ").append(job.getDescription()).append("\n");
        }
        
        if (job.getResponsibilities() != null && !job.getResponsibilities().isEmpty()) {
            sb.append("工作职责:\n");
            job.getResponsibilities().forEach(resp -> sb.append("- ").append(resp).append("\n"));
        }
        
        if (job.getRequirements() != null && !job.getRequirements().isEmpty()) {
            sb.append("任职要求:\n");
            job.getRequirements().forEach(req -> sb.append("- ").append(req).append("\n"));
        }
        
        if (job.getSkills() != null && !job.getSkills().isEmpty()) {
            sb.append("技能要求:\n");
            job.getSkills().forEach(skill -> sb.append("- ").append(skill).append("\n"));
        }
        
        return sb.toString();
    }

    /**
     * 从文本中提取列表信息
     */
    private List<String> extractListFromText(String text, String... keywords) {
        List<String> items = new ArrayList<>();
        
        for (String keyword : keywords) {
            int index = text.indexOf(keyword);
            if (index != -1) {
                // 查找关键词后的内容
                String afterKeyword = text.substring(index);
                String[] lines = afterKeyword.split("\n");
                
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("-") || line.matches("\\\\d+\\\\.")) {
                        String item = line.replaceFirst("^[-\\\\d\\\\.\\\\s]+", "").trim();
                        if (!item.isEmpty() && item.length() > 2) {
                            items.add(item);
                        }
                    }
                }
                
                if (!items.isEmpty()) {
                    break; // 找到内容后停止搜索
                }
            }
        }
        
        return items.isEmpty() ? Arrays.asList("未找到相关信息") : items;
    }
    
    /**
     * 使用语义检索的简历岗位匹配方法（改进版）
     * 解决原方法的以下问题：
     * 1. 检索范围有限：使用语义相似性检索而不是简单的resumeId过滤
     * 2. 智能筛选：对检索到的内容进行相关性排序和筛选
     * 3. 真正的RAG检索：使用RAG Advisor进行智能检索和重排序
     * 
     * @param resumeId 简历ID
     * @param jobId 岗位ID
     * @return 匹配结果
     */
    public JobMatchingResult matchResumeWithJobUsingSemanticSearch(String resumeId, String jobId) {
        logger.info("开始语义检索简历岗位匹配 - resumeId: {}, jobId: {}", resumeId, jobId);
        
        try {
            // 从数据库获取岗位信息
            Job job = jobRepository.findById(jobId).orElse(null);
            if (job == null) {
                logger.warn("岗位不存在 - jobId: {}", jobId);
                throw new RuntimeException("岗位不存在: " + jobId);
            }
            
            // 检查是否已有缓存的匹配结果
            Optional<JobMatchingResult> existingResult = jobMatchingResultRepository.findByResumeIdAndJobId(resumeId, jobId);
            if (existingResult.isPresent()) {
                logger.info("使用已缓存的匹配结果 - resumeId: {}, jobId: {}, matchScore: {}",
                           resumeId, jobId, existingResult.get().getMatchScore());
                return existingResult.get();
            }
            // 构建岗位信息字符串
            String jobInfo = buildJobInfoString(job);
            // 获取匹配提示词模板
            String matchingPrompt = jobMatchingPromptResource.getContentAsString(StandardCharsets.UTF_8);
            String userPrompt = String.format(
                "请根据上下文中检索到的简历内容和以下岗位信息进行匹配分析。\n\n" +
                "岗位信息：\n%s\n\n" +
                "分析要求：\n" +
                "1. 使用上下文中的简历内容进行分析\n" +
                "2. 给出匹配分数（matchScore）和匹配等级（matchLevel）\n" +
                "3. 提供具体的优势、差距和建议\n" +
                "4. 如果上下文中没有简历内容，请在summary中说明“没有获取到简历信息”", 
                jobInfo);
            
            try {
                // 使用手动检索和上下文注入的方式实现RAG
                logger.info("开始使用手动RAG检索进行匹配分析 - resumeId: {}, jobId: {}", resumeId, jobId);
                FilterExpressionBuilder b = new FilterExpressionBuilder();
                Filter.Expression expression = b.eq("resumeId", resumeId).build();
                RetrievalAugmentationAdvisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .filterExpression(expression)
                                .build())
                        .build();
                JobMatchingResult result = ChatClient.builder(chatModel)
                    .defaultAdvisors(retrievalAugmentationAdvisor)
                    .defaultSystem(matchingPrompt)
                    .build()
                    .prompt()
                    .user(userPrompt)
                    .call()
                    .entity(JobMatchingResult.class);
                // 设置基本信息
                result.setResumeId(resumeId);
                result.setJobId(jobId);
                result.setAnalysisTime(java.time.LocalDateTime.now());

                logger.info("语义检索匹配完成 - resumeId: {}, jobId: {}, matchScore: {}",
                        resumeId, jobId, result.getMatchScore());
                
                // 保存匹配结果到数据库
                try {
                    JobMatchingResult savedResult = jobMatchingResultRepository.save(result);
                    logger.info("语义检索匹配结果已保存 - resumeId: {}, jobId: {}, matchScore: {}", 
                               resumeId, jobId, savedResult.getMatchScore());
                    return savedResult;
                } catch (Exception e) {
                    logger.warn("保存匹配结果失败 - resumeId: {}, jobId: {}, 错误: {}", 
                               resumeId, jobId, e.getMessage());
                    return result;
                }
                
            } catch (Exception ragException) {
                logger.warn("RAG语义检索失败，使用传统方法降级 - resumeId: {}, jobId: {}, 错误: {}",
                           resumeId, jobId, ragException.getMessage());
                
                // 降级到传统的ID过滤方法
                return matchResumeWithJob(resumeId, jobId);
            }
            
        } catch (IOException e) {
            logger.error("读取匹配提示词模板失败 - resumeId: {}, jobId: {}", resumeId, jobId, e);
            throw new RuntimeException("读取匹配提示词模板失败", e);
        } catch (Exception e) {
            logger.error("语义检索匹配失败 - resumeId: {}, jobId: {}, 错误: {}", resumeId, jobId, e.getMessage(), e);
            throw new RuntimeException("语义检索匹配失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 基于岗位要求构建语义检索查询
     */
    private String buildSemanticQuery(Job job, String resumeId) {
        StringBuilder queryBuilder = new StringBuilder();
        // 添加简历ID
        // 添加岗位标题和关键技能
        if (job.getTitle() != null) {
            queryBuilder.append(job.getTitle()).append(" ");
        }
        
        // 添加技能要求
        if (job.getSkills() != null && !job.getSkills().isEmpty()) {
            job.getSkills().forEach(skill -> queryBuilder.append(skill).append(" "));
        }
        
        // 添加公司信息
        if (job.getCompany() != null) {
            queryBuilder.append(job.getCompany()).append(" ");
        }
        
        String query = queryBuilder.toString().trim();
        
        // 如果查询为空，使用岗位描述
        if (query.isEmpty() && job.getDescription() != null) {
            query = job.getDescription();
        }
        
        return query.isEmpty() ? "简历" : query;
    }
    
    /**
     * 创建降级结果
     */
    private JobMatchingResult createFallbackResult(String resumeId, String jobId, String reason) {
        JobMatchingResult result = new JobMatchingResult();
        result.setResumeId(resumeId);
        result.setJobId(jobId);
        result.setMatchScore(60.0);
        result.setMatchLevel("FAIR");
        result.setSummary("由于" + reason + "，无法提供详细匹配分析。建议手动检查简历与岗位要求的匹配度。");
        result.setAdvantages(Arrays.asList("需要进一步人工分析"));
        result.setGaps(Arrays.asList("系统分析受限"));
        result.setRecommendations(Arrays.asList("建议手动审查匹配度"));
        result.setAnalysisTime(java.time.LocalDateTime.now());
        return result;
    }
    
    /**
     * 标准化匹配等级，将中文转换为英文
     */
    private String normalizeMatchLevel(String matchLevel) {
        if (matchLevel == null) {
            return "FAIR";
        }
        
        switch (matchLevel) {
            case "优秀":
            case "高匹配":
                return "EXCELLENT";
            case "良好":
            case "中等匹配":
                return "GOOD";
            case "一般":
            case "低匹配":
                return "FAIR";
            case "较差":
                return "POOR";
            default:
                return matchLevel; // 如果已经是英文，直接返回
        }
    }
    
    /**
     * 从分析结果中提取匹配分数
     * 支持多种格式的分数表示
     */
    private double extractMatchScore(String analysisResult) {
        // 简单的分数提取逻辑
        // 实际项目中可以使用更复杂的NLP技术或结构化输出
        
        // 查找包含数字的模式
        String[] patterns = {
            "\\d+分",        // XX分
            "\\d+%",        // XX%
            "\\d+\\.\\d+分", // XX.X分
            "\\d+\\.\\d+%",  // XX.X%
            "分数[::：]?\\s*\\d+",  // 分数：XX
            "匹配度[::：]?\\s*\\d+" // 匹配度：XX
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(analysisResult);
            
            if (m.find()) {
                String match = m.group();
                String numberStr = match.replaceAll("[^\\\\d\\\\.]", "");
                try {
                    double score = Double.parseDouble(numberStr);
                    if (score <= 100) {
                        logger.debug("提取到匹配分数: {}, 原文: {}", score, match);
                        return score;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        
        // 根据关键词判断分数范围
        String lowerCase = analysisResult.toLowerCase();
        if (lowerCase.contains("非常匹配") || lowerCase.contains("完全符合") || 
            lowerCase.contains("excellent") || lowerCase.contains("非常适合")) {
            return 90.0;
        } else if (lowerCase.contains("比较匹配") || lowerCase.contains("良好") || 
                   lowerCase.contains("good") || lowerCase.contains("相对适合")) {
            return 75.0;
        } else if (lowerCase.contains("一般匹配") || lowerCase.contains("可以") || 
                   lowerCase.contains("fair") || lowerCase.contains("基本符合")) {
            return 60.0;
        } else if (lowerCase.contains("不太匹配") || lowerCase.contains("较差") || 
                   lowerCase.contains("poor") || lowerCase.contains("不适合")) {
            return 40.0;
        }
        
        logger.debug("未找到明确分数，使用默认分数 65.0");
        return 65.0; // 默认分数
    }

    // 注意：数据库初始化数据已移至database-schema.sql脚本
    // 系统启动时会自动从数据库加载已有数据
    
    /**
     * 为简历推荐合适的岗位（带过滤条件）
     * 
     * @param resumeId 简历ID
     * @param topK 返回推荐岗位数量
     * @param minScore 最低匹配分数
     * @param location 期望工作地点（可选）
     * @param company 期望公司（可选）
     * @return 岗位推荐列表
     */
    public List<JobMatchingResult> recommendJobsWithFilters(String resumeId, int topK, 
                                                           double minScore, String location, String company) {
        logger.info("开始带过滤条件的岗位推荐 - resumeId: {}, topK: {}, minScore: {}, location: {}, company: {}", 
                   resumeId, topK, minScore, location, company);
        
        List<JobMatchingResult> allRecommendations = recommendJobsForResume(resumeId, Integer.MAX_VALUE);
        
        // 应用过滤条件
        return allRecommendations.stream()
            .filter(result -> result.getMatchScore() >= minScore)
            .filter(result -> {
                if (location == null || location.trim().isEmpty()) {
                    return true;
                }
                Job job = getJobById(result.getJobId());
                return job != null && job.getLocation() != null && 
                       job.getLocation().toLowerCase().contains(location.toLowerCase());
            })
            .filter(result -> {
                if (company == null || company.trim().isEmpty()) {
                    return true;
                }
                Job job = getJobById(result.getJobId());
                return job != null && job.getCompany() != null && 
                       job.getCompany().toLowerCase().contains(company.toLowerCase());
            })
            .limit(topK)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 批量匹配简历与多个岗位
     * 
     * @param resumeId 简历ID
     * @param jobIds 岗位ID列表
     * @return 匹配结果列表
     */
    public List<JobMatchingResult> batchMatchResumeWithJobs(String resumeId, List<String> jobIds) {
        logger.info("开始批量匹配 - resumeId: {}, jobIds: {}", resumeId, jobIds);
        
        List<JobMatchingResult> results = new ArrayList<>();
        
        for (String jobId : jobIds) {
            try {
                JobMatchingResult result = matchResumeWithJob(resumeId, jobId);
                results.add(result);
            } catch (Exception e) {
                logger.warn("批量匹配中单个岗位失败 - resumeId: {}, jobId: {}, 错误: {}", 
                           resumeId, jobId, e.getMessage());
            }
        }
        
        // 按匹配分数排序
        results.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        
        logger.info("批量匹配完成 - resumeId: {}, 成功匹配数量: {}/{}", 
                   resumeId, results.size(), jobIds.size());
        
        return results;
    }

}