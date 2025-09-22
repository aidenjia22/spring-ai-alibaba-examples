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
            
            SearchRequest searchRequest = SearchRequest.builder()
                .topK(15)
                .similarityThreshold(0.1)
                .filterExpression(filter)
                .build();

            // 获取匹配提示词模板
            String matchingPrompt = jobMatchingPromptResource.getContentAsString(StandardCharsets.UTF_8);
            
            // 构建岗位信息字符串
            String jobInfo = buildJobInfoString(job);
            
            logger.debug("执行RAG匹配分析 - 岗位信息: {}", jobInfo);
            
            // 检查是否已有缓存的匹配结果
            Optional<JobMatchingResult> existingResult = jobMatchingResultRepository.findByResumeIdAndJobId(resumeId, jobId);
            if (existingResult.isPresent()) {
                logger.info("使用已缓存的匹配结果 - resumeId: {}, jobId: {}, matchScore: {}", 
                           resumeId, jobId, existingResult.get().getMatchScore());
                return existingResult.get();
            }
            
            // 使用entity方法直接获取结构化结果
            JobMatchingResult result = ChatClient.builder(chatModel)
                .defaultAdvisors(new RetrievalRerankAdvisor(vectorStore, rerankModel, searchRequest, 
                    new SystemPromptTemplate(matchingPrompt), 0.1))
                .build()
                .prompt()
                .user("请分析简历与以下岗位的匹配度：\n\n" + jobInfo)
                .call()
                .entity(JobMatchingResult.class);
                
            result.setResumeId(resumeId);
            result.setJobId(jobId);
            result.setAnalysisTime(java.time.LocalDateTime.now());
            
            logger.debug("AI返回结果: {}", result);
            
            // 保存匹配结果到数据库
            try {
                JobMatchingResult savedResult = jobMatchingResultRepository.save(result);
                logger.info("匹配结果已保存到数据库 - resumeId: {}, jobId: {}, matchScore: {}", 
                           resumeId, jobId, savedResult.getMatchScore());
                result = savedResult;
            } catch (Exception e) {
                logger.warn("保存匹配结果失败 - resumeId: {}, jobId: {}, 错误: {}", 
                           resumeId, jobId, e.getMessage());
            }
            
            logger.info("简历岗位匹配完成 - resumeId: {}, jobId: {}, matchScore: {}", 
                       resumeId, jobId, result.getMatchScore());
            
            return result;
            
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
     * 解析匹配结果
     * 使用简单的文本解析
     */
    private JobMatchingResult parseMatchingResult(String resumeId, String jobId, String analysisResult) {
        logger.debug("开始解析匹配结果 - resumeId: {}, jobId: {}", resumeId, jobId);
        
        JobMatchingResult result = new JobMatchingResult(resumeId, jobId);
        
        // 简单的分数提取逻辑
        try {
            double score = extractMatchScore(analysisResult);
            result.setMatchScore(score);
        } catch (Exception e) {
            logger.warn("文本分数提取失败，使用默认分数 - resumeId: {}, jobId: {}", resumeId, jobId);
            result.setMatchScore(50.0); // 默认分数
        }
        
        result.setSummary(analysisResult);
        
        // 简单的优势和差距提取
        result.setAdvantages(extractListFromText(analysisResult, "优势", "适合", "符合"));
        result.setGaps(extractListFromText(analysisResult, "不足", "缺少", "差距"));
        result.setRecommendations(extractListFromText(analysisResult, "建议", "推荐", "提升"));
        
        logger.debug("文本解析完成 - resumeId: {}, jobId: {}, 匹配分数: {}", 
                    resumeId, jobId, result.getMatchScore());
        
        return result;
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