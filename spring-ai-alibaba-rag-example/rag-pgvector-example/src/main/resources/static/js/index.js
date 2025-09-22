// 首页功能脚本

// 系统状态检查
async function checkSystemStatus() {
    const backendStatus = document.getElementById('backend-status');
    const databaseStatus = document.getElementById('database-status');
    const aiStatus = document.getElementById('ai-status');
    const resumeCount = document.getElementById('resume-count');
    
    // 检查后端服务状态
    try {
        await apiCall('/ai/resume/list');
        backendStatus.textContent = '正常';
        backendStatus.style.color = '#28a745';
    } catch (error) {
        backendStatus.textContent = '异常';
        backendStatus.style.color = '#dc3545';
    }
    
    // 检查数据库和AI服务（通过获取简历列表）
    try {
        const resumes = await getResumeList();
        databaseStatus.textContent = '正常';
        databaseStatus.style.color = '#28a745';
        
        aiStatus.textContent = '正常';
        aiStatus.style.color = '#28a745';
        
        resumeCount.textContent = resumes.length;
    } catch (error) {
        databaseStatus.textContent = '异常';
        databaseStatus.style.color = '#dc3545';
        
        aiStatus.textContent = '未知';
        aiStatus.style.color = '#ffc107';
        
        resumeCount.textContent = '0';
    }
}

// 加载演示简历
async function loadDemoResume() {
    const demoText = `张三
联系方式：13800138000
邮箱：zhangsan@example.com

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
- 工具：Git、Maven、Docker、Kubernetes`;

    try {
        showSuccess('正在加载演示简历...');
        
        const result = await uploadText(demoText, '张三（演示）');
        
        if (result.success) {
            showSuccess('演示简历加载成功！');
            
            // 延迟跳转到分析页面
            setTimeout(() => {
                window.location.href = `/resume?demo=${result.resumeId}`;
            }, 1000);
        } else {
            showErrorMessage('演示简历加载失败: ' + result.message);
        }
    } catch (error) {
        console.error('加载演示简历失败:', error);
        showErrorMessage('演示简历加载失败，请稍后重试');
    }
}

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 检查系统状态
    checkSystemStatus();
    
    // 添加动画效果
    const cards = document.querySelectorAll('.intro-card, .action-card');
    cards.forEach((card, index) => {
        setTimeout(() => {
            card.classList.add('fade-in');
        }, index * 100);
    });
    
    // 定期刷新系统状态（每30秒）
    setInterval(checkSystemStatus, 30000);
});

// 导出函数供HTML调用
window.loadDemoResume = loadDemoResume;
window.checkSystemStatus = checkSystemStatus;