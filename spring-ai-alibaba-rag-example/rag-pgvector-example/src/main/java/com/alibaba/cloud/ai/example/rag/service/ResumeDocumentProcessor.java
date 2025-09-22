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

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历文档处理器
 * 支持多种文档格式的解析和处理
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@Component
public class ResumeDocumentProcessor {

    private final TokenTextSplitter textSplitter;

    public ResumeDocumentProcessor() {
        this.textSplitter = new TokenTextSplitter();
    }

    /**
     * 处理简历文档
     * 
     * @param file 上传的文件
     * @param resumeId 简历ID
     * @param candidateName 候选人姓名
     * @return 处理后的文档列表
     */
    public List<Document> processResume(MultipartFile file, String resumeId, String candidateName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        String fileExtension = getFileExtension(file.getOriginalFilename());
        DocumentReader reader = createDocumentReader(file, fileExtension);
        
        List<Document> documents = reader.get();
        
        // 为简历文档添加特定的元数据
        documents.forEach(doc -> {
            doc.getMetadata().put("resumeId", resumeId);
            doc.getMetadata().put("documentType", "resume");
            doc.getMetadata().put("candidateName", candidateName != null ? candidateName : "Unknown");
            doc.getMetadata().put("fileName", file.getOriginalFilename());
            doc.getMetadata().put("fileType", fileExtension);
            doc.getMetadata().put("uploadTime", LocalDateTime.now().toString());
        });
        
        // 分割文档
        return textSplitter.apply(documents);
    }

    /**
     * 处理文本内容
     * 
     * @param text 文本内容
     * @param resumeId 简历ID
     * @param candidateName 候选人姓名
     * @return 处理后的文档列表
     */
    public List<Document> processText(String text, String resumeId, String candidateName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文本内容不能为空");
        }

        Document document = new Document(text);
        
        // 添加元数据
        document.getMetadata().put("resumeId", resumeId);
        document.getMetadata().put("documentType", "resume");
        document.getMetadata().put("candidateName", candidateName != null ? candidateName : "Unknown");
        document.getMetadata().put("fileType", "text");
        document.getMetadata().put("uploadTime", LocalDateTime.now().toString());
        
        // 分割文档
        return textSplitter.apply(List.of(document));
    }

    /**
     * 创建文档读取器
     */
    private DocumentReader createDocumentReader(MultipartFile file, String fileExtension) {
        switch (fileExtension.toLowerCase()) {
            case "pdf":
                return new PagePdfDocumentReader(file.getResource());
            case "doc":
            case "docx":
            case "txt":
            case "rtf":
                return new TikaDocumentReader(file.getResource());
            default:
                // 尝试使用 Tika 处理未知格式
                return new TikaDocumentReader(file.getResource());
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

    /**
     * 检查是否支持的文件格式
     */
    public boolean isSupportedFileType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return extension.matches("pdf|doc|docx|txt|rtf");
    }
}