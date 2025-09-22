// 简历分析页面功能脚本
// 依赖 common.js，使用其中定义的 API_BASE 和其他公共函数

// 增强版API调用函数，带有详细的日志输出
async function apiCallWithLogging(url, options = {}) {
    try {
        console.log('发送请求:', url, options);
        
        const response = await fetch(url, {
            ...options
        });
        
        console.log('响应状态:', response.status, response.statusText);
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('响应错误内容:', errorText);
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }
        
        // 检查响应类型
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            const result = await response.json();
            console.log('JSON响应:', result);
            return result;
        } else {
            const result = await response.text();
            console.log('文本响应:', result);
            return result;
        }
    } catch (error) {
        console.error('API调用失败:', error);
        throw error;
    }
}

let currentResumeId = null;
let selectedFile = null;

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeFileUpload();
    initializeDragAndDrop();
    
    // 检查是否有演示参数
    const urlParams = new URLSearchParams(window.location.search);
    const demoResumeId = urlParams.get('demo');
    if (demoResumeId) {
        loadDemoResult(demoResumeId);
    }
});

// 初始化文件上传
function initializeFileUpload() {
    const fileInput = document.getElementById('fileInput');
    const uploadBtn = document.getElementById('uploadBtn');
    
    fileInput.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
            handleFileSelection(file);
        }
    });
}

// 初始化拖拽上传
function initializeDragAndDrop() {
    const uploadArea = document.getElementById('fileUploadArea');
    
    uploadArea.addEventListener('dragover', function(e) {
        e.preventDefault();
        uploadArea.classList.add('drag-over');
    });
    
    uploadArea.addEventListener('dragleave', function(e) {
        e.preventDefault();
        uploadArea.classList.remove('drag-over');
    });
    
    uploadArea.addEventListener('drop', function(e) {
        e.preventDefault();
        uploadArea.classList.remove('drag-over');
        
        const files = e.dataTransfer.files;
        if (files.length > 0) {
            handleFileSelection(files[0]);
        }
    });
}

// 处理文件选择
function handleFileSelection(file) {
    // 验证文件类型 - 使用 common.js 中的函数
    if (!window.validateFileType || !window.validateFileType(file)) {
        if (window.showErrorMessage) {
            window.showErrorMessage('不支持的文件格式，请上传PDF、DOC、DOCX或TXT文件');
        }
        return;
    }
    
    // 验证文件大小 - 使用 common.js 中的函数
    if (!window.validateFileSize || !window.validateFileSize(file, 10)) {
        if (window.showErrorMessage) {
            window.showErrorMessage('文件大小不能超过10MB');
        }
        return;
    }
    
    selectedFile = file;
    
    // 更新UI显示
    const uploadArea = document.getElementById('fileUploadArea');
    const fileSizeText = window.formatFileSize ? window.formatFileSize(file.size) : (file.size + ' bytes');
    uploadArea.innerHTML = `
        <i class="fas fa-file-check"></i>
        <p><strong>已选择文件:</strong> ${file.name}</p>
        <p class="file-info">文件大小: ${fileSizeText}</p>
        <button class="btn-secondary" onclick="resetFileSelection()">重新选择</button>
    `;
    
    // 启用上传按钮
    const uploadBtn = document.getElementById('uploadBtn');
    if (uploadBtn) {
        uploadBtn.disabled = false;
    }
}

// 重置文件选择
function resetFileSelection() {
    selectedFile = null;
    const fileInput = document.getElementById('fileInput');
    fileInput.value = '';
    
    const uploadArea = document.getElementById('fileUploadArea');
    uploadArea.innerHTML = `
        <i class="fas fa-file-upload"></i>
        <p>拖拽文件至此处或点击选择文件</p>
        <p class="file-info">支持格式：PDF、DOC、DOCX、TXT（最大10MB）</p>
        <input type="file" id="fileInput" accept=".pdf,.doc,.docx,.txt" style="display: none;">
        <button class="btn-primary" onclick="document.getElementById('fileInput').click()">
            选择文件
        </button>
    `;
    
    // 重新初始化文件上传
    initializeFileUpload();
    
    // 禁用上传按钮
    const uploadBtn = document.getElementById('uploadBtn');
    uploadBtn.disabled = true;
}

// 上传文件
async function uploadFile() {
    if (!selectedFile) {
        if (window.showErrorMessage) {
            window.showErrorMessage('请先选择文件');
        }
        return;
    }
    
    const candidateName = document.getElementById('candidateName')?.value?.trim();
    
    try {
        showProgress('正在上传文件...', 20);
        
        // 直接调用上传逻辑，使用 common.js 中的 API_BASE
        const formData = new FormData();
        formData.append('file', selectedFile);
        if (candidateName) {
            formData.append('candidateName', candidateName);
        }
        
        const result = await apiCallWithLogging(`${window.API_BASE || '/ai/resume'}/upload`, {
            method: 'POST',
            body: formData
        });
        
        console.log('上传响应:', result);
        
        // 检查响应的类型和结构
        if (typeof result === 'object' && result !== null) {
            // JSON对象响应 - 使用 isSuccess() 方法名称
            if (result.success === true && result.resumeId) {
                showProgress('文件上传成功，正在处理...', 60);
                currentResumeId = result.resumeId;
                
                // 显示基本信息
                displayBasicInfo({
                    resumeId: result.resumeId,
                    candidateName: candidateName || '未指定',
                    message: result.message
                });
                
                // 开始分析流程
                await startAnalysisProcess();
                
            } else {
                hideProgress();
                const errorMsg = result.message || '未知错误';
                if (window.showErrorMessage) {
                    window.showErrorMessage('上传失败: ' + errorMsg);
                }
            }
        } else if (typeof result === 'string') {
            // 字符串响应，可能是错误信息或成功消息
            if (result.includes('success') || result.includes('成功')) {
                // 如果是成功消息，但没有resumeId，则简单显示成功
                showProgress('处理完成', 100);
                setTimeout(hideProgress, 2000);
                if (window.showSuccess) {
                    window.showSuccess('文件处理成功！');
                }
            } else {
                hideProgress();
                if (window.showErrorMessage) {
                    window.showErrorMessage('上传失败: ' + result);
                }
            }
        } else {
            hideProgress();
            if (window.showErrorMessage) {
                window.showErrorMessage('服务器返回未知类型的响应');
            }
        }
    } catch (error) {
        hideProgress();
        console.error('上传失败:', error);
        if (window.showErrorMessage) {
            window.showErrorMessage('上传失败: ' + error.message);
        }
    }
}

// 上传文本
async function uploadText() {
    const text = document.getElementById('resumeText')?.value?.trim();
    const candidateName = document.getElementById('candidateNameText')?.value?.trim();
    
    if (!text) {
        if (window.showErrorMessage) {
            window.showErrorMessage('请输入简历内容');
        }
        return;
    }
    
    try {
        showProgress('正在处理简历文本...', 30);
        
        // 直接调用上传逻辑
        const formData = new FormData();
        formData.append('text', text);
        if (candidateName) {
            formData.append('candidateName', candidateName);
        }
        
        const result = await apiCallWithLogging(`${window.API_BASE || '/ai/resume'}/import-text`, {
            method: 'POST',
            body: formData
        });
        
        console.log('文本上传响应:', result);
        
        // 检查结果是否是JSON对象
        if (typeof result === 'object' && result !== null && result.success === true && result.resumeId) {
            showProgress('文本处理成功...', 60);
            currentResumeId = result.resumeId;
            
            // 显示基本信息
            displayBasicInfo({
                resumeId: result.resumeId,
                candidateName: candidateName || '未指定',
                message: result.message
            });
            
            // 开始分析流程
            await startAnalysisProcess();
            
        } else {
            hideProgress();
            const errorMsg = (result && result.message) || '处理失败';
            if (window.showErrorMessage) {
                window.showErrorMessage('处理失败: ' + errorMsg);
            }
        }
    } catch (error) {
        hideProgress();
        console.error('文本处理失败:', error);
        if (window.showErrorMessage) {
            window.showErrorMessage('处理失败: ' + error.message);
        }
    }
}

// 显示进度
function showProgress(text, percentage) {
    const progressSection = document.getElementById('progressSection');
    const progressText = document.getElementById('progressText');
    const progressFill = document.getElementById('progressFill');
    
    progressSection.style.display = 'block';
    progressText.textContent = text;
    progressFill.style.width = percentage + '%';
    
    // 滚动到进度区域
    progressSection.scrollIntoView({ behavior: 'smooth' });
}

// 隐藏进度
function hideProgress() {
    const progressSection = document.getElementById('progressSection');
    progressSection.style.display = 'none';
}

// 显示基本信息
function displayBasicInfo(result) {
    const resultSection = document.getElementById('resultSection');
    const resumeId = document.getElementById('resumeId');
    const candidateDisplay = document.getElementById('candidateDisplay');
    const uploadTime = document.getElementById('uploadTime');
    const processStatus = document.getElementById('processStatus');
    
    if (resultSection) resultSection.style.display = 'block';
    if (resumeId) resumeId.textContent = result.resumeId || currentResumeId || '-';
    if (candidateDisplay) candidateDisplay.textContent = result.candidateName || '未指定';
    if (uploadTime) {
        const formatDateFunc = window.formatDate || ((date) => new Date(date).toLocaleString('zh-CN'));
        uploadTime.textContent = formatDateFunc(new Date());
    }
    if (processStatus) {
        processStatus.textContent = '处理中...';
        processStatus.className = 'status-processing';
    }
    
    // 滚动到结果区域
    if (resultSection) {
        resultSection.scrollIntoView({ behavior: 'smooth' });
    }
}

// 开始分析流程
async function startAnalysisProcess() {
    try {
        // 并行获取摘要和分析结果
        const [summary, analysis] = await Promise.all([
            apiCallWithLogging(`${window.API_BASE || '/ai/resume'}/summary/${currentResumeId}`),
            apiCallWithLogging(`${window.API_BASE || '/ai/resume'}/analyze/${currentResumeId}`)
        ]);
        
        // 显示摘要
        const summaryContent = document.getElementById('summaryContent');
        if (summaryContent) {
            summaryContent.innerHTML = `<p>${summary}</p>`;
        }
        
        // 显示分析结果
        const analysisContent = document.getElementById('analysisContent');
        if (analysisContent) {
            analysisContent.innerHTML = `<pre style="white-space: pre-wrap; font-family: inherit;">${analysis.summary || '分析完成'}</pre>`;
        }
        
        // 更新状态
        const processStatus = document.getElementById('processStatus');
        if (processStatus) {
            processStatus.textContent = '已完成';
            processStatus.className = 'status-analyzed';
        }
        
        showProgress('分析完成！', 100);
        
        // 显示岗位推荐按钮
        const jobRecommendBtn = document.getElementById('jobRecommendBtn');
        if (jobRecommendBtn) {
            jobRecommendBtn.style.display = 'inline-flex';
        }
        
        // 延迟隐藏进度条
        setTimeout(hideProgress, 2000);
        
        if (window.showSuccess) {
            window.showSuccess('简历分析完成！');
        }
        
    } catch (error) {
        console.error('分析失败:', error);
        if (window.showErrorMessage) {
            window.showErrorMessage('分析失败: ' + error.message);
        }
        
        const processStatus = document.getElementById('processStatus');
        if (processStatus) {
            processStatus.textContent = '分析失败';
            processStatus.className = 'status-error';
        }
        
        hideProgress();
    }
}

// 加载演示结果
async function loadDemoResult(resumeId) {
    currentResumeId = resumeId;
    
    try {
        showProgress('正在加载演示结果...', 50);
        
        // 显示基本信息
        displayBasicInfo({
            resumeId: resumeId,
            candidateName: '张三（演示）'
        });
        
        // 开始分析流程
        await startAnalysisProcess();
        
    } catch (error) {
        console.error('加载演示失败:', error);
        showErrorMessage('加载演示失败');
        hideProgress();
    }
}

// 发送聊天消息
async function sendMessage() {
    const chatInput = document.getElementById('chatInput');
    const question = chatInput.value.trim();
    
    if (!question) {
        showErrorMessage('请输入问题');
        return;
    }
    
    if (!currentResumeId) {
        showErrorMessage('请先上传简历');
        return;
    }
    
    const chatMessages = document.getElementById('chatMessages');
    
    // 添加用户消息
    const userMessage = document.createElement('div');
    userMessage.className = 'user-message';
    userMessage.innerHTML = `<strong>您:</strong> ${question}`;
    chatMessages.appendChild(userMessage);
    
    // 添加AI消息容器
    const aiMessage = document.createElement('div');
    aiMessage.className = 'ai-message';
    aiMessage.innerHTML = '<strong>AI助手:</strong> <span class="typing">正在思考...</span>';
    chatMessages.appendChild(aiMessage);
    
    // 清空输入框
    chatInput.value = '';
    
    // 滚动到底部
    chatMessages.scrollTop = chatMessages.scrollHeight;
    
    try {
        // 创建流式聊天
        let fullResponse = '';
        const url = `${API_BASE}/chat/${currentResumeId}?question=${encodeURIComponent(question)}`;
        const eventSource = new EventSource(url);
        
        eventSource.onmessage = function(event) {
            const data = event.data;
            if (data && data !== '[DONE]') {
                fullResponse += data;
                const aiSpan = aiMessage.querySelector('span');
                if (aiSpan) {
                    aiSpan.textContent = fullResponse;
                    aiSpan.className = '';
                }
                chatMessages.scrollTop = chatMessages.scrollHeight;
            }
        };
        
        eventSource.onerror = function(error) {
            console.error('聊天流错误:', error);
            eventSource.close();
            const aiSpan = aiMessage.querySelector('span');
            if (aiSpan) {
                aiSpan.textContent = '抱歉，回答时出现错误，请重试。';
                aiSpan.className = 'error';
            }
        };
        
        eventSource.addEventListener('close', function() {
            eventSource.close();
            console.log('聊天完成:', fullResponse);
        });
        
    } catch (error) {
        console.error('发送消息失败:', error);
        const aiSpan = aiMessage.querySelector('span');
        if (aiSpan) {
            aiSpan.textContent = '抱歉，发送消息失败，请重试。';
            aiSpan.className = 'error';
        }
    }
}

// 处理聊天输入框回车事件
function handleChatKeyPress(event) {
    if (event.key === 'Enter') {
        sendMessage();
    }
}

// 快速提问
function askQuestion(question) {
    const chatInput = document.getElementById('chatInput');
    chatInput.value = question;
    sendMessage();
}

// 重置表单
function resetForm() {
    // 重置文件上传
    resetFileSelection();
    
    // 清空文本区域
    document.getElementById('resumeText').value = '';
    document.getElementById('candidateName').value = '';
    document.getElementById('candidateNameText').value = '';
    
    // 隐藏结果区域
    document.getElementById('resultSection').style.display = 'none';
    document.getElementById('progressSection').style.display = 'none';
    
    // 清空聊天记录
    const chatMessages = document.getElementById('chatMessages');
    chatMessages.innerHTML = `
        <div class="system-message">
            <i class="fas fa-robot"></i>
            您好！我是AI助手，可以回答关于这份简历的任何问题。
        </div>
    `;
    
    // 重置变量
    currentResumeId = null;
    selectedFile = null;
    
    // 滚动到顶部
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

// 查看管理页面
function viewManagement() {
    window.location.href = '/management';
}

// 显示岗位推荐
async function showJobRecommendations() {
    if (!currentResumeId) {
        if (window.showErrorMessage) {
            window.showErrorMessage('请先上传简历');
        }
        return;
    }
    
    const recommendationsDiv = document.getElementById('jobRecommendations');
    const listDiv = document.getElementById('jobRecommendationsList');
    
    // 显示推荐区域
    recommendationsDiv.style.display = 'block';
    
    // 显示加载状态
    listDiv.innerHTML = `
        <div class="loading-recommendations">
            <i class="fas fa-spinner fa-spin"></i>
            <p>正在分析匹配的岗位...</p>
        </div>
    `;
    
    try {
        // 获取岗位推荐
        const recommendations = await apiCallWithLogging(`${window.API_BASE || '/ai/resume'}/recommend/${currentResumeId}?topK=5`);
        
        if (recommendations && recommendations.length > 0) {
            // 渲染推荐结果
            listDiv.innerHTML = recommendations.map(recommendation => createJobRecommendationCard(recommendation)).join('');
        } else {
            listDiv.innerHTML = `
                <div class="empty-state">
                    <i class="fas fa-briefcase"></i>
                    <h3>暂无匹配的岗位</h3>
                    <p>系统中暂无适合的岗位推荐</p>
                </div>
            `;
        }
        
        // 滚动到推荐区域
        recommendationsDiv.scrollIntoView({ behavior: 'smooth' });
        
    } catch (error) {
        console.error('获取岗位推荐失败:', error);
        listDiv.innerHTML = `
            <div class="error-message">
                <i class="fas fa-exclamation-triangle"></i>
                <p>获取岗位推荐失败：${error.message}</p>
                <button class="btn-secondary" onclick="showJobRecommendations()">重试</button>
            </div>
        `;
    }
}

// 隐藏岗位推荐
function hideJobRecommendations() {
    const recommendationsDiv = document.getElementById('jobRecommendations');
    recommendationsDiv.style.display = 'none';
}

// 创建岗位推荐卡片
function createJobRecommendationCard(recommendation) {
    const matchScoreClass = getMatchScoreClass(recommendation.matchLevel);
    const matchScoreText = getMatchScoreText(recommendation.matchLevel);
    
    return `
        <div class="job-item">
            <div class="job-header">
                <div>
                    <h4 class="job-title">${recommendation.job?.title || '未知岗位'}</h4>
                    <div class="job-company">${recommendation.job?.company || '未知公司'}</div>
                </div>
                <span class="match-score ${matchScoreClass}">
                    ${Math.round(recommendation.matchScore || 0)}分 ${matchScoreText}
                </span>
            </div>
            
            <div class="job-info">
                <p><i class="fas fa-map-marker-alt"></i> ${recommendation.job?.location || '地点待定'}</p>
                <p><i class="fas fa-clock"></i> ${recommendation.job?.experience || '经验不限'}</p>
                <p><i class="fas fa-graduation-cap"></i> ${recommendation.job?.education || '学历不限'}</p>
                <p><i class="fas fa-money-bill-wave"></i> ${recommendation.job?.salaryRange || '薪资面议'}</p>
            </div>
            
            ${recommendation.summary ? `
                <div class="job-summary">${recommendation.summary}</div>
            ` : ''}
            
            <div class="job-item-actions">
                <button class="btn-primary btn-small" onclick="viewJobDetail('${recommendation.jobId}')">
                    <i class="fas fa-eye"></i> 查看详情
                </button>
                <button class="btn-secondary btn-small" onclick="showMatchingDetail('${currentResumeId}', '${recommendation.jobId}')">
                    <i class="fas fa-chart-line"></i> 匹配详情
                </button>
            </div>
        </div>
    `;
}

// 获取匹配分数样式类
function getMatchScoreClass(matchLevel) {
    const classMap = {
        'EXCELLENT': 'excellent',
        'GOOD': 'good',
        'FAIR': 'fair',
        'POOR': 'poor'
    };
    return classMap[matchLevel] || 'fair';
}

// 获取匹配分数文本
function getMatchScoreText(matchLevel) {
    const textMap = {
        'EXCELLENT': '优秀',
        'GOOD': '良好',
        'FAIR': '一般',
        'POOR': '较差'
    };
    return textMap[matchLevel] || '一般';
}

// 查看岗位详情
function viewJobDetail(jobId) {
    // 可以打开新窗口或在当前页面显示岗位详情
    window.open(`/jobs?id=${jobId}`, '_blank');
}

// 显示匹配详情
async function showMatchingDetail(resumeId, jobId) {
    try {
        // 获取详细匹配结果
        const matchingResult = await apiCallWithLogging(`${window.API_BASE || '/ai/resume'}/match/${resumeId}/${jobId}`, {
            method: 'POST'
        });
        
        // 显示匹配详情模态框（这里可以实现一个模态框显示详细信息）
        alert(`匹配分数: ${Math.round(matchingResult.matchScore || 0)}分\n匹配级别: ${getMatchScoreText(matchingResult.matchLevel)}\n\n${matchingResult.summary || '暂无详细分析'}`);
        
    } catch (error) {
        console.error('获取匹配详情失败:', error);
        if (window.showErrorMessage) {
            window.showErrorMessage('获取匹配详情失败: ' + error.message);
        }
    }
}

// 导出函数供HTML调用
window.uploadFile = uploadFile;
window.uploadText = uploadText;
window.sendMessage = sendMessage;
window.handleChatKeyPress = handleChatKeyPress;
window.askQuestion = askQuestion;
window.resetForm = resetForm;
window.viewManagement = viewManagement;
window.resetFileSelection = resetFileSelection;
window.showJobRecommendations = showJobRecommendations;
window.hideJobRecommendations = hideJobRecommendations;
window.viewJobDetail = viewJobDetail;
window.showMatchingDetail = showMatchingDetail;