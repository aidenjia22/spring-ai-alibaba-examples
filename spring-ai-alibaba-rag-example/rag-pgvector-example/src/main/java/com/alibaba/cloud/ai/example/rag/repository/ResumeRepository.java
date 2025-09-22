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

import com.alibaba.cloud.ai.example.rag.model.Resume;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 简历数据访问接口
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
public interface ResumeRepository {

    /**
     * 保存简历
     */
    Resume save(Resume resume);

    /**
     * 根据ID查找简历
     */
    Optional<Resume> findById(String resumeId);

    /**
     * 查找所有简历
     */
    List<Resume> findAll();

    /**
     * 根据候选人姓名查找简历
     */
    List<Resume> findByCandidateNameContaining(String candidateName);

    /**
     * 根据状态查找简历
     */
    List<Resume> findByStatus(String status);

    /**
     * 检查简历是否存在
     */
    boolean existsById(String resumeId);

    /**
     * 删除简历
     */
    void deleteById(String resumeId);

    /**
     * 查找最近上传的简历
     */
    List<Resume> findRecentResumes(int limit);
}