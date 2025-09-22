# 问题诊断指南

## 问题描述
前端报错：`ai/resume/analyze/null` 返回 404，表明传递给分析API的resumeId是null。

## 可能的原因

### 1. 数据库中没有数据
- 检查PostgreSQL是否正在运行
- 检查数据库表是否创建成功
- 检查是否有简历数据

### 2. 后端API问题
- 简历列表API返回的数据格式不正确
- resumeId字段为空或未正确序列化

### 3. 前端数据处理问题
- JavaScript无法正确解析后端返回的JSON
- resumeId字段名不匹配

## 诊断步骤

### 步骤1：检查应用启动
```bash
cd "d:\workspace\spring-ai-alibaba-examples\spring-ai-alibaba-rag-example\rag-pgvector-example"
mvn spring-boot:run
```

### 步骤2：使用调试页面
1. 访问：http://localhost:8080/debug.html
2. 点击"获取简历列表"查看返回的数据结构
3. 如果没有数据，上传一个测试简历
4. 检查上传后的简历ID是否正确

### 步骤3：检查浏览器控制台
1. 打开浏览器开发者工具
2. 查看Network标签页的API请求和响应
3. 查看Console标签页的JavaScript错误和调试信息

### 步骤4：检查数据库
连接PostgreSQL并执行：
```sql
-- 检查表是否存在
SELECT tablename FROM pg_tables WHERE schemaname = 'public';

-- 检查简历数据
SELECT resume_id, candidate_name, status FROM resumes LIMIT 5;
```

## 修复方案

### 如果数据库没有数据
1. 执行database-schema.sql脚本
2. 上传测试简历

### 如果API返回数据有问题
1. 检查Resume类的JSON序列化
2. 检查ResumeRepositoryImpl的数据映射

### 如果前端解析有问题
1. 检查字段名是否匹配（resumeId vs resume_id）
2. 检查JavaScript的错误处理

## 当前修改的文件
- `resume-management.js` - 添加了调试信息和错误处理
- `Resume.java` - 添加了JSON格式化注解
- `debug.html` - 新增的调试工具页面

## 验证修复
1. 访问 http://localhost:8080/debug.html
2. 确认简历列表返回正确的数据
3. 上传测试简历验证整个流程
4. 在简历管理页面测试分析功能