// 通用工具函数和API调用

// API基础URL
const API_BASE = '/ai/resume';

// 通用API调用函数
async function apiCall(url, method = 'GET', data = null) {
    try {
        const options = {
            method: method,
            headers: {}
        };
        
        // 如果有数据需要发送
        if (data !== null) {
            if (method === 'POST' || method === 'PUT' || method === 'PATCH') {
                options.headers['Content-Type'] = 'application/json';
                options.body = JSON.stringify(data);
            }
        }
        
        const response = await fetch(url, options);
        
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }
        
        // 检查响应类型
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            return await response.json();
        } else {
            return await response.text();
        }
    } catch (error) {
        console.error('API调用失败:', error);
        throw error;
    }
}

// 文件上传API
async function uploadFile(file, candidateName) {
    const formData = new FormData();
    formData.append('file', file);
    if (candidateName) {
        formData.append('candidateName', candidateName);
    }
    
    // 对于文件上传，使用特殊的参数处理
    const response = await fetch(`${API_BASE}/upload`, {
        method: 'POST',
        body: formData
        // 不设置Content-Type，让浏览器自动设置multipart/form-data
    });
    
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
    }
    
    return await response.json();
}

// 文本上传API
async function uploadText(text, candidateName) {
    const formData = new FormData();
    formData.append('text', text);
    if (candidateName) {
        formData.append('candidateName', candidateName);
    }
    
    // 对于文本上传，使用特殊的参数处理
    const response = await fetch(`${API_BASE}/import-text`, {
        method: 'POST',
        body: formData
        // 不设置Content-Type，让浏览器自动设置multipart/form-data
    });
    
    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`HTTP ${response.status}: ${errorText}`);
    }
    
    return await response.json();
}

// 获取简历列表
async function getResumeList() {
    return await apiCall(`${API_BASE}/list`);
}

// 分析简历
async function analyzeResume(resumeId) {
    return await apiCall(`${API_BASE}/analyze/${resumeId}`);
}

// 获取简历摘要
async function getResumeSummary(resumeId) {
    return await apiCall(`${API_BASE}/summary/${resumeId}`);
}

// 获取已存在的分析结果
async function getExistingAnalysis(resumeId) {
    return await apiCall(`${API_BASE}/analysis/${resumeId}`);
}

// 删除简历
async function deleteResume(resumeId) {
    return await apiCall(`${API_BASE}/${resumeId}`, 'DELETE');
}

// 流式聊天API（使用EventSource）
function createChatStream(resumeId, question, onMessage, onError, onComplete) {
    const url = `${API_BASE}/chat/${resumeId}?question=${encodeURIComponent(question)}`;
    const eventSource = new EventSource(url);
    
    let fullMessage = '';
    
    eventSource.onmessage = function(event) {
        const data = event.data;
        if (data && data !== '[DONE]') {
            fullMessage += data;
            onMessage(data, fullMessage);
        }
    };
    
    eventSource.onerror = function(event) {
        eventSource.close();
        if (onError) {
            onError(event);
        }
    };
    
    eventSource.addEventListener('close', function() {
        eventSource.close();
        if (onComplete) {
            onComplete(fullMessage);
        }
    });
    
    // 返回关闭函数
    return () => eventSource.close();
}

// 工具函数
function formatDate(dateString) {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN');
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function getStatusText(status) {
    const statusMap = {
        'UPLOADED': '已上传',
        'PROCESSING': '处理中',
        'ANALYZED': '已分析',
        'ERROR': '错误'
    };
    return statusMap[status] || status;
}

function getStatusClass(status) {
    const classMap = {
        'UPLOADED': 'status-uploaded',
        'PROCESSING': 'status-processing',
        'ANALYZED': 'status-analyzed',
        'ERROR': 'status-error'
    };
    return classMap[status] || '';
}

// 显示加载状态
function showLoading(element, text = '加载中...') {
    if (element) {
        element.innerHTML = `
            <div class="loading-state">
                <i class="fas fa-spinner fa-spin"></i>
                <p>${text}</p>
            </div>
        `;
    }
}

// 显示错误信息
function showError(element, message) {
    if (element) {
        element.innerHTML = `
            <div class="error-state">
                <i class="fas fa-exclamation-triangle"></i>
                <p>错误: ${message}</p>
            </div>
        `;
    }
}

// 显示成功消息
function showSuccess(message, duration = 3000) {
    // 创建成功提示元素
    const successDiv = document.createElement('div');
    successDiv.className = 'success-toast';
    successDiv.innerHTML = `
        <i class="fas fa-check-circle"></i>
        <span>${message}</span>
    `;
    
    // 添加样式
    successDiv.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #28a745;
        color: white;
        padding: 1rem;
        border-radius: 5px;
        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
        z-index: 1000;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        animation: slideIn 0.3s ease-out;
    `;
    
    document.body.appendChild(successDiv);
    
    // 自动移除
    setTimeout(() => {
        successDiv.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => {
            if (successDiv.parentNode) {
                successDiv.parentNode.removeChild(successDiv);
            }
        }, 300);
    }, duration);
}

// 显示错误消息
function showErrorMessage(message, duration = 5000) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-toast';
    errorDiv.innerHTML = `
        <i class="fas fa-exclamation-triangle"></i>
        <span>${message}</span>
    `;
    
    errorDiv.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: #dc3545;
        color: white;
        padding: 1rem;
        border-radius: 5px;
        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
        z-index: 1000;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        animation: slideIn 0.3s ease-out;
    `;
    
    document.body.appendChild(errorDiv);
    
    setTimeout(() => {
        errorDiv.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => {
            if (errorDiv.parentNode) {
                errorDiv.parentNode.removeChild(errorDiv);
            }
        }, 300);
    }, duration);
}

// 确认对话框
function confirmAction(message, onConfirm) {
    if (confirm(message)) {
        onConfirm();
    }
}

// 文件类型验证
function validateFileType(file) {
    const allowedTypes = ['application/pdf', 'application/msword', 
                         'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                         'text/plain'];
    const allowedExtensions = ['.pdf', '.doc', '.docx', '.txt'];
    
    const isValidType = allowedTypes.includes(file.type);
    const hasValidExtension = allowedExtensions.some(ext => 
        file.name.toLowerCase().endsWith(ext)
    );
    
    return isValidType || hasValidExtension;
}

// 文件大小验证
function validateFileSize(file, maxSizeMB = 10) {
    const maxSizeBytes = maxSizeMB * 1024 * 1024;
    return file.size <= maxSizeBytes;
}

// 防抖函数
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// 节流函数
function throttle(func, limit) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    }
}

// 页面初始化时的通用设置
document.addEventListener('DOMContentLoaded', function() {
    // 设置活动导航链接
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.nav-link');
    
    navLinks.forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        }
    });
    
    // 添加全局错误处理
    window.addEventListener('unhandledrejection', function(event) {
        console.error('未处理的Promise拒绝:', event.reason);
        showErrorMessage('系统错误，请稍后重试');
    });
});

// 导出全局函数供HTML使用
window.API_BASE = API_BASE;
window.apiCall = apiCall;
window.uploadFile = uploadFile;
window.uploadText = uploadText;
window.getResumeList = getResumeList;
window.analyzeResume = analyzeResume;
window.getResumeSummary = getResumeSummary;
window.getExistingAnalysis = getExistingAnalysis;
window.deleteResume = deleteResume;
window.createChatStream = createChatStream;
window.formatDate = formatDate;
window.formatFileSize = formatFileSize;
window.getStatusText = getStatusText;
window.getStatusClass = getStatusClass;
window.showLoading = showLoading;
window.showError = showError;
window.showSuccess = showSuccess;
window.showErrorMessage = showErrorMessage;
window.confirmAction = confirmAction;
window.validateFileType = validateFileType;
window.validateFileSize = validateFileSize;
window.debounce = debounce;
window.throttle = throttle;