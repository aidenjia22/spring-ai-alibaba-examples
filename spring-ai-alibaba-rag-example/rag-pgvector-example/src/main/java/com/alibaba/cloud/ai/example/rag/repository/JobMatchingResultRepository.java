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

import com.alibaba.cloud.ai.example.rag.model.JobMatchingResult;

import java.util.List;
import java.util.Optional;

/**
 * 简历岗位匹配结果数据访问接口
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
public interface JobMatchingResultRepository {

    /**
     * 保存匹配结果
     */
    JobMatchingResult save(JobMatchingResult result);

    /**
     * 根据简历ID和岗位ID查找匹配结果
     */
    Optional<JobMatchingResult> findByResumeIdAndJobId(String resumeId, String jobId);

    /**
     * 根据简历ID查找所有匹配结果
     */
    List<JobMatchingResult> findByResumeId(String resumeId);

    /**
     * 根据岗位ID查找所有匹配结果
     */
    List<JobMatchingResult> findByJobId(String jobId);

    /**
     * 根据匹配等级查找结果
     */
    List<JobMatchingResult> findByMatchLevel(String matchLevel);

    /**
     * 查找匹配分数大于指定值的结果
     */
    List<JobMatchingResult> findByMatchScoreGreaterThan(double minScore);

    /**
     * 删除匹配结果
     */
    void deleteByResumeIdAndJobId(String resumeId, String jobId);

    /**
     * 删除简历的所有匹配结果
     */
    void deleteByResumeId(String resumeId);

    /**
     * 删除岗位的所有匹配结果
     */
    void deleteByJobId(String jobId);
}