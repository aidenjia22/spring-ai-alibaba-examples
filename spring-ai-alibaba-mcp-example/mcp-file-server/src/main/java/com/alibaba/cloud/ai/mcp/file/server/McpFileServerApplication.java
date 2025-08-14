package com.alibaba.cloud.ai.mcp.file.server;


import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpFileServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpFileServerApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider fileTools(FileService fileService) {
		return MethodToolCallbackProvider.builder().toolObjects(fileService).build();
	}
}