package com.alibaba.cloud.ai.toolcall.controller;

import com.alibaba.cloud.ai.toolcall.component.SearchTools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/web-search")
public class WebSerachController {

    private final ChatClient dashScopeChatClient;
    private final SearchTools searchTools;

    public WebSerachController(ChatClient chatClient, SearchTools searchTools) {
        this.dashScopeChatClient = chatClient;
        this.searchTools = searchTools;
    }

    @GetMapping("/search-no-tool")
    public String searchNoTool(@RequestParam String query) {
        return dashScopeChatClient.prompt(query)
                .call()
                .content();
    }


     @GetMapping("/search-using-tool")
    public String search(@RequestParam String query) {
        return dashScopeChatClient.prompt(query)
                .tools(searchTools)
                .call()
                .content();
    }


}
