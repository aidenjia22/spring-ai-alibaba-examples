-- ==============================================
-- 简历智能分析系统数据库表结构
-- 基于Spring AI Alibaba RAG示例项目
-- 数据库类型: PostgreSQL with PgVector
-- ==============================================

-- 启用必要的扩展
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==============================================
-- 1. 简历基本信息表
-- ==============================================
CREATE TABLE IF NOT EXISTS resumes (
    resume_id VARCHAR(50) PRIMARY KEY,
    candidate_name VARCHAR(100),
    original_file_name VARCHAR(255),
    file_type VARCHAR(10),
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    upload_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 简历状态枚举约束
ALTER TABLE resumes ADD CONSTRAINT chk_resume_status 
    CHECK (status IN ('UPLOADED', 'PROCESSING', 'ANALYZED', 'ERROR'));

-- 文件类型约束
ALTER TABLE resumes ADD CONSTRAINT chk_file_type 
    CHECK (file_type IN ('pdf', 'doc', 'docx', 'txt'));

-- ==============================================
-- 2. 个人信息表
-- ==============================================
CREATE TABLE IF NOT EXISTS personal_info (
    resume_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    age INTEGER,
    gender VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE
);

-- 年龄约束
ALTER TABLE personal_info ADD CONSTRAINT chk_age 
    CHECK (age > 0 AND age < 150);

-- 性别约束
ALTER TABLE personal_info ADD CONSTRAINT chk_gender 
    CHECK (gender IN ('男', '女', 'MALE', 'FEMALE', 'OTHER'));

-- ==============================================
-- 3. 工作经历表
-- ==============================================
CREATE TABLE IF NOT EXISTS work_experiences (
    id SERIAL PRIMARY KEY,
    resume_id VARCHAR(50) NOT NULL,
    company VARCHAR(200),
    position VARCHAR(100),
    start_date VARCHAR(20),
    end_date VARCHAR(20),
    description TEXT,
    achievements TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE
);

-- ==============================================
-- 4. 教育背景表
-- ==============================================
CREATE TABLE IF NOT EXISTS education_info (
    id SERIAL PRIMARY KEY,
    resume_id VARCHAR(50) NOT NULL,
    school VARCHAR(200),
    major VARCHAR(100),
    degree VARCHAR(50),
    start_date VARCHAR(20),
    end_date VARCHAR(20),
    gpa VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE
);

-- ==============================================
-- 5. 技能表
-- ==============================================
CREATE TABLE IF NOT EXISTS resume_skills (
    id SERIAL PRIMARY KEY,
    resume_id VARCHAR(50) NOT NULL,
    skill_name VARCHAR(100) NOT NULL,
    skill_level VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE
);

-- 技能水平约束
ALTER TABLE resume_skills ADD CONSTRAINT chk_skill_level 
    CHECK (skill_level IN ('初级', '中级', '高级', 'BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'));

-- ==============================================
-- 6. 简历分析结果表
-- ==============================================
CREATE TABLE IF NOT EXISTS resume_analysis (
    resume_id VARCHAR(50) PRIMARY KEY,
    summary TEXT,
    strengths TEXT[], -- 数组类型存储优势列表
    improvements TEXT[], -- 数组类型存储改进建议列表
    experience_score INTEGER,
    skill_score INTEGER,
    education_score INTEGER,
    overall_score INTEGER,
    recommendation VARCHAR(50),
    analysis_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE
);

-- 分数约束
ALTER TABLE resume_analysis ADD CONSTRAINT chk_experience_score 
    CHECK (experience_score >= 0 AND experience_score <= 100);
ALTER TABLE resume_analysis ADD CONSTRAINT chk_skill_score 
    CHECK (skill_score >= 0 AND skill_score <= 100);
ALTER TABLE resume_analysis ADD CONSTRAINT chk_education_score 
    CHECK (education_score >= 0 AND education_score <= 100);
ALTER TABLE resume_analysis ADD CONSTRAINT chk_overall_score 
    CHECK (overall_score >= 0 AND overall_score <= 100);

-- ==============================================
-- 7. 岗位信息表
-- ==============================================
CREATE TABLE IF NOT EXISTS jobs (
    job_id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    company VARCHAR(200) NOT NULL,
    department VARCHAR(100),
    location VARCHAR(100),
    employment_type VARCHAR(20),
    experience VARCHAR(100),
    education VARCHAR(100),
    salary_range VARCHAR(100),
    description TEXT,
    responsibilities TEXT[], -- 数组类型存储职责列表
    requirements TEXT[], -- 数组类型存储要求列表
    skills TEXT[], -- 数组类型存储技能要求列表
    benefits TEXT[], -- 数组类型存储福利列表
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    create_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- 雇佣类型约束
ALTER TABLE jobs ADD CONSTRAINT chk_employment_type 
    CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN'));

-- 岗位状态约束
ALTER TABLE jobs ADD CONSTRAINT chk_job_status 
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'CLOSED'));

-- ==============================================
-- 8. 简历岗位匹配结果表
-- ==============================================
CREATE TABLE IF NOT EXISTS job_matching_results (
    id SERIAL PRIMARY KEY,
    resume_id VARCHAR(50) NOT NULL,
    job_id VARCHAR(50) NOT NULL,
    match_score DECIMAL(5,2) NOT NULL,
    match_level VARCHAR(20) NOT NULL,
    summary TEXT,
    detail_matches JSONB, -- JSON格式存储详细匹配分析
    advantages TEXT[], -- 数组类型存储候选人优势
    gaps TEXT[], -- 数组类型存储能力差距
    recommendations TEXT[], -- 数组类型存储建议
    analysis_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (resume_id) REFERENCES resumes(resume_id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(job_id) ON DELETE CASCADE,
    UNIQUE(resume_id, job_id) -- 防重复匹配
);

-- 匹配分数约束
ALTER TABLE job_matching_results ADD CONSTRAINT chk_match_score 
    CHECK (match_score >= 0 AND match_score <= 100);

-- 匹配等级约束
ALTER TABLE job_matching_results ADD CONSTRAINT chk_match_level 
    CHECK (match_level IN ('EXCELLENT', 'GOOD', 'FAIR', 'POOR'));

-- ==============================================
-- 9. 向量存储表（用于RAG功能）
-- ==============================================
CREATE TABLE IF NOT EXISTS document_embeddings (
    id SERIAL PRIMARY KEY,
    document_id VARCHAR(100) NOT NULL,
    document_type VARCHAR(20) NOT NULL, -- 'RESUME' 或 'JOB'
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1536), -- 假设使用1536维向量，根据实际模型调整
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 文档类型约束
ALTER TABLE document_embeddings ADD CONSTRAINT chk_document_type 
    CHECK (document_type IN ('RESUME', 'JOB', 'ANALYSIS'));

-- ==============================================
-- 索引创建
-- ==============================================

-- 简历表索引
CREATE INDEX IF NOT EXISTS idx_resumes_status ON resumes(status);
CREATE INDEX IF NOT EXISTS idx_resumes_upload_time ON resumes(upload_time);
CREATE INDEX IF NOT EXISTS idx_resumes_candidate_name ON resumes(candidate_name);

-- 个人信息表索引
CREATE INDEX IF NOT EXISTS idx_personal_info_email ON personal_info(email);
CREATE INDEX IF NOT EXISTS idx_personal_info_name ON personal_info(name);

-- 工作经历表索引
CREATE INDEX IF NOT EXISTS idx_work_exp_resume_id ON work_experiences(resume_id);
CREATE INDEX IF NOT EXISTS idx_work_exp_company ON work_experiences(company);

-- 教育背景表索引
CREATE INDEX IF NOT EXISTS idx_education_resume_id ON education_info(resume_id);
CREATE INDEX IF NOT EXISTS idx_education_school ON education_info(school);

-- 技能表索引
CREATE INDEX IF NOT EXISTS idx_skills_resume_id ON resume_skills(resume_id);
CREATE INDEX IF NOT EXISTS idx_skills_name ON resume_skills(skill_name);

-- 简历分析表索引
CREATE INDEX IF NOT EXISTS idx_analysis_overall_score ON resume_analysis(overall_score);
CREATE INDEX IF NOT EXISTS idx_analysis_time ON resume_analysis(analysis_time);

-- 岗位表索引
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);
CREATE INDEX IF NOT EXISTS idx_jobs_company ON jobs(company);
CREATE INDEX IF NOT EXISTS idx_jobs_title ON jobs(title);
CREATE INDEX IF NOT EXISTS idx_jobs_location ON jobs(location);
CREATE INDEX IF NOT EXISTS idx_jobs_create_time ON jobs(create_time);

-- 匹配结果表索引
CREATE INDEX IF NOT EXISTS idx_matching_resume_id ON job_matching_results(resume_id);
CREATE INDEX IF NOT EXISTS idx_matching_job_id ON job_matching_results(job_id);
CREATE INDEX IF NOT EXISTS idx_matching_score ON job_matching_results(match_score);
CREATE INDEX IF NOT EXISTS idx_matching_level ON job_matching_results(match_level);
CREATE INDEX IF NOT EXISTS idx_matching_time ON job_matching_results(analysis_time);

-- 向量表索引
CREATE INDEX IF NOT EXISTS idx_embeddings_document_id ON document_embeddings(document_id);
CREATE INDEX IF NOT EXISTS idx_embeddings_document_type ON document_embeddings(document_type);

-- 向量相似度搜索索引（HNSW算法）
CREATE INDEX IF NOT EXISTS idx_embeddings_vector_hnsw ON document_embeddings 
USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);

-- ==============================================
-- 更新时间触发器
-- ==============================================

-- 创建更新时间函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为相关表添加更新时间触发器
CREATE TRIGGER update_resumes_updated_at BEFORE UPDATE ON resumes 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_personal_info_updated_at BEFORE UPDATE ON personal_info 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_work_experiences_updated_at BEFORE UPDATE ON work_experiences 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_education_info_updated_at BEFORE UPDATE ON education_info 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_resume_analysis_updated_at BEFORE UPDATE ON resume_analysis 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_jobs_updated_at BEFORE UPDATE ON jobs 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_job_matching_results_updated_at BEFORE UPDATE ON job_matching_results 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_document_embeddings_updated_at BEFORE UPDATE ON document_embeddings 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ==============================================
-- 初始化示例数据
-- ==============================================

-- 插入示例岗位数据
INSERT INTO jobs (job_id, title, company, department, location, employment_type, 
                  experience, education, salary_range, description, 
                  responsibilities, requirements, skills, benefits, created_by) 
VALUES 
('job_001', 'Java后端开发工程师', '阿里巴巴', '技术部', '杭州', 'FULL_TIME',
 '3-5年', '本科及以上', '25K-35K·13薪', 
 '负责电商平台后端服务开发，参与系统架构设计和性能优化',
 ARRAY['负责核心业务模块的开发和维护', '参与系统架构设计', '编写高质量代码', '性能调优和问题排查'],
 ARRAY['计算机相关专业本科及以上学历', '3年以上Java开发经验', '熟悉Spring Boot框架', '有分布式系统经验'],
 ARRAY['Java', 'Spring Boot', 'MySQL', 'Redis', 'Kafka', 'Docker'],
 ARRAY['五险一金', '年终奖', '股票期权', '带薪年假', '免费三餐'],
 'system'),

('job_002', '前端开发工程师', '腾讯', '用户体验部', '深圳', 'FULL_TIME',
 '2-4年', '本科及以上', '20K-30K·14薪',
 '负责Web前端开发，提升用户体验，参与产品设计和交互优化',
 ARRAY['开发高质量的前端应用', '与设计师协作实现UI设计', '优化前端性能', '参与代码评审'],
 ARRAY['计算机相关专业', '2年以上前端开发经验', '精通JavaScript', '熟悉React或Vue'],
 ARRAY['JavaScript', 'React', 'Vue.js', 'TypeScript', 'CSS3', 'Webpack'],
 ARRAY['五险一金', '弹性工作', '年度体检', '培训津贴'],
 'system'),

('job_003', '产品经理', '字节跳动', '产品部', '北京', 'FULL_TIME',
 '3-5年', '本科及以上', '30K-45K·15薪',
 '负责产品规划和需求分析，推动产品迭代和用户增长',
 ARRAY['产品需求分析和规划', '跨部门协调沟通', '用户体验优化', '数据分析和决策'],
 ARRAY['产品管理相关经验', '优秀的沟通协调能力', '数据分析能力', '互联网产品sense'],
 ARRAY['产品设计', '数据分析', '用户研究', 'Axure', 'SQL', '项目管理'],
 ARRAY['期权激励', '免费健身房', '下午茶', '团建活动'],
 'system');

-- 插入示例简历数据
INSERT INTO resumes (resume_id, candidate_name, original_file_name, file_type, status) 
VALUES 
('resume_001', '张三', '张三_Java开发工程师.pdf', 'pdf', 'ANALYZED'),
('resume_002', '李四', '李四_前端开发.docx', 'docx', 'ANALYZED'),
('resume_003', '王五', '王五简历.pdf', 'pdf', 'UPLOADED');

-- 插入示例个人信息
INSERT INTO personal_info (resume_id, name, email, phone, address, age, gender) 
VALUES 
('resume_001', '张三', 'zhangsan@email.com', '13800138001', '浙江省杭州市', 28, '男'),
('resume_002', '李四', 'lisi@email.com', '13800138002', '广东省深圳市', 26, '女'),
('resume_003', '王五', 'wangwu@email.com', '13800138003', '北京市朝阳区', 30, '男');

-- ==============================================
-- 注释说明
-- ==============================================

-- 表设计说明：
-- 1. 所有主要表都包含created_at和updated_at字段，便于审计
-- 2. 使用JSONB类型存储灵活的元数据和复杂结构
-- 3. 使用数组类型存储列表数据，减少表关联
-- 4. 添加必要的约束确保数据完整性
-- 5. 为常用查询字段创建索引提升性能
-- 6. 使用向量索引支持语义搜索功能
-- 7. 触发器自动维护更新时间

-- 向量维度说明：
-- 1. embedding字段假设使用1536维向量（OpenAI text-embedding-ada-002标准）
-- 2. 如使用其他模型需要调整向量维度
-- 3. HNSW索引参数可根据实际数据量调整

-- 扩展性考虑：
-- 1. 预留了metadata字段支持未来扩展
-- 2. 使用JSONB支持灵活的结构化数据
-- 3. 分离的表结构便于单独扩展各模块

COMMENT ON TABLE resumes IS '简历基本信息表';
COMMENT ON TABLE personal_info IS '个人信息表';
COMMENT ON TABLE work_experiences IS '工作经历表';
COMMENT ON TABLE education_info IS '教育背景表';
COMMENT ON TABLE resume_skills IS '简历技能表';
COMMENT ON TABLE resume_analysis IS '简历分析结果表';
COMMENT ON TABLE jobs IS '岗位信息表';
COMMENT ON TABLE job_matching_results IS '简历岗位匹配结果表';
COMMENT ON TABLE document_embeddings IS '文档向量嵌入表';