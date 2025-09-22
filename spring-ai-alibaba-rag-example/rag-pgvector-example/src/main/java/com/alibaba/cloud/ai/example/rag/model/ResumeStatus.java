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

package com.alibaba.cloud.ai.example.rag.model;

/**
 * 简历处理状态枚举
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
public enum ResumeStatus {
    UPLOADED("已上传"),
    PROCESSING("处理中"),
    ANALYZED("已分析"),
    ERROR("错误");

    private final String description;

    ResumeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}