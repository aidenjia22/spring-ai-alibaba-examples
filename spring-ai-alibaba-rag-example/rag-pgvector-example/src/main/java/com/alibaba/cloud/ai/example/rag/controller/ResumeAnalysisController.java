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

package com.alibaba.cloud.ai.example.rag.controller;

import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.example.rag.model.*;
import com.alibaba.cloud.ai.example.rag.service.ResumeAnalysisService;
import com.alibaba.cloud.ai.example.rag.service.ResumeDeleteService;
import com.alibaba.cloud.ai.example.rag.service.ResumeDocumentProcessor;
import com.alibaba.cloud.ai.example.rag.service.JobMatchingService;
import com.alibaba.cloud.ai.example.rag.repository.ResumeRepository;
import com.alibaba.cloud.ai.model.RerankModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 简历分析控制器
 * 提供基于RAG的简历分析接口
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@RestController
@RequestMapping("/ai/resume")
public class ResumeAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(ResumeAnalysisController.class);

    @Value("classpath:/prompts/resume-chat.st")
    private Resource chatPromptResource;

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final RerankModel rerankModel;
    private final ResumeDocumentProcessor documentProcessor;
    private final ResumeAnalysisService analysisService;
    private final JobMatchingService jobMatchingService;
    private final ResumeRepository resumeRepository;
    private final ResumeDeleteService resumeDeleteService;

    public ResumeAnalysisController(VectorStore vectorStore, 
                                  ChatModel chatModel, 
                                  RerankModel rerankModel,
                                  ResumeDocumentProcessor documentProcessor,
                                  ResumeAnalysisService analysisService,
                                  JobMatchingService jobMatchingService,
                                  ResumeRepository resumeRepository,
                                  ResumeDeleteService resumeDeleteService) {
        this.vectorStore = vectorStore;
        this.chatModel = chatModel;
        this.rerankModel = rerankModel;
        this.documentProcessor = documentProcessor;
        this.analysisService = analysisService;
        this.jobMatchingService = jobMatchingService;
        this.resumeRepository = resumeRepository;
        this.resumeDeleteService = resumeDeleteService;
    }

    /**
     * 上传简历文件
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeUploadResult> uploadResume(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "candidateName", required = false) String candidateName) {
        
        logger.info("开始处理简历上传请求 - 文件名: {}, 候选人: {}, 文件大小: {} bytes", 
            file != null ? file.getOriginalFilename() : "null", candidateName, 
            file != null ? file.getSize() : 0);
        
        try {
            // 1. 文件验证
            if (file == null || file.isEmpty()) {
                logger.warn("文件上传失败 - 文件为空或null");
                return ResponseEntity.badRequest()
                    .body(new ResumeUploadResult("文件不能为空", false));
            }

            if (!documentProcessor.isSupportedFileType(file.getOriginalFilename())) {
                logger.warn("文件上传失败 - 不支持的文件格式: {}", file.getOriginalFilename());
                return ResponseEntity.badRequest()
                    .body(new ResumeUploadResult("不支持的文件格式，请上传PDF、DOC、DOCX或TXT文件", false));
            }

            // 2. 生成简历ID
            String resumeId = UUID.randomUUID().toString();
            logger.info("生成简历ID: {} - 文件: {}", resumeId, file.getOriginalFilename());
            
            // 3. 处理简历文档
            logger.info("开始处理简历文档 - resumeId: {}", resumeId);
            List<Document> documents = documentProcessor.processResume(file, resumeId, candidateName);
            logger.info("文档处理完成 - resumeId: {}, 生成文档片段数: {}", resumeId, documents.size());
            
            // 4. 存储到向量数据库
            logger.info("开始存储文档到向量数据库 - resumeId: {}", resumeId);
            vectorStore.add(documents);
            logger.info("向量数据库存储完成 - resumeId: {}", resumeId);
            
            // 5. 保存简历信息到数据库
            Resume resume = new Resume(resumeId, candidateName, file.getOriginalFilename());
            resume.setFileType(getFileExtension(file.getOriginalFilename()));
            resume.setStatus(ResumeStatus.UPLOADED);
            resumeRepository.save(resume);
            logger.info("简历信息保存到数据库完成 - resumeId: {}, 状态: {}", resumeId, ResumeStatus.UPLOADED);
            
            String message = String.format("简历上传成功，共处理 %d 个文档片段", documents.size());
            logger.info("简历上传流程完成 - resumeId: {}, 文档片段数: {}", resumeId, documents.size());
            return ResponseEntity.ok(new ResumeUploadResult(resumeId, message));
            
        } catch (Exception e) {
            logger.error("简历上传失败 - 文件: {}, 错误: {}", 
                file != null ? file.getOriginalFilename() : "null", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResumeUploadResult("上传失败: " + e.getMessage(), false));
        }
    }

    /**
     * 导入简历文本
     */
    @PostMapping("/import-text")
    public ResponseEntity<ResumeUploadResult> importResumeText(
            @RequestParam("text") String text,
            @RequestParam(value = "candidateName", required = false) String candidateName) {
        
        logger.info("开始处理文本导入请求 - 候选人: {}, 文本长度: {} 字符", candidateName, 
            text != null ? text.length() : 0);
        
        try {
            // 1. 参数验证
            if (!StringUtils.hasText(text)) {
                logger.warn("文本导入失败 - 文本内容为空");
                return ResponseEntity.badRequest()
                    .body(new ResumeUploadResult("文本内容不能为空", false));
            }
            
            // 2. 生成简历ID
            String resumeId = UUID.randomUUID().toString();
            logger.info("生成简历ID: {} - 文本导入", resumeId);
            
            // 3. 处理文本内容
            logger.info("开始处理文本内容 - resumeId: {}", resumeId);
            List<Document> documents = documentProcessor.processText(text, resumeId, candidateName);
            logger.info("文本处理完成 - resumeId: {}, 生成文档片段数: {}", resumeId, documents.size());
            
            // 4. 存储到向量数据库
            logger.info("开始存储文本到向量数据库 - resumeId: {}", resumeId);
            vectorStore.add(documents);
            logger.info("向量数据库存储完成 - resumeId: {}", resumeId);
            
            // 5. 保存简历信息到数据库
            Resume resume = new Resume(resumeId, candidateName, null);
            resume.setFileType("txt");
            resume.setStatus(ResumeStatus.UPLOADED);
            resumeRepository.save(resume);
            logger.info("简历信息保存到数据库完成 - resumeId: {}, 类型: txt, 状态: {}", resumeId, ResumeStatus.UPLOADED);
            
            String message = String.format("简历文本导入成功，共处理 %d 个文档片段", documents.size());
            logger.info("文本导入流程完成 - resumeId: {}, 文档片段数: {}", resumeId, documents.size());
            return ResponseEntity.ok(new ResumeUploadResult(resumeId, message));
            
        } catch (Exception e) {
            logger.error("文本导入失败 - 候选人: {}, 错误: {}", candidateName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResumeUploadResult("导入失败: " + e.getMessage(), false));
        }
    }

    /**
     * 分析简历
     */
    @GetMapping("/analyze/{resumeId}")
    public ResponseEntity<ResumeAnalysis> analyzeResume(@PathVariable String resumeId) {
        logger.info("收到简历分析请求 - resumeId: {}", resumeId);
        
        try {
            // 检查简历是否存在
            if (!resumeRepository.existsById(resumeId)) {
                logger.warn("简历分析失败 - 简历不存在: {}", resumeId);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("开始分析简历 - resumeId: {}", resumeId);
            // 进行分析
            ResumeAnalysis analysis = analysisService.analyzeResume(resumeId);
            logger.info("简历分析完成 - resumeId: {}, 综合评分: {}", resumeId, 
                analysis != null && analysis.getAssessmentScore() != null ? 
                    analysis.getAssessmentScore().getOverallScore() : "null");
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            logger.error("简历分析失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 简历智能问答
     */
    @GetMapping(value = "/chat/{resumeId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatAboutResume(
            @PathVariable String resumeId,
            @RequestParam String question) throws IOException {
        
        logger.info("收到简历问答请求 - resumeId: {}, 问题: {}", resumeId, question);
        
        // 检查简历是否存在
        if (!resumeRepository.existsById(resumeId)) {
            logger.warn("简历问答失败 - 简历不存在: {}", resumeId);
            return Flux.just("简历不存在: " + resumeId);
        }
        
        logger.info("开始构建向量检索请求 - resumeId: {}", resumeId);
        // 2. 构建过滤器
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        Filter.Expression filter = builder.eq("resumeId", resumeId).build();
        
        // 3. 构建搜索请求
        SearchRequest searchRequest = SearchRequest.builder()
            .topK(5)
            .similarityThreshold(0.5)
            .filterExpression(filter)
            .build();
        
        logger.info("构建ChatClient - resumeId: {}, topK: 5, similarityThreshold: 0.5", resumeId);
        
        // 4. 获取聊天提示词模板
        String chatPrompt = chatPromptResource.getContentAsString(StandardCharsets.UTF_8);
        
        // 5. 流式回答
        logger.info("开始流式问答 - resumeId: {}, 问题长度: {} 字符", resumeId, question.length());
        return ChatClient.builder(chatModel)
            .defaultAdvisors(new RetrievalRerankAdvisor(vectorStore, rerankModel, searchRequest, 
                new SystemPromptTemplate(chatPrompt), 0.1))
            .build()
            .prompt()
            .user(question)
            .stream()
            .content();
    }

    /**
     * 获取简历摘要
     */
    @GetMapping("/summary/{resumeId}")
    public ResponseEntity<String> getResumeSummary(@PathVariable String resumeId) {
        logger.info("收到获取简历摘要请求 - resumeId: {}", resumeId);
        
        try {
            // 检查简历是否存在
            if (!resumeRepository.existsById(resumeId)) {
                logger.warn("获取摘要失败 - 简历不存在: {}", resumeId);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("开始生成简历摘要 - resumeId: {}", resumeId);
            String summary = analysisService.getResumeSummary(resumeId);
            logger.info("简历摘要生成完成 - resumeId: {}, 摘要长度: {} 字符", 
                resumeId, summary != null ? summary.length() : 0);
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            logger.error("生成简历摘要失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("生成摘要失败: " + e.getMessage());
        }
    }

    /**
     * 获取已存在的分析结果
     */
    @GetMapping("/analysis/{resumeId}")
    public ResponseEntity<ResumeAnalysis> getExistingAnalysis(@PathVariable String resumeId) {
        logger.info("收到获取已存在分析结果请求 - resumeId: {}", resumeId);
        
        try {
            // 检查简历是否存在
            if (!resumeRepository.existsById(resumeId)) {
                logger.warn("获取分析结果失败 - 简历不存在: {}", resumeId);
                return ResponseEntity.notFound().build();
            }
            
            ResumeAnalysis analysis = analysisService.getExistingAnalysis(resumeId);
            if (analysis == null) {
                logger.info("未找到分析结果 - resumeId: {}", resumeId);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("返回已存在的分析结果 - resumeId: {}, 综合评分: {}", 
                resumeId, analysis.getAssessmentScore() != null ? 
                    analysis.getAssessmentScore().getOverallScore() : "null");
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            logger.error("获取分析结果失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取简历列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<Resume>> getResumeList() {
        logger.info("收到获取简历列表请求");
        
        try {
            List<Resume> resumes = resumeRepository.findAll();
            logger.info("获取简历列表成功 - 简历数量: {}", resumes.size());
            
            // 记录每个简历的基本信息（用于调试）
            if (logger.isDebugEnabled()) {
                resumes.forEach(resume -> {
                    logger.debug("简历信息 - ID: {}, 候选人: {}, 状态: {}, 上传时间: {}", 
                        resume.getResumeId(), resume.getCandidateName(), 
                        resume.getStatus(), resume.getUploadTime());
                });
            }
            
            return ResponseEntity.ok(resumes);
        } catch (Exception e) {
            logger.error("获取简历列表失败 - 错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ArrayList<>());
        }
    }

    /**
     * 删除简历
     * 完整删除简历及其相关的所有数据
     */
    @DeleteMapping("/{resumeId}")
    public ResponseEntity<String> deleteResume(@PathVariable String resumeId) {
        logger.info("收到简历删除请求 - resumeId: {}", resumeId);
        
        try {
            // 检查简历是否存在
            if (!resumeDeleteService.resumeExists(resumeId)) {
                logger.warn("简历删除失败 - 简历不存在: {}", resumeId);
                return ResponseEntity.notFound().build();
            }
            
            // 获取删除前的数据统计
            var dataSummary = resumeDeleteService.getResumeDataSummary(resumeId);
            logger.info("删除前数据统计 - resumeId: {}, 统计信息: {}", resumeId, dataSummary);
            
            // 执行完整删除
            logger.info("开始执行完整删除操作 - resumeId: {}", resumeId);
            resumeDeleteService.deleteResumeCompletely(resumeId);
            logger.info("简历删除操作完成 - resumeId: {}, 删除记录数: {}", resumeId, dataSummary.getTotalRecords());
            
            String message = String.format(
                "简历删除成功，共删除 %d 条相关记录（包括分析结果和向量数据）", 
                dataSummary.getTotalRecords()
            );
            
            return ResponseEntity.ok(message);
            
        } catch (IllegalArgumentException e) {
            logger.warn("简历删除失败 - 参数错误: resumeId: {}, 错误: {}", resumeId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("简历删除失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("删除失败: " + e.getMessage());
        }
    }

    /**
     * 简历岗位匹配
     */
    @PostMapping("/match/{resumeId}/{jobId}")
    public ResponseEntity<JobMatchingResult> matchResumeWithJob(
            @PathVariable String resumeId,
            @PathVariable String jobId) {
        try {
            // 检查简历和岗位是否存在
            if (!resumeRepository.existsById(resumeId)) {
                return ResponseEntity.notFound().build();
            }
            
            if (jobMatchingService.getJobById(jobId) == null) {
                return ResponseEntity.notFound().build();
            }
            
            JobMatchingResult result = jobMatchingService.matchResumeWithJob(resumeId, jobId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 为简历推荐岗位
     */
    @GetMapping("/recommend/{resumeId}")
    public ResponseEntity<List<JobMatchingResult>> recommendJobs(
            @PathVariable String resumeId,
            @RequestParam(value = "topK", defaultValue = "5") int topK) {
        try {
            // 检查简历是否存在
            if (!resumeRepository.existsById(resumeId)) {
                return ResponseEntity.notFound().build();
            }
            
            List<JobMatchingResult> recommendations = jobMatchingService.recommendJobsForResume(resumeId, topK);
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ArrayList<>());
        }
    }

    /**
     * 获取所有岗位列表
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<Job>> getAllJobs() {
        try {
            List<Job> jobs = jobMatchingService.getAllJobs();
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ArrayList<>());
        }
    }

    /**
     * 根据ID获取岗位详情
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<Job> getJobById(@PathVariable String jobId) {
        try {
            Job job = jobMatchingService.getJobById(jobId);
            if (job == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(job);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 添加新岗位
     */
    @PostMapping("/jobs")
    public ResponseEntity<Job> addJob(@RequestBody Job job) {
        try {
            Job savedJob = jobMatchingService.addJob(job);
            return ResponseEntity.ok(savedJob);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 更新岗位
     */
    @PutMapping("/jobs/{jobId}")
    public ResponseEntity<Job> updateJob(@PathVariable String jobId, @RequestBody Job job) {
        try {
            job.setJobId(jobId);
            Job updatedJob = jobMatchingService.updateJob(job);
            return ResponseEntity.ok(updatedJob);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 删除岗位
     */
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<String> deleteJob(@PathVariable String jobId) {
        try {
            boolean deleted = jobMatchingService.deleteJob(jobId);
            if (deleted) {
                return ResponseEntity.ok("岗位删除成功");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 带过滤条件的岗位推荐
     */
    @GetMapping("/recommend/{resumeId}/filtered")
    public ResponseEntity<List<JobMatchingResult>> recommendJobsWithFilters(
            @PathVariable String resumeId,
            @RequestParam(value = "topK", defaultValue = "5") int topK,
            @RequestParam(value = "minScore", defaultValue = "50.0") double minScore,
            @RequestParam(value = "location", required = false) String location,
            @RequestParam(value = "company", required = false) String company) {
        try {
            // 检查简历是否存在
            if (!resumeRepository.existsById(resumeId)) {
                return ResponseEntity.notFound().build();
            }
            
            List<JobMatchingResult> recommendations = jobMatchingService.recommendJobsWithFilters(
                resumeId, topK, minScore, location, company);
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            logger.error("带过滤条件的岗位推荐失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ArrayList<>());
        }
    }
    
    /**
     * 批量匹配简历与多个岗位
     */
    @PostMapping("/match/{resumeId}/batch")
    public ResponseEntity<List<JobMatchingResult>> batchMatchResumeWithJobs(
            @PathVariable String resumeId,
            @RequestBody List<String> jobIds) {
        try {
            // 检查简历是否存在
            if (!resumeRepository.existsById(resumeId)) {
                return ResponseEntity.notFound().build();
            }
            
            if (jobIds == null || jobIds.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<JobMatchingResult> results = jobMatchingService.batchMatchResumeWithJobs(resumeId, jobIds);
            return ResponseEntity.ok(results);
            
        } catch (Exception e) {
            logger.error("批量匹配失败 - resumeId: {}, 错误: {}", resumeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ArrayList<>());
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex + 1);
    }
}