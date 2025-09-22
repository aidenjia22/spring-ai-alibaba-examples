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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 简历岗位匹配结果模型
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
public class JobMatchingResult {
    private String resumeId;
    private String jobId;
    private double matchScore; // 匹配分数 0-100
    private String matchLevel; // 匹配等级：EXCELLENT, GOOD, FAIR, POOR
    private String summary; // 匹配总结
    private Map<String, MatchDetail> detailMatches; // 详细匹配分析
    private List<String> advantages; // 候选人优势
    private List<String> gaps; // 能力差距
    private List<String> recommendations; // 建议
    private LocalDateTime analysisTime;

    public JobMatchingResult() {
        this.analysisTime = LocalDateTime.now();
    }

    public JobMatchingResult(String resumeId, String jobId) {
        this();
        this.resumeId = resumeId;
        this.jobId = jobId;
    }

    /**
     * 匹配详情
     */
    public static class MatchDetail {
        private double score; // 该项匹配分数
        private String level; // 匹配等级
        private String analysis; // 分析说明
        private List<String> matchedItems; // 匹配项
        private List<String> missingItems; // 缺失项

        public MatchDetail() {}

        public MatchDetail(double score, String level, String analysis) {
            this.score = score;
            this.level = level;
            this.analysis = analysis;
        }

        // Getters and Setters
        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getAnalysis() {
            return analysis;
        }

        public void setAnalysis(String analysis) {
            this.analysis = analysis;
        }

        public List<String> getMatchedItems() {
            return matchedItems;
        }

        public void setMatchedItems(List<String> matchedItems) {
            this.matchedItems = matchedItems;
        }

        public List<String> getMissingItems() {
            return missingItems;
        }

        public void setMissingItems(List<String> missingItems) {
            this.missingItems = missingItems;
        }
    }

    // Getters and Setters
    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(double matchScore) {
        this.matchScore = matchScore;
        // 根据分数自动设置匹配等级
        if (matchScore >= 85) {
            this.matchLevel = "EXCELLENT";
        } else if (matchScore >= 70) {
            this.matchLevel = "GOOD";
        } else if (matchScore >= 50) {
            this.matchLevel = "FAIR";
        } else {
            this.matchLevel = "POOR";
        }
    }

    public String getMatchLevel() {
        return matchLevel;
    }

    public void setMatchLevel(String matchLevel) {
        this.matchLevel = matchLevel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Map<String, MatchDetail> getDetailMatches() {
        return detailMatches;
    }

    public void setDetailMatches(Map<String, MatchDetail> detailMatches) {
        this.detailMatches = detailMatches;
    }

    public List<String> getAdvantages() {
        return advantages;
    }

    public void setAdvantages(List<String> advantages) {
        this.advantages = advantages;
    }

    public List<String> getGaps() {
        return gaps;
    }

    public void setGaps(List<String> gaps) {
        this.gaps = gaps;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public LocalDateTime getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(LocalDateTime analysisTime) {
        this.analysisTime = analysisTime;
    }
}