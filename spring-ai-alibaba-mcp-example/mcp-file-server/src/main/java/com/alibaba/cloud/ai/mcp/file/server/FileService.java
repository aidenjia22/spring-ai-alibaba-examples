/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 * @author brianxiadong
 */

package com.alibaba.cloud.ai.mcp.file.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * 提供本地文件读取服务
 * 该服务可以读取指定路径的文件内容并返回
 */
@Service
public class FileService {

    /**
     * 读取指定路径的文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容字符串
     * @throws IOException 如果文件不存在或无法读取
     */
    @Tool(name = "读取指定路径的本地文件内容")
    public String readFile(String filePath) throws IOException {
        filePath = "D:\\workspace\\spring-ai-alibaba-examples\\spring-ai-alibaba-mcp-example\\mcp-file-server\\data"
                 + "\\" + filePath;
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return "文件不存在";
        }
        if (!Files.isReadable(path)) {
            throw new IOException("文件不可读: " + filePath);
        }
        return Files.readString(path);
    }

}
