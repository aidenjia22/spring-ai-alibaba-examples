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

import java.util.List;

/**
 * 简历分析结果类
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
public class ResumeAnalysis {
    private String resumeId;
    private PersonalInfo personalInfo;
    private List<WorkExperience> workExperiences;
    private List<String> skills;
    private EducationInfo education;
    private AssessmentScore assessmentScore;
    private String summary;              // 简历总结
    private List<String> strengths;      // 优势
    private List<String> improvements;   // 改进建议

    public ResumeAnalysis() {
    }

    public ResumeAnalysis(String resumeId) {
        this.resumeId = resumeId;
    }

    // Getters and Setters
    public String getResumeId() {
        return resumeId;
    }

    public void setResumeId(String resumeId) {
        this.resumeId = resumeId;
    }

    public PersonalInfo getPersonalInfo() {
        return personalInfo;
    }

    public void setPersonalInfo(PersonalInfo personalInfo) {
        this.personalInfo = personalInfo;
    }

    public List<WorkExperience> getWorkExperiences() {
        return workExperiences;
    }

    public void setWorkExperiences(List<WorkExperience> workExperiences) {
        this.workExperiences = workExperiences;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public EducationInfo getEducation() {
        return education;
    }

    public void setEducation(EducationInfo education) {
        this.education = education;
    }

    public AssessmentScore getAssessmentScore() {
        return assessmentScore;
    }

    public void setAssessmentScore(AssessmentScore assessmentScore) {
        this.assessmentScore = assessmentScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getImprovements() {
        return improvements;
    }

    public void setImprovements(List<String> improvements) {
        this.improvements = improvements;
    }
}