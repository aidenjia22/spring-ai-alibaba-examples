// 简历管理页面功能脚本

let allResumes = [];
let filteredResumes = [];
let currentPage = 1;
const itemsPerPage = 12;
let currentResumeId = null;

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    loadResumeList();
    initializeFilters();
});

// 加载简历列表
async function loadResumeList() {
    const loadingState = document.getElementById('loadingState');
    const emptyState = document.getElementById('emptyState');
    const resumeGrid = document.getElementById('resumeGrid');
    
    try {
        if (loadingState) loadingState.style.display = 'block';
        if (emptyState) emptyState.style.display = 'none';
        if (resumeGrid) resumeGrid.innerHTML = '';
        
        console.log('开始加载简历列表...');
        
        // 确保使用正确的API调用
        const resumes = await apiCall(`${API_BASE}/list`);
        console.log('获取到的简历数据:', resumes);
        console.log('数据类型:', typeof resumes, '是否为数组:', Array.isArray(resumes));
        
        // 确保数据是数组格式
        const resumeArray = Array.isArray(resumes) ? resumes : [];
        
        // 过滤掉无效的简历数据
        const validResumes = resumeArray.filter(resume => {
            if (!resume) {
                console.warn('发现空的简历对象，已过滤');
                return false;
            }
            if (!resume.resumeId || resume.resumeId === 'null' || resume.resumeId === 'undefined') {
                console.warn('发现无效的resumeId，已过滤:', resume);
                return false;
            }
            return true;
        });
        
        if (validResumes.length < resumeArray.length) {
            console.warn(`过滤掉 ${resumeArray.length - validResumes.length} 个无效的简历数据`);
        }
        
        allResumes = validResumes;
        filteredResumes = validResumes;
        
        if (loadingState) loadingState.style.display = 'none';
        
        if (validResumes.length === 0) {
            console.log('没有有效的简历数据');
            if (emptyState) emptyState.style.display = 'block';
        } else {
            console.log(`加载了 ${validResumes.length} 个有效的简历`);
            updateStatistics();
            renderResumeGrid();
            updatePagination();
        }
        
    } catch (error) {
        console.error('加载简历列表失败:', error);
        if (loadingState) loadingState.style.display = 'none';
        if (resumeGrid) showError(resumeGrid, '加载失败，请刷新页面重试: ' + error.message);
    }
}

// 更新统计信息
function updateStatistics() {
    const totalCount = document.getElementById('totalCount');
    const analyzedCount = document.getElementById('analyzedCount');
    const processingCount = document.getElementById('processingCount');
    
    totalCount.textContent = allResumes.length;
    analyzedCount.textContent = allResumes.filter(r => r.status === 'ANALYZED').length;
    processingCount.textContent = allResumes.filter(r => r.status === 'PROCESSING').length;
}

// 渲染简历网格
function renderResumeGrid() {
    const resumeGrid = document.getElementById('resumeGrid');
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const pageResumes = filteredResumes.slice(startIndex, endIndex);
    
    if (pageResumes.length === 0) {
        resumeGrid.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-search"></i>
                <h3>没有找到匹配的简历</h3>
                <p>请尝试调整筛选条件</p>
            </div>
        `;
        return;
    }
    
    resumeGrid.innerHTML = pageResumes.map(resume => createResumeCard(resume)).join('');
}

// 创建简历卡片
function createResumeCard(resume) {
    // 添加调试信息
    console.log('创建简历卡片:', resume);
    
    // 检查resume对象是否存在
    if (!resume) {
        console.error('简历对象为空');
        return `
            <div class="resume-card fade-in error-card">
                <div class="resume-header">
                    <div class="resume-title">错误：简历数据为空</div>
                    <span class="resume-status status-error">错误</span>
                </div>
                <div class="resume-info">
                    <p><i class="fas fa-exclamation-triangle"></i> 简历数据异常</p>
                </div>
            </div>
        `;
    }
    
    // 检查resumeId是否存在且有效
    if (!resume.resumeId || resume.resumeId === 'null' || resume.resumeId === 'undefined') {
        console.error('简历缺少有效的resumeId:', resume);
        return `
            <div class="resume-card fade-in error-card">
                <div class="resume-header">
                    <div class="resume-title">错误：缺少有效的简历ID</div>
                    <span class="resume-status status-error">错误</span>
                </div>
                <div class="resume-info">
                    <p><i class="fas fa-exclamation-triangle"></i> 简历数据异常: resumeId = ${resume.resumeId}</p>
                </div>
            </div>
        `;
    }
    
    const statusText = getStatusText(resume.status);
    const statusClass = getStatusClass(resume.status);
    const uploadTime = formatDate(resume.uploadTime);
    
    return `
        <div class="resume-card fade-in">
            <div class="resume-header">
                <div class="resume-title">${resume.candidateName || '未命名候选人'}</div>
                <span class="resume-status ${statusClass}">${statusText}</span>
            </div>
            
            <div class="resume-info">
                <p><i class="fas fa-file"></i> ${resume.originalFileName || '文本输入'}</p>
                <p><i class="fas fa-calendar"></i> ${uploadTime}</p>
                <p><i class="fas fa-tag"></i> ${resume.fileType || 'text'}</p>
                <p><i class="fas fa-fingerprint"></i> ${resume.resumeId.substring(0, 8)}...</p>
            </div>
            
            <div class="resume-actions">
                <button class="btn-primary btn-small" onclick="viewResumeDetail('${resume.resumeId}')">
                    <i class="fas fa-eye"></i> 查看
                </button>
                ${resume.status === 'ANALYZED' ? `
                    <button class="btn-success btn-small" onclick="viewAnalysisDetail('${resume.resumeId}')">
                        <i class="fas fa-chart-line"></i> 详细分析
                    </button>
                    <button class="btn-info btn-small" onclick="openJobMatchingModal('${resume.resumeId}', '${resume.candidateName || ''}')">
                        <i class="fas fa-bullseye"></i> 岗位匹配
                    </button>
                    <button class="btn-secondary btn-small" onclick="openChatModal('${resume.resumeId}', '${resume.candidateName || ''}')">
                        <i class="fas fa-comments"></i> 问答
                    </button>
                ` : ''}
                ${resume.status === 'UPLOADED' ? `
                    <button class="btn-secondary btn-small" onclick="analyzeResumeFromList('${resume.resumeId}')">
                        <i class="fas fa-brain"></i> 分析
                    </button>
                ` : ''}
                <button class="btn-danger btn-small" onclick="confirmDeleteResume('${resume.resumeId}', '${resume.candidateName || ''}')">
                    <i class="fas fa-trash"></i> 删除
                </button>
            </div>
        </div>
    `;
}

// 查看分析结果详情
function viewAnalysisDetail(resumeId) {
    console.log('跳转到分析详情页面 - resumeId:', resumeId);
    
    // 检查resumeId是否有效
    if (!resumeId || resumeId === 'null' || resumeId === 'undefined') {
        console.error('无效的resumeId:', resumeId);
        showErrorMessage('无效的简历ID');
        return;
    }
    
    // 跳转到分析详情页面
    window.location.href = `/analysis-detail?resumeId=${encodeURIComponent(resumeId)}`;
}

// 查看简历详情
function viewResumeDetail(resumeId) {
    console.log('查看简历详情 - resumeId:', resumeId, '类型:', typeof resumeId);
    
    // 检查resumeId是否有效
    if (!resumeId || resumeId === 'null' || resumeId === 'undefined') {
        console.error('无效的resumeId:', resumeId);
        showErrorMessage('无效的简历ID，无法查看详情');
        return;
    }
    
    const resume = allResumes.find(r => r.resumeId === resumeId);
    if (!resume) {
        console.error('找不到简历:', resumeId);
        console.log('当前所有简历:', allResumes.map(r => ({
            resumeId: r.resumeId,
            candidateName: r.candidateName
        })));
        showErrorMessage('找不到指定的简历');
        return;
    }
    
    const modal = document.getElementById('resumeModal');
    const modalTitle = document.getElementById('modalTitle');
    const modalBody = document.getElementById('modalBody');
    const analyzeBtn = document.getElementById('analyzeBtn');
    const deleteBtn = document.getElementById('deleteBtn');
    
    // 设置全局currentResumeId
    currentResumeId = resumeId;
    console.log('设置 currentResumeId 为:', currentResumeId);
    
    modalTitle.textContent = `简历详情 - ${resume.candidateName || '未命名'}`;
    
    modalBody.innerHTML = `
        <div class="resume-detail">
            <div class="detail-section">
                <h4><i class="fas fa-info-circle"></i> 基本信息</h4>
                <div class="detail-grid">
                    <div class="detail-item">
                        <label>简历ID:</label>
                        <span>${resume.resumeId}</span>
                    </div>
                    <div class="detail-item">
                        <label>候选人姓名:</label>
                        <span>${resume.candidateName || '未指定'}</span>
                    </div>
                    <div class="detail-item">
                        <label>文件名:</label>
                        <span>${resume.originalFileName || '文本输入'}</span>
                    </div>
                    <div class="detail-item">
                        <label>文件类型:</label>
                        <span>${resume.fileType || 'text'}</span>
                    </div>
                    <div class="detail-item">
                        <label>上传时间:</label>
                        <span>${formatDate(resume.uploadTime)}</span>
                    </div>
                    <div class="detail-item">
                        <label>处理状态:</label>
                        <span class="resume-status ${getStatusClass(resume.status)}">${getStatusText(resume.status)}</span>
                    </div>
                </div>
            </div>
            
            ${resume.status === 'ANALYZED' ? `
                <div class="detail-section">
                    <h4><i class="fas fa-chart-line"></i> 快速操作</h4>
                    <div class="quick-actions">
                        <button class="btn-primary" onclick="getSummaryInModal('${resume.resumeId}')">
                            <i class="fas fa-file-alt"></i> 获取摘要
                        </button>
                        <button class="btn-secondary" onclick="getAnalysisInModal('${resume.resumeId}')">
                            <i class="fas fa-brain"></i> 查看分析
                        </button>
                        <button class="btn-secondary" onclick="closeModal(); openChatModal('${resume.resumeId}', '${resume.candidateName || ''}')">
                            <i class="fas fa-comments"></i> 智能问答
                        </button>
                    </div>
                </div>
                
                <div class="detail-section">
                    <h4 id="resultTitle"></h4>
                    <div id="resultContent" class="result-content">
                        点击上方按钮查看相关信息
                    </div>
                </div>
            ` : ''}
        </div>
    `;
    
    // 设置按钮状态
    if (resume.status === 'UPLOADED') {
        analyzeBtn.style.display = 'inline-block';
        analyzeBtn.onclick = () => analyzeFromModal();
    } else {
        analyzeBtn.style.display = 'none';
    }
    
    deleteBtn.onclick = () => deleteFromModal();
    
    modal.style.display = 'flex';
}

// 在模态框中获取摘要
async function getSummaryInModal(resumeId) {
    const resultTitle = document.getElementById('resultTitle');
    const resultContent = document.getElementById('resultContent');
    
    resultTitle.innerHTML = '<i class="fas fa-file-alt"></i> 简历摘要';
    resultContent.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 正在生成摘要...';
    
    try {
        const summary = await getResumeSummary(resumeId);
        resultContent.innerHTML = `<p>${summary}</p>`;
    } catch (error) {
        resultContent.innerHTML = `<p class="error">获取摘要失败: ${error.message}</p>`;
    }
}

// 在模态框中获取分析结果
async function getAnalysisInModal(resumeId) {
    const resultTitle = document.getElementById('resultTitle');
    const resultContent = document.getElementById('resultContent');
    
    resultTitle.innerHTML = '<i class="fas fa-brain"></i> 详细分析';
    resultContent.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 正在分析...';
    
    try {
        const analysis = await analyzeResume(resumeId);
        resultContent.innerHTML = `<pre style="white-space: pre-wrap; font-family: inherit;">${analysis.summary || '分析完成'}</pre>`;
    } catch (error) {
        resultContent.innerHTML = `<p class="error">获取分析失败: ${error.message}</p>`;
    }
}

// 打开聊天模态框
function openChatModal(resumeId, candidateName) {
    // 检查resumeId是否有效
    if (!resumeId || resumeId === 'null' || resumeId === 'undefined') {
        console.error('无效的resumeId:', resumeId);
        showErrorMessage('无效的简历ID，无法开启聊天');
        return;
    }
    
    const chatModal = document.getElementById('chatModal');
    const modalChatMessages = document.getElementById('modalChatMessages');
    
    currentResumeId = resumeId;
    
    // 重置聊天记录
    modalChatMessages.innerHTML = `
        <div class="system-message">
            <i class="fas fa-robot"></i>
            您好！我可以回答关于${candidateName || '这份简历'}的任何问题。
        </div>
    `;
    
    chatModal.style.display = 'flex';
    
    // 聚焦到输入框
    setTimeout(() => {
        document.getElementById('modalChatInput').focus();
    }, 100);
}

// 发送模态框聊天消息
async function sendModalMessage() {
    const chatInput = document.getElementById('modalChatInput');
    const question = chatInput.value.trim();
    
    if (!question || !currentResumeId) return;
    
    const chatMessages = document.getElementById('modalChatMessages');
    
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
    
    chatInput.value = '';
    chatMessages.scrollTop = chatMessages.scrollHeight;
    
    try {
        let fullResponse = '';
        const closeStream = createChatStream(
            currentResumeId,
            question,
            (chunk, fullMessage) => {
                fullResponse = fullMessage;
                const aiSpan = aiMessage.querySelector('span');
                if (aiSpan) {
                    aiSpan.textContent = fullMessage;
                    aiSpan.className = '';
                }
                chatMessages.scrollTop = chatMessages.scrollHeight;
            },
            (error) => {
                console.error('聊天流错误:', error);
                const aiSpan = aiMessage.querySelector('span');
                if (aiSpan) {
                    aiSpan.textContent = '抱歉，回答时出现错误，请重试。';
                    aiSpan.className = 'error';
                }
            }
        );
    } catch (error) {
        console.error('发送消息失败:', error);
        const aiSpan = aiMessage.querySelector('span');
        if (aiSpan) {
            aiSpan.textContent = '抱歉，发送消息失败，请重试。';
            aiSpan.className = 'error';
        }
    }
}

// 处理模态框聊天输入框回车事件
function handleModalChatKeyPress(event) {
    if (event.key === 'Enter') {
        sendModalMessage();
    }
}

// 从列表分析简历
async function analyzeResumeFromList(resumeId) {
    console.log('开始分析简历:', resumeId);
    
    // 检查resumeId是否有效
    if (!resumeId || resumeId === 'null' || resumeId === 'undefined') {
        console.error('无效的resumeId:', resumeId);
        showErrorMessage('无效的简历ID，无法进行分析');
        return;
    }
    
    try {
        showSuccess('开始分析简历...');
        const result = await analyzeResume(resumeId);
        console.log('分析结果:', result);
        showSuccess('分析完成！');
        
        // 刷新列表
        await loadResumeList();
        
    } catch (error) {
        console.error('分析失败:', error);
        showErrorMessage('分析失败: ' + error.message);
    }
}

// 确认删除简历
function confirmDeleteResume(resumeId, candidateName) {
    // 检查resumeId是否有效
    if (!resumeId || resumeId === 'null' || resumeId === 'undefined') {
        console.error('无效的resumeId:', resumeId);
        showErrorMessage('无效的简历ID，无法删除');
        return;
    }
    
    const message = `确定要删除 "${candidateName || '未命名候选人'}" 的简历吗？此操作不可恢复。`;
    
    confirmAction(message, async () => {
        try {
            await deleteResume(resumeId);
            showSuccess('简历删除成功');
            await loadResumeList();
        } catch (error) {
            console.error('删除失败:', error);
            showErrorMessage('删除失败: ' + error.message);
        }
    });
}

// 初始化筛选器
function initializeFilters() {
    const searchInput = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter');
    const sortBy = document.getElementById('sortBy');
    
    // 防抖搜索
    const debouncedFilter = debounce(filterResumes, 300);
    searchInput.addEventListener('input', debouncedFilter);
    statusFilter.addEventListener('change', filterResumes);
    sortBy.addEventListener('change', sortResumes);
}

// 筛选简历
function filterResumes() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const statusFilter = document.getElementById('statusFilter').value;
    
    filteredResumes = allResumes.filter(resume => {
        const matchesSearch = !searchTerm || 
            (resume.candidateName && resume.candidateName.toLowerCase().includes(searchTerm)) ||
            (resume.originalFileName && resume.originalFileName.toLowerCase().includes(searchTerm));
        
        const matchesStatus = !statusFilter || resume.status === statusFilter;
        
        return matchesSearch && matchesStatus;
    });
    
    currentPage = 1;
    renderResumeGrid();
    updatePagination();
}

// 排序简历
function sortResumes() {
    const sortBy = document.getElementById('sortBy').value;
    
    filteredResumes.sort((a, b) => {
        switch (sortBy) {
            case 'candidateName':
                return (a.candidateName || '').localeCompare(b.candidateName || '');
            case 'status':
                return (a.status || '').localeCompare(b.status || '');
            case 'uploadTime':
            default:
                return new Date(b.uploadTime || 0) - new Date(a.uploadTime || 0);
        }
    });
    
    renderResumeGrid();
}

// 更新分页
function updatePagination() {
    const totalPages = Math.ceil(filteredResumes.length / itemsPerPage);
    const paginationSection = document.getElementById('paginationSection');
    const pageInfo = document.getElementById('pageInfo');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    
    if (totalPages <= 1) {
        paginationSection.style.display = 'none';
        return;
    }
    
    paginationSection.style.display = 'block';
    pageInfo.textContent = `第 ${currentPage} 页，共 ${totalPages} 页`;
    
    prevBtn.disabled = currentPage <= 1;
    nextBtn.disabled = currentPage >= totalPages;
}

// 切换页面
function changePage(direction) {
    const totalPages = Math.ceil(filteredResumes.length / itemsPerPage);
    const newPage = currentPage + direction;
    
    if (newPage >= 1 && newPage <= totalPages) {
        currentPage = newPage;
        renderResumeGrid();
        updatePagination();
        
        // 滚动到顶部
        document.querySelector('.main-content').scrollIntoView({ behavior: 'smooth' });
    }
}

// 刷新列表
function refreshList() {
    loadResumeList();
}

// 关闭模态框
function closeModal() {
    console.log('关闭模态框 - 当前currentResumeId:', currentResumeId);
    const modal = document.getElementById('resumeModal');
    modal.style.display = 'none';
    // 注意：不要在这里清空currentResumeId，因为analyzeFromModal可能需要它
    // currentResumeId = null;
}

// 完全关闭模态框并清理状态
function closeModalCompletely() {
    console.log('完全关闭模态框并清理状态');
    const modal = document.getElementById('resumeModal');
    modal.style.display = 'none';
    currentResumeId = null;
}

// 关闭聊天模态框
function closeChatModal() {
    const chatModal = document.getElementById('chatModal');
    chatModal.style.display = 'none';
    currentResumeId = null;
}

// 从模态框分析
async function analyzeFromModal() {
    console.log('从模态框开始分析 - currentResumeId:', currentResumeId);
    
    // 检查currentResumeId是否有效
    if (!currentResumeId || currentResumeId === 'null' || currentResumeId === 'undefined') {
        console.error('模态框分析失败 - currentResumeId无效:', currentResumeId);
        showErrorMessage('无效的简历ID，无法进行分析。请关闭模态框重新打开。');
        return;
    }
    
    // 保存resumeId副本
    const resumeIdToAnalyze = currentResumeId;
    
    try {
        closeModal();
        await analyzeResumeFromList(resumeIdToAnalyze);
        // 分析成功后清理状态
        currentResumeId = null;
    } catch (error) {
        console.error('模态框分析失败:', error);
        showErrorMessage('分析失败: ' + error.message);
        // 失败也清理状态
        currentResumeId = null;
    }
}

// 从模态框删除
async function deleteFromModal() {
    if (!currentResumeId) return;
    
    const resume = allResumes.find(r => r.resumeId === currentResumeId);
    if (resume) {
        closeModal();
        confirmDeleteResume(currentResumeId, resume.candidateName);
    }
}

// 点击模态框外部关闭
window.addEventListener('click', function(event) {
    const resumeModal = document.getElementById('resumeModal');
    const chatModal = document.getElementById('chatModal');
    
    if (event.target === resumeModal) {
        closeModalCompletely();
    }
    if (event.target === chatModal) {
        closeChatModal();
    }
});

// 导出函数供HTML调用
window.loadResumeList = loadResumeList;
window.refreshList = refreshList;
window.filterResumes = filterResumes;
window.sortResumes = sortResumes;
window.changePage = changePage;
window.viewResumeDetail = viewResumeDetail;
window.openChatModal = openChatModal;
window.sendModalMessage = sendModalMessage;
window.handleModalChatKeyPress = handleModalChatKeyPress;
window.analyzeResumeFromList = analyzeResumeFromList;
window.confirmDeleteResume = confirmDeleteResume;
window.closeModal = closeModal;
window.closeChatModal = closeChatModal;
window.analyzeFromModal = analyzeFromModal;
window.deleteFromModal = deleteFromModal;
window.getSummaryInModal = getSummaryInModal;
window.getAnalysisInModal = getAnalysisInModal;

// 调试函数 - 检查简历数据的完整性
window.debugResumeData = function() {
    console.log('=== 简历数据调试信息 ===');
    console.log('总简历数:', allResumes.length);
    console.log('过滤后简历数:', filteredResumes.length);
    
    allResumes.forEach((resume, index) => {
        console.log(`简历 ${index + 1}:`, {
            resumeId: resume.resumeId,
            resumeIdType: typeof resume.resumeId,
            candidateName: resume.candidateName,
            status: resume.status,
            isValidId: !!(resume.resumeId && resume.resumeId !== 'null' && resume.resumeId !== 'undefined')
        });
    });
    
    console.log('=== 调试信息结束 ===');
};

// 自动在控制台提供调试命令
console.log('调试提示: 输入 debugResumeData() 查看简历数据状态');

// =============================================================================
// 简历岗位匹配功能
// =============================================================================

/**
 * 打开岗位匹配模态框
 */
function openJobMatchingModal(resumeId, candidateName) {
    console.log('打开岗位匹配模态框 - resumeId:', resumeId, 'candidateName:', candidateName);
    
    if (!resumeId || resumeId === 'null' || resumeId === 'undefined') {
        console.error('无效的resumeId:', resumeId);
        showErrorMessage('无效的简历ID');
        return;
    }
    
    currentResumeId = resumeId;
    
    // 创建模态框HTML
    const modalHtml = `
        <div id="jobMatchingModal" class="modal fade-in">
            <div class="modal-content">
                <div class="modal-header">
                    <h3><i class="fas fa-bullseye"></i> 岗位匹配分析</h3>
                    <span class="close" onclick="closeJobMatchingModal()">&times;</span>
                </div>
                
                <div class="modal-body">
                    <!-- 候选人信息 -->
                    <div class="candidate-info">
                        <h4><i class="fas fa-user"></i> 候选人信息</h4>
                        <p><strong>姓名:</strong> ${candidateName || '未知'}</p>
                        <p><strong>简历ID:</strong> ${resumeId.substring(0, 8)}...</p>
                    </div>
                    
                    <!-- 匹配模式选择 -->
                    <div class="match-mode-selector">
                        <h4><i class="fas fa-cogs"></i> 匹配模式</h4>
                        <div class="radio-group">
                            <label>
                                <input type="radio" name="matchMode" value="single" checked>
                                <span>单岗位匹配</span>
                            </label>
                            <label>
                                <input type="radio" name="matchMode" value="recommend">
                                <span>智能推荐</span>
                            </label>
                            <label>
                                <input type="radio" name="matchMode" value="batch">
                                <span>批量匹配</span>
                            </label>
                        </div>
                    </div>
                    
                    <!-- 单岗位匹配 -->
                    <div id="singleMatchSection" class="match-section">
                        <h4><i class="fas fa-search"></i> 选择岗位</h4>
                        <select id="singleJobSelect" class="form-control">
                            <option value="">请选择岗位...</option>
                        </select>
                        <button class="btn-primary" onclick="performSingleMatch()">开始匹配</button>
                    </div>
                    
                    <!-- 智能推荐 -->
                    <div id="recommendSection" class="match-section" style="display: none;">
                        <h4><i class="fas fa-magic"></i> 推荐设置</h4>
                        <div class="form-row">
                            <div class="form-group">
                                <label>推荐数量:</label>
                                <select id="recommendTopK" class="form-control">
                                    <option value="3">前3个</option>
                                    <option value="5" selected>前5个</option>
                                    <option value="10">前10个</option>
                                </select>
                            </div>
                            <div class="form-group">
                                <label>最低匹配分数:</label>
                                <select id="recommendMinScore" class="form-control">
                                    <option value="30">30分以上</option>
                                    <option value="50" selected>50分以上</option>
                                    <option value="70">70分以上</option>
                                </select>
                            </div>
                        </div>
                        <div class="form-row">
                            <div class="form-group">
                                <label>期望地点 (可选):</label>
                                <input type="text" id="recommendLocation" class="form-control" placeholder="如: 北京, 上海">
                            </div>
                            <div class="form-group">
                                <label>期望公司 (可选):</label>
                                <input type="text" id="recommendCompany" class="form-control" placeholder="如: 阿里巴巴, 腾讯">
                            </div>
                        </div>
                        <button class="btn-primary" onclick="performRecommendation()">智能推荐</button>
                    </div>
                    
                    <!-- 批量匹配 -->
                    <div id="batchMatchSection" class="match-section" style="display: none;">
                        <h4><i class="fas fa-list"></i> 选择多个岗位</h4>
                        <div class="job-selection-grid" id="jobSelectionGrid">
                            <!-- 岗位选择列表将在这里生成 -->
                        </div>
                        <button class="btn-primary" onclick="performBatchMatch()">批量匹配</button>
                    </div>
                    
                    <!-- 匹配结果 -->
                    <div id="matchingResults" class="matching-results" style="display: none;">
                        <h4><i class="fas fa-chart-bar"></i> 匹配结果</h4>
                        <div id="resultsList"></div>
                    </div>
                    
                    <!-- 加载状态 -->
                    <div id="matchingLoading" class="loading-state" style="display: none;">
                        <i class="fas fa-spinner fa-spin"></i>
                        <p>正在分析匹配度，请稍候...</p>
                    </div>
                </div>
                
                <div class="modal-footer">
                    <button class="btn-secondary" onclick="closeJobMatchingModal()">关闭</button>
                </div>
            </div>
        </div>
    `;
    
    // 添加到页面
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    // 初始化模态框
    initializeJobMatchingModal();
}

/**
 * 初始化岗位匹配模态框
 */
async function initializeJobMatchingModal() {
    try {
        // 加载岗位列表
        const jobs = await apiCall(`${API_BASE}/jobs`);
        populateJobSelections(jobs);
        
        // 设置模式切换事件
        setupMatchModeHandlers();
        
    } catch (error) {
        console.error('初始化岗位匹配模态框失败:', error);
        showErrorMessage('加载岗位列表失败: ' + error.message);
    }
}

/**
 * 填充岗位选择列表
 */
function populateJobSelections(jobs) {
    // 填充单选下拉框
    const singleSelect = document.getElementById('singleJobSelect');
    singleSelect.innerHTML = '<option value="">请选择岗位...</option>';
    
    jobs.forEach(job => {
        const option = document.createElement('option');
        option.value = job.jobId;
        option.textContent = `${job.title} - ${job.company}`;
        singleSelect.appendChild(option);
    });
    
    // 填充批量选择网格
    const grid = document.getElementById('jobSelectionGrid');
    grid.innerHTML = jobs.map(job => `
        <label class="job-checkbox-item">
            <input type="checkbox" name="batchJobs" value="${job.jobId}">
            <div class="job-info">
                <strong>${job.title}</strong>
                <span class="company">${job.company}</span>
                <span class="location">${job.location || ''}</span>
            </div>
        </label>
    `).join('');
}

/**
 * 设置匹配模式切换处理
 */
function setupMatchModeHandlers() {
    const modeRadios = document.querySelectorAll('input[name="matchMode"]');
    modeRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            // 隐藏所有部分
            document.getElementById('singleMatchSection').style.display = 'none';
            document.getElementById('recommendSection').style.display = 'none';
            document.getElementById('batchMatchSection').style.display = 'none';
            
            // 显示选中的部分
            if (this.value === 'single') {
                document.getElementById('singleMatchSection').style.display = 'block';
            } else if (this.value === 'recommend') {
                document.getElementById('recommendSection').style.display = 'block';
            } else if (this.value === 'batch') {
                document.getElementById('batchMatchSection').style.display = 'block';
            }
        });
    });
}

/**
 * 执行单岗位匹配
 */
async function performSingleMatch() {
    const jobId = document.getElementById('singleJobSelect').value;
    if (!jobId) {
        showErrorMessage('请先选择一个岗位');
        return;
    }
    
    try {
        showMatchingLoading(true);
        
        const result = await apiCall(`${API_BASE}/match/${currentResumeId}/${jobId}`, 'POST');
        displayMatchingResults([result], 'single');
        
    } catch (error) {
        console.error('单岗位匹配失败:', error);
        showErrorMessage('匹配失败: ' + error.message);
    } finally {
        showMatchingLoading(false);
    }
}

/**
 * 执行智能推荐
 */
async function performRecommendation() {
    try {
        showMatchingLoading(true);
        
        const topK = document.getElementById('recommendTopK').value;
        const minScore = document.getElementById('recommendMinScore').value;
        const location = document.getElementById('recommendLocation').value;
        const company = document.getElementById('recommendCompany').value;
        
        const params = new URLSearchParams({
            topK: topK,
            minScore: minScore
        });
        
        if (location) params.append('location', location);
        if (company) params.append('company', company);
        
        const results = await apiCall(`${API_BASE}/recommend/${currentResumeId}/filtered?${params}`);
        displayMatchingResults(results, 'recommend');
        
    } catch (error) {
        console.error('智能推荐失败:', error);
        showErrorMessage('推荐失败: ' + error.message);
    } finally {
        showMatchingLoading(false);
    }
}

/**
 * 执行批量匹配
 */
async function performBatchMatch() {
    const selectedJobs = Array.from(document.querySelectorAll('input[name="batchJobs"]:checked'))
        .map(checkbox => checkbox.value);
        
    if (selectedJobs.length === 0) {
        showErrorMessage('请至少选择一个岗位');
        return;
    }
    
    try {
        showMatchingLoading(true);
        
        const results = await apiCall(`${API_BASE}/match/${currentResumeId}/batch`, 'POST', selectedJobs);
        displayMatchingResults(results, 'batch');
        
    } catch (error) {
        console.error('批量匹配失败:', error);
        showErrorMessage('批量匹配失败: ' + error.message);
    } finally {
        showMatchingLoading(false);
    }
}

/**
 * 显示匹配结果
 */
function displayMatchingResults(results, mode) {
    const resultsContainer = document.getElementById('matchingResults');
    const resultsList = document.getElementById('resultsList');
    
    if (!results || results.length === 0) {
        resultsList.innerHTML = `
            <div class="no-results">
                <i class="fas fa-search"></i>
                <p>没有找到匹配的岗位</p>
            </div>
        `;
    } else {
        resultsList.innerHTML = results.map(result => createMatchingResultCard(result)).join('');
    }
    
    resultsContainer.style.display = 'block';
    
    // 滚动到结果区域
    resultsContainer.scrollIntoView({ behavior: 'smooth' });
}

/**
 * 创建匹配结果卡片
 */
function createMatchingResultCard(result) {
    const scoreClass = getMatchScoreClass(result.matchScore);
    const levelText = getMatchLevelText(result.matchLevel);
    
    return `
        <div class="matching-result-card">
            <div class="result-header">
                <div class="result-title">
                    <h5>岗位匹配分析</h5>
                    <span class="match-score ${scoreClass}">${result.matchScore.toFixed(1)}分</span>
                </div>
                <span class="match-level">${levelText}</span>
            </div>
            
            <div class="result-summary">
                <p><strong>匹配总结:</strong></p>
                <p class="summary-text">${result.summary || '正在分析中...'}</p>
            </div>
            
            ${result.advantages && result.advantages.length > 0 ? `
                <div class="result-advantages">
                    <p><strong><i class="fas fa-thumbs-up"></i> 优势亮点:</strong></p>
                    <ul>
                        ${result.advantages.map(adv => `<li>${adv}</li>`).join('')}
                    </ul>
                </div>
            ` : ''}
            
            ${result.gaps && result.gaps.length > 0 ? `
                <div class="result-gaps">
                    <p><strong><i class="fas fa-exclamation-triangle"></i> 能力差距:</strong></p>
                    <ul>
                        ${result.gaps.map(gap => `<li>${gap}</li>`).join('')}
                    </ul>
                </div>
            ` : ''}
            
            ${result.recommendations && result.recommendations.length > 0 ? `
                <div class="result-recommendations">
                    <p><strong><i class="fas fa-lightbulb"></i> 发展建议:</strong></p>
                    <ul>
                        ${result.recommendations.map(rec => `<li>${rec}</li>`).join('')}
                    </ul>
                </div>
            ` : ''}
            
            <div class="result-actions">
                <button class="btn-secondary btn-small" onclick="viewJobDetail('${result.jobId}')">
                    <i class="fas fa-info-circle"></i> 岗位详情
                </button>
                <button class="btn-primary btn-small" onclick="exportMatchingResult('${result.resumeId}', '${result.jobId}')">
                    <i class="fas fa-download"></i> 导出报告
                </button>
            </div>
        </div>
    `;
}

/**
 * 获取匹配分数样式类
 */
function getMatchScoreClass(score) {
    if (score >= 85) return 'score-excellent';
    if (score >= 70) return 'score-good';
    if (score >= 50) return 'score-fair';
    return 'score-poor';
}

/**
 * 获取匹配等级文本
 */
function getMatchLevelText(level) {
    const levelMap = {
        'EXCELLENT': '优秀匹配',
        'GOOD': '良好匹配',
        'FAIR': '一般匹配',
        'POOR': '匹配度较低'
    };
    return levelMap[level] || '未知';
}

/**
 * 显示/隐藏匹配加载状态
 */
function showMatchingLoading(show) {
    const loadingElement = document.getElementById('matchingLoading');
    const resultsElement = document.getElementById('matchingResults');
    
    if (show) {
        loadingElement.style.display = 'block';
        resultsElement.style.display = 'none';
    } else {
        loadingElement.style.display = 'none';
    }
}

/**
 * 查看岗位详情
 */
async function viewJobDetail(jobId) {
    try {
        const job = await apiCall(`${API_BASE}/jobs/${jobId}`);
        
        const detailHtml = `
            <div class="job-detail-modal">
                <h4>${job.title}</h4>
                <p><strong>公司:</strong> ${job.company}</p>
                <p><strong>地点:</strong> ${job.location || '未指定'}</p>
                <p><strong>薪资:</strong> ${job.salaryRange || '面议'}</p>
                <p><strong>经验要求:</strong> ${job.experience || '不限'}</p>
                <p><strong>学历要求:</strong> ${job.education || '不限'}</p>
                
                ${job.description ? `<p><strong>岗位描述:</strong><br>${job.description}</p>` : ''}
                
                ${job.requirements && job.requirements.length > 0 ? `
                    <div><strong>任职要求:</strong>
                        <ul>${job.requirements.map(req => `<li>${req}</li>`).join('')}</ul>
                    </div>
                ` : ''}
                
                ${job.skills && job.skills.length > 0 ? `
                    <div><strong>技能要求:</strong>
                        <ul>${job.skills.map(skill => `<li>${skill}</li>`).join('')}</ul>
                    </div>
                ` : ''}
            </div>
        `;
        
        showInfoModal('岗位详情', detailHtml);
        
    } catch (error) {
        console.error('获取岗位详情失败:', error);
        showErrorMessage('获取岗位详情失败: ' + error.message);
    }
}

/**
 * 导出匹配报告
 */
function exportMatchingResult(resumeId, jobId) {
    // 这里可以实现PDF导出或其他格式的报告导出
    showInfoMessage('报告导出功能正在开发中...');
}

/**
 * 关闭岗位匹配模态框
 */
function closeJobMatchingModal() {
    const modal = document.getElementById('jobMatchingModal');
    if (modal) {
        modal.remove();
    }
    currentResumeId = null;
}

/**
 * 显示信息模态框
 */
function showInfoModal(title, content) {
    const modalHtml = `
        <div id="infoModal" class="modal fade-in">
            <div class="modal-content">
                <div class="modal-header">
                    <h3>${title}</h3>
                    <span class="close" onclick="closeInfoModal()">&times;</span>
                </div>
                <div class="modal-body">
                    ${content}
                </div>
                <div class="modal-footer">
                    <button class="btn-secondary" onclick="closeInfoModal()">关闭</button>
                </div>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

/**
 * 关闭信息模态框
 */
function closeInfoModal() {
    const modal = document.getElementById('infoModal');
    if (modal) {
        modal.remove();
    }
}