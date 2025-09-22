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

package com.alibaba.cloud.ai.example.rag.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面控制器
 * 负责页面路由和视图渲染
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@Controller
public class PageController {

    /**
     * 首页 - 简历分析系统主页
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }

    /**
     * 简历分析页面
     */
    @GetMapping("/resume")
    public String resumeAnalysis() {
        return "resume-analysis";
    }

    /**
     * 简历管理页面
     */
    @GetMapping("/management")
    public String resumeManagement() {
        return "resume-management";
    }

    /**
     * 岗位管理页面
     */
    @GetMapping("/jobs")
    public String jobManagement() {
        return "job-management";
    }

    /**
     * 分析结果详情页面
     */
    @GetMapping("/analysis-detail")
    public String analysisDetail() {
        return "analysis-detail";
    }
}