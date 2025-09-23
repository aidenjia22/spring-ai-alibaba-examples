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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 简历岗位匹配结果模型
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobMatchingResult {
    private String resumeId;
    private String jobId;
    @JsonProperty("matchScore")
    @JsonPropertyDescription("匹配分数，范围60-100分，数值越高表示匹配度越好")
    private double matchScore; // 匹配分数 0-100
    @JsonProperty("matchLevel")
    @JsonPropertyDescription("匹配等级，可选值：优秀/良好/一般/较差")
    private String matchLevel; // 匹配等级：EXCELLENT, GOOD, FAIR, POOR
    @JsonProperty("summary")
    @JsonPropertyDescription("详细的匹配分析总结，包括技能匹配、经验匹配、教育背景等方面的分析")
    private String summary; // 匹配总结
    @JsonProperty("detailMatches")
    @JsonPropertyDescription("各维度的详细匹配分析，包括技能、经验、教育等方面")
    private MatchDetail detailMatches; // 详细匹配分析，支持灵活的数据结构
    @JsonProperty("advantages")
    @JsonPropertyDescription("候选人的主要优势和竞争力，字符串数组")
    private List<String> advantages; // 候选人优势
    @JsonProperty("gaps")
    @JsonPropertyDescription("候选人的能力差距和不足之处，字符串数组")
    private List<String> gaps; // 能力差距
    @JsonProperty("recommendations")
    @JsonPropertyDescription("针对候选人的发展建议和改进方向，字符串数组")
    private List<String> recommendations;
    @JsonIgnore
    private LocalDateTime analysisTime;

    /**
     * 匹配详情
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchDetail {
        @JsonProperty("skills")
        @JsonPropertyDescription("匹配的技能")
        private String skills;
        @JsonProperty("experience")
        @JsonPropertyDescription("该项匹配经验")
        private String experience;
        @JsonProperty("education")
        @JsonPropertyDescription("学历")
        private String education;
        @JsonProperty("workHistory")
        @JsonPropertyDescription("工作经历")
        private String workHistory;

        public String getSkills() {
            return skills;
        }

        public void setSkills(String skills) {
            this.skills = skills;
        }

        public String getExperience() {
            return experience;
        }

        public void setExperience(String experience) {
            this.experience = experience;
        }

        public String getEducation() {
            return education;
        }

        public void setEducation(String education) {
            this.education = education;
        }

        public String getWorkHistory() {
            return workHistory;
        }

        public void setWorkHistory(String workHistory) {
            this.workHistory = workHistory;
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

    public MatchDetail getDetailMatches() {
        return detailMatches;
    }

    public void setDetailMatches(MatchDetail detailMatches) {
        this.detailMatches = detailMatches;
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
    
    @Override
    public String toString() {
        return "JobMatchingResult{" +
                "resumeId='" + resumeId + '\'' +
                ", jobId='" + jobId + '\'' +
                ", matchScore=" + matchScore +
                ", matchLevel='" + matchLevel + '\'' +
                ", summary='" + (summary != null ? summary.substring(0, Math.min(50, summary.length())) + "..." : "null") + '\'' +
                ", advantagesCount=" + (advantages != null ? advantages.size() : 0) +
                ", gapsCount=" + (gaps != null ? gaps.size() : 0) +
                ", recommendationsCount=" + (recommendations != null ? recommendations.size() : 0) +
                ", analysisTime=" + analysisTime +
                '}';
    }
}