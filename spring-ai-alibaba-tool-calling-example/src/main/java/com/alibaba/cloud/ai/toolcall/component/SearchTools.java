package com.alibaba.cloud.ai.toolcall.component;

import org.springframework.ai.tool.annotation.Tool;

public class SearchTools {

    private final BaiduSearchService baiduSearchService;

    public SearchTools(BaiduSearchService baiduSearchService) {
        this.baiduSearchService = baiduSearchService;
    }

    @Tool(name = "baiduSearch", description = "使用百度搜索网页内容")
    public String search(String query) {
        return baiduSearchService.search(query);
    }
}
