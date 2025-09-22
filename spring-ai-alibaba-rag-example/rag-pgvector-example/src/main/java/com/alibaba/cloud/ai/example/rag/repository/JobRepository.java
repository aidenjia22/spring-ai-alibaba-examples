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

import com.alibaba.cloud.ai.example.rag.model.Job;

import java.util.List;
import java.util.Optional;

/**
 * 岗位数据访问接口
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
public interface JobRepository {

    /**
     * 保存岗位
     */
    Job save(Job job);

    /**
     * 根据ID查找岗位
     */
    Optional<Job> findById(String jobId);

    /**
     * 查找所有岗位
     */
    List<Job> findAll();

    /**
     * 根据状态查找岗位
     */
    List<Job> findByStatus(String status);

    /**
     * 根据公司查找岗位
     */
    List<Job> findByCompany(String company);

    /**
     * 根据职位标题查找岗位
     */
    List<Job> findByTitleContaining(String title);

    /**
     * 根据地点查找岗位
     */
    List<Job> findByLocation(String location);

    /**
     * 检查岗位是否存在
     */
    boolean existsById(String jobId);

    /**
     * 删除岗位
     */
    void deleteById(String jobId);

    /**
     * 查找活跃的岗位
     */
    List<Job> findActiveJobs();
}