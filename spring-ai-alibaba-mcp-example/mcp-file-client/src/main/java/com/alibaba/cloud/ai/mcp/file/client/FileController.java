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

package com.alibaba.cloud.ai.mcp.file.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 文件服务控制器
 * 提供文件读取和检查的Web接口
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final  ChatClient chatClient;

    private final ToolCallbackProvider tools;

    public FileController(ChatClient chatClient, ToolCallbackProvider tools) {
        this.chatClient = chatClient;
        this.tools = tools;
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    @GetMapping("/read")
    public Flux<String> readFile(@RequestParam("filePath") String filePath) {
         return chatClient.prompt(filePath).stream().content();
    }
}
