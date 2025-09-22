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

package com.alibaba.cloud.ai.example.rag.repository;

import com.alibaba.cloud.ai.example.rag.model.ResumeAnalysis;

import java.util.List;
import java.util.Optional;

/**
 * 简历分析结果数据访问接口
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
public interface ResumeAnalysisRepository {

    /**
     * 保存简历分析结果
     */
    ResumeAnalysis save(ResumeAnalysis analysis);

    /**
     * 根据简历ID查找分析结果
     */
    Optional<ResumeAnalysis> findByResumeId(String resumeId);

    /**
     * 查找所有分析结果
     */
    List<ResumeAnalysis> findAll();

    /**
     * 根据综合评分范围查找分析结果
     */
    List<ResumeAnalysis> findByOverallScoreRange(int minScore, int maxScore);

    /**
     * 检查分析结果是否存在
     */
    boolean existsByResumeId(String resumeId);

    /**
     * 删除分析结果
     */
    void deleteByResumeId(String resumeId);

    /**
     * 查找最近的分析结果
     */
    List<ResumeAnalysis> findRecentAnalysis(int limit);
}