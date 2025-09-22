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
 * 工作类型枚举
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
public enum EmploymentType {
    /**
     * 全职
     */
    FULL_TIME("全职"),
    
    /**
     * 兼职
     */
    PART_TIME("兼职"),
    
    /**
     * 合同工
     */
    CONTRACT("合同工"),
    
    /**
     * 实习
     */
    INTERN("实习");

    private final String description;

    EmploymentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}