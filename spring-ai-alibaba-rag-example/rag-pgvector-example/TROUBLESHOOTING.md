# 文件上传问题故障排除指南

## 🔍 问题诊断步骤

### 1. 确认项目已启动
```bash
# 检查端口8080是否被监听
netstat -ano | findstr :8080

# 或者访问 http://localhost:8080
```

### 2. 使用测试工具验证
访问以下测试页面进行详细诊断：

- **文件上传测试**: http://localhost:8080/upload-test.html
- **完整调试工具**: http://localhost:8080/debug.html
- **主页面**: http://localhost:8080/resume-analysis

### 3. 浏览器控制台检查
1. 按 F12 打开开发者工具
2. 切换到 Console 标签
3. 尝试上传文件
4. 查看是否有错误信息

### 4. 网络请求检查
1. 在开发者工具中切换到 Network 标签
2. 尝试上传文件
3. 查看 `/ai/resume/upload` 请求的详情：
   - 请求状态码
   - 请求头
   - 响应内容

## 🔧 常见问题及解决方案

### 问题1: 选择文件后无响应
**可能原因**: JavaScript函数未正确绑定或有错误

**解决方案**:
1. 检查浏览器控制台是否有JavaScript错误
2. 确认文件大小 < 10MB
3. 确认文件格式为 PDF、DOC、DOCX、TXT

### 问题2: 上传按钮禁用
**可能原因**: 文件选择逻辑有问题

**解决方案**:
1. 检查是否选择了有效文件
2. 刷新页面重新选择文件
3. 使用测试页面验证

### 问题3: 服务器连接失败
**可能原因**: 
- 服务器未启动
- 端口冲突
- JDK版本问题

**解决方案**:
```bash
# 确认JDK 17配置
java -version

# 启动项目
cd "spring-ai-alibaba-rag-example\rag-pgvector-example"
mvn spring-boot:run
```

### 问题4: 上传成功但界面无响应
**可能原因**: 前端响应处理逻辑有问题

**解决方案**:
1. 检查浏览器控制台的响应内容
2. 确认返回的JSON格式是否正确
3. 检查 `result.success` 和 `result.resumeId` 字段

### 问题5: 文件格式不支持
**可能原因**: 文件类型验证失败

**支持格式**:
- PDF (.pdf)
- Word文档 (.doc, .docx) 
- 纯文本 (.txt)

**文件大小限制**: 最大 10MB

## 🚀 快速修复命令

### 重启服务器
```bash
# 停止当前进程 (Ctrl+C)
# 重新启动
mvn spring-boot:run
```

### 清除浏览器缓存
- Chrome: Ctrl+Shift+Delete
- 或者使用无痕模式测试

### 验证环境配置
```bash
# 检查Java版本
java -version

# 检查Maven版本  
mvn -version

# 检查项目编译
mvn compile
```

## 📊 测试检查清单

- [ ] 服务器在8080端口正常运行
- [ ] 浏览器可以访问 http://localhost:8080
- [ ] 控制台无JavaScript错误
- [ ] 文件格式和大小符合要求
- [ ] 网络请求能正常发送和接收
- [ ] 响应包含正确的JSON结构

## 📞 技术支持信息

如果问题仍然存在，请提供以下信息：

1. **Java版本**: `java -version`
2. **浏览器版本**: Chrome/Firefox/Edge 版本号
3. **错误截图**: 包含控制台错误信息
4. **网络请求详情**: Network面板中的请求/响应信息
5. **服务器日志**: Maven启动时的完整日志

## 🔗 相关链接

- 主页面: http://localhost:8080/resume-analysis
- 上传测试: http://localhost:8080/upload-test.html  
- 调试工具: http://localhost:8080/debug.html
- 简历管理: http://localhost:8080/management

---

**最后更新**: 2024年 | **版本**: Spring AI Alibaba RAG Example v1.0