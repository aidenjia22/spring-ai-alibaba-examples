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
 * 简历上传结果类
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
public class ResumeUploadResult {
    private String resumeId;
    private String message;
    private boolean success;

    public ResumeUploadResult() {
    }

    public ResumeUploadResult(String resumeId, String message) {
        this.resumeId = resumeId;
        this.message = message;
        this.success = true;
    }

    public ResumeUploadResult(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    // Getters and Setters
    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}