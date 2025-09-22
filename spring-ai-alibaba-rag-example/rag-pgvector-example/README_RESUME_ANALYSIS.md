# 基于RAG的简历分析系统 API 文档

## 概述
基于Spring AI Alibaba和PgVector实现的智能简历分析系统，提供简历上传、分析、问答等功能。

## 前置条件
1. 启动PostgreSQL数据库（支持pgvector扩展）
2. 配置通义千问API Key
3. 确保Java 17和Maven环境

## 启动服务
```bash
cd spring-ai-alibaba-rag-example/rag-pgvector-example
mvn spring-boot:run
```

## API 接口说明

### 1. 上传简历文件
**POST** `/ai/resume/upload`

**参数：**
- `file`: 简历文件（支持PDF、DOC、DOCX、TXT格式）
- `candidateName`: 候选人姓名（可选）

**示例：**
```bash
curl -X POST "http://localhost:8080/ai/resume/upload" \
  -F "file=@test_resume.pdf" \
  -F "candidateName=张三"
```

**返回示例：**
```json
{
  "resumeId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "简历上传成功，共处理 5 个文档片段",
  "success": true
}
```

### 2. 导入简历文本
**POST** `/ai/resume/import-text`

**参数：**
- `text`: 简历文本内容
- `candidateName`: 候选人姓名（可选）

**示例：**
```bash
curl -X POST "http://localhost:8080/ai/resume/import-text" \
  -d "text=张三，男，28岁，5年Java开发经验..." \
  -d "candidateName=张三"
```

### 3. 分析简历
**GET** `/ai/resume/analyze/{resumeId}`

**示例：**
```bash
curl "http://localhost:8080/ai/resume/analyze/550e8400-e29b-41d4-a716-446655440000"
```

### 4. 简历智能问答（流式响应）
**GET** `/ai/resume/chat/{resumeId}?question={question}`

**示例：**
```bash
curl "http://localhost:8080/ai/resume/chat/550e8400-e29b-41d4-a716-446655440000?question=这个候选人的技术能力如何？"
```

### 5. 获取简历摘要
**GET** `/ai/resume/summary/{resumeId}`

**示例：**
```bash
curl "http://localhost:8080/ai/resume/summary/550e8400-e29b-41d4-a716-446655440000"
```

### 6. 获取简历列表
**GET** `/ai/resume/list`

**示例：**
```bash
curl "http://localhost:8080/ai/resume/list"
```

### 7. 删除简历
**DELETE** `/ai/resume/{resumeId}`

**示例：**
```bash
curl -X DELETE "http://localhost:8080/ai/resume/550e8400-e29b-41d4-a716-446655440000"
```

## 测试用例

### 测试简历文本示例：
```
张三
联系方式：13800138000
邮箱：zhangsan@example.com
地址：北京市朝阳区

教育背景：
2016-2020 北京大学 计算机科学与技术 本科

工作经历：
2020.7-2023.3 阿里巴巴 Java开发工程师
- 负责电商平台后端开发
- 使用Spring Boot、MySQL、Redis等技术
- 优化系统性能，提升并发处理能力50%

2023.4-至今 腾讯 高级Java开发工程师  
- 负责微服务架构设计与开发
- 熟练使用Kubernetes、Docker容器化技术
- 带领5人团队完成多个核心项目

技能：
- 编程语言：Java、Python、JavaScript
- 框架：Spring Boot、Spring Cloud、Vue.js
- 数据库：MySQL、Redis、MongoDB
- 工具：Git、Maven、Docker、Kubernetes
```

## 常见问题

1. **上传文件失败**
   - 检查文件格式是否支持
   - 确认文件大小不超过10MB

2. **分析结果为空**
   - 确认简历内容已成功上传到向量数据库
   - 检查通义千问API Key配置

3. **数据库连接失败**
   - 确认PostgreSQL服务已启动
   - 检查pgvector扩展是否已安装

## 扩展功能
- 批量简历处理
- 职位匹配分析
- 简历评分排序
- 面试问题生成