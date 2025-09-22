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

package com.alibaba.cloud.ai.example.rag.repository.impl;

import com.alibaba.cloud.ai.example.rag.model.EmploymentType;
import com.alibaba.cloud.ai.example.rag.model.Job;
import com.alibaba.cloud.ai.example.rag.model.JobStatus;
import com.alibaba.cloud.ai.example.rag.repository.JobRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 岗位数据访问实现类
 * 
 * @author WANG, ZHEN
 * @since 1.0.0-M3
 */
@Repository
public class JobRepositoryImpl implements JobRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Job save(Job job) {
        if (existsById(job.getJobId())) {
            return update(job);
        } else {
            return insert(job);
        }
    }

    private Job insert(Job job) {
        String sql = """
            INSERT INTO jobs (job_id, title, company, department, location, employment_type,
                            experience, education, salary_range, description, 
                            responsibilities, requirements, skills, benefits, 
                            status, create_time, update_time, created_by) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try {
            jdbcTemplate.update(sql,
                job.getJobId(),
                job.getTitle(),
                job.getCompany(),
                job.getDepartment(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getExperience(),
                job.getEducation(),
                job.getSalaryRange(),
                job.getDescription(),
                job.getResponsibilities() != null ? 
                    jdbcTemplate.getDataSource().getConnection().createArrayOf("text", job.getResponsibilities().toArray()) : null,
                job.getRequirements() != null ? 
                    jdbcTemplate.getDataSource().getConnection().createArrayOf("text", job.getRequirements().toArray()) : null,
                job.getSkills() != null ? 
                    jdbcTemplate.getDataSource().getConnection().createArrayOf("text", job.getSkills().toArray()) : null,
                job.getBenefits() != null ? 
                    jdbcTemplate.getDataSource().getConnection().createArrayOf("text", job.getBenefits().toArray()) : null,
                job.getStatus() != null ? job.getStatus() : "ACTIVE",
                Timestamp.valueOf(job.getCreateTime() != null ? job.getCreateTime() : LocalDateTime.now()),
                Timestamp.valueOf(job.getUpdateTime() != null ? job.getUpdateTime() : LocalDateTime.now()),
                job.getCreatedBy()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert job", e);
        }
        
        return job;
    }

    private Job update(Job job) {
        String sql = """
            UPDATE jobs 
            SET title = ?, company = ?, department = ?, location = ?, employment_type = ?,
                experience = ?, education = ?, salary_range = ?, description = ?,
                responsibilities = ?, requirements = ?, skills = ?, benefits = ?,
                status = ?, update_time = ?, created_by = ?
            WHERE job_id = ?
            """;

        try {
            jdbcTemplate.update(sql,
                job.getTitle(),
                job.getCompany(),
                job.getDepartment(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getExperience(),
                job.getEducation(),
                job.getSalaryRange(),
                job.getDescription(),
                job.getResponsibilities() != null ? 
                    jdbcTemplate.getDataSource().getConnection().createArrayOf("text", job.getResponsibilities().toArray()) : null,
                job.getRequirements() != null ? 
                    jdbcTemplate.getDataSource().getConnection().createArrayOf("text", job.getRequirements().toArray()) : null,
                job.getSkills() != null ? 
                    jdbcTemplate.getDataSource().getConnection().createArrayOf("text", job.getSkills().toArray()) : null,
                job.getBenefits() != null ? 
                    jdbcTemplate.getDataSource().getConnection().createArrayOf("text", job.getBenefits().toArray()) : null,
                job.getStatus(),
                Timestamp.valueOf(LocalDateTime.now()),
                job.getCreatedBy(),
                job.getJobId()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to update job", e);
        }
        
        return job;
    }

    @Override
    public Optional<Job> findById(String jobId) {
        String sql = "SELECT * FROM jobs WHERE job_id = ?";
        List<Job> jobs = jdbcTemplate.query(sql, new JobRowMapper(), jobId);
        return jobs.isEmpty() ? Optional.empty() : Optional.of(jobs.get(0));
    }

    @Override
    public List<Job> findAll() {
        String sql = "SELECT * FROM jobs ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, new JobRowMapper());
    }

    @Override
    public List<Job> findByStatus(String status) {
        String sql = "SELECT * FROM jobs WHERE status = ? ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, new JobRowMapper(), status);
    }

    @Override
    public List<Job> findByCompany(String company) {
        String sql = "SELECT * FROM jobs WHERE company ILIKE ? ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, new JobRowMapper(), "%" + company + "%");
    }

    @Override
    public List<Job> findByTitleContaining(String title) {
        String sql = "SELECT * FROM jobs WHERE title ILIKE ? ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, new JobRowMapper(), "%" + title + "%");
    }

    @Override
    public List<Job> findByLocation(String location) {
        String sql = "SELECT * FROM jobs WHERE location ILIKE ? ORDER BY create_time DESC";
        return jdbcTemplate.query(sql, new JobRowMapper(), "%" + location + "%");
    }

    @Override
    public boolean existsById(String jobId) {
        String sql = "SELECT COUNT(*) FROM jobs WHERE job_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, jobId);
        return count != null && count > 0;
    }

    @Override
    public void deleteById(String jobId) {
        String sql = "DELETE FROM jobs WHERE job_id = ?";
        jdbcTemplate.update(sql, jobId);
    }

    @Override
    public List<Job> findActiveJobs() {
        return findByStatus("ACTIVE");
    }

    /**
     * Job行映射器
     */
    private class JobRowMapper implements RowMapper<Job> {
        @Override
        public Job mapRow(ResultSet rs, int rowNum) throws SQLException {
            Job job = new Job();
            job.setJobId(rs.getString("job_id"));
            job.setTitle(rs.getString("title"));
            job.setCompany(rs.getString("company"));
            job.setDepartment(rs.getString("department"));
            job.setLocation(rs.getString("location"));
            job.setEmploymentType(EmploymentType.valueOf(rs.getString("employment_type")));
            job.setExperience(rs.getString("experience"));
            job.setEducation(rs.getString("education"));
            job.setSalaryRange(rs.getString("salary_range"));
            job.setDescription(rs.getString("description"));
            job.setStatus(JobStatus.valueOf(rs.getString("status")));
            job.setCreatedBy(rs.getString("created_by"));
            
            // 处理时间字段
            Timestamp createTime = rs.getTimestamp("create_time");
            if (createTime != null) {
                job.setCreateTime(createTime.toLocalDateTime());
            }
            
            Timestamp updateTime = rs.getTimestamp("update_time");
            if (updateTime != null) {
                job.setUpdateTime(updateTime.toLocalDateTime());
            }
            
            // 处理数组字段
            Array responsibilitiesArray = rs.getArray("responsibilities");
            if (responsibilitiesArray != null) {
                String[] responsibilities = (String[]) responsibilitiesArray.getArray();
                job.setResponsibilities(Arrays.asList(responsibilities));
            }
            
            Array requirementsArray = rs.getArray("requirements");
            if (requirementsArray != null) {
                String[] requirements = (String[]) requirementsArray.getArray();
                job.setRequirements(Arrays.asList(requirements));
            }
            
            Array skillsArray = rs.getArray("skills");
            if (skillsArray != null) {
                String[] skills = (String[]) skillsArray.getArray();
                job.setSkills(Arrays.asList(skills));
            }
            
            Array benefitsArray = rs.getArray("benefits");
            if (benefitsArray != null) {
                String[] benefits = (String[]) benefitsArray.getArray();
                job.setBenefits(Arrays.asList(benefits));
            }
            
            return job;
        }
    }
}