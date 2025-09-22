// 岗位管理页面功能脚本

let allJobs = [];
let filteredJobs = [];
let currentPage = 1;
const itemsPerPage = 12;
let currentJobId = null;
let isEditMode = false;

// 页面初始化
document.addEventListener('DOMContentLoaded', function() {
    loadJobList();
    initializeFilters();
});

// 加载岗位列表
async function loadJobList() {
    const loadingState = document.getElementById('loadingState');
    const emptyState = document.getElementById('emptyState');
    const jobGrid = document.getElementById('jobGrid');
    
    try {
        loadingState.style.display = 'block';
        emptyState.style.display = 'none';
        jobGrid.innerHTML = '';
        
        const jobs = await getJobList();
        allJobs = jobs;
        filteredJobs = jobs;
        
        loadingState.style.display = 'none';
        
        if (jobs.length === 0) {
            emptyState.style.display = 'block';
        } else {
            updateStatistics();
            renderJobGrid();
            updatePagination();
        }
        
    } catch (error) {
        console.error('加载岗位列表失败:', error);
        loadingState.style.display = 'none';
        showError(jobGrid, '加载失败，请刷新页面重试');
    }
}

// 更新统计信息
function updateStatistics() {
    const totalJobs = document.getElementById('totalJobs');
    const activeJobs = document.getElementById('activeJobs');
    const newJobs = document.getElementById('newJobs');
    
    totalJobs.textContent = allJobs.length;
    activeJobs.textContent = allJobs.filter(j => j.status === 'ACTIVE').length;
    
    // 计算本月新增（简化计算）
    const currentMonth = new Date().getMonth();
    const thisMonthJobs = allJobs.filter(job => {
        if (job.createTime) {
            const jobMonth = new Date(job.createTime).getMonth();
            return jobMonth === currentMonth;
        }
        return false;
    });
    newJobs.textContent = thisMonthJobs.length;
}

// 渲染岗位网格
function renderJobGrid() {
    const jobGrid = document.getElementById('jobGrid');
    const startIndex = (currentPage - 1) * itemsPerPage;
    const endIndex = startIndex + itemsPerPage;
    const pageJobs = filteredJobs.slice(startIndex, endIndex);
    
    if (pageJobs.length === 0) {
        jobGrid.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-search"></i>
                <h3>没有找到匹配的岗位</h3>
                <p>请尝试调整筛选条件</p>
            </div>
        `;
        return;
    }
    
    jobGrid.innerHTML = pageJobs.map(job => createJobCard(job)).join('');
}

// 创建岗位卡片
function createJobCard(job) {
    const statusText = getJobStatusText(job.status);
    const statusClass = getJobStatusClass(job.status);
    const createTime = formatDate(job.createTime);
    
    return `
        <div class="job-card fade-in">
            <div class="job-header">
                <div>
                    <h3 class="job-title">${job.title || '未命名岗位'}</h3>
                    <div class="job-company">${job.company || '未知公司'}</div>
                </div>
                <span class="job-status ${statusClass}">${statusText}</span>
            </div>
            
            <div class="job-info">
                <p><i class="fas fa-map-marker-alt"></i> ${job.location || '地点待定'}</p>
                <p><i class="fas fa-clock"></i> ${job.experience || '经验不限'}</p>
                <p><i class="fas fa-graduation-cap"></i> ${job.education || '学历不限'}</p>
                <p><i class="fas fa-money-bill-wave"></i> ${job.salaryRange || '薪资面议'}</p>
                <p><i class="fas fa-briefcase"></i> ${getEmploymentTypeText(job.employmentType)}</p>
                <p><i class="fas fa-calendar"></i> ${createTime}</p>
            </div>
            
            ${job.description ? `
                <div class="job-description">${job.description}</div>
            ` : ''}
            
            ${job.skills && job.skills.length > 0 ? `
                <div class="job-tags">
                    ${job.skills.slice(0, 5).map(skill => `<span class="job-tag">${skill}</span>`).join('')}
                    ${job.skills.length > 5 ? '<span class="job-tag">...</span>' : ''}
                </div>
            ` : ''}
            
            <div class="job-actions">
                <button class="btn-primary btn-small" onclick="viewJobDetail('${job.jobId}')">
                    <i class="fas fa-eye"></i> 查看
                </button>
                <button class="btn-secondary btn-small" onclick="editJob('${job.jobId}')">
                    <i class="fas fa-edit"></i> 编辑
                </button>
                <button class="btn-secondary btn-small" onclick="findMatchingResumes('${job.jobId}')">
                    <i class="fas fa-users"></i> 匹配简历
                </button>
                <button class="btn-danger btn-small" onclick="confirmDeleteJob('${job.jobId}', '${job.title}')">
                    <i class="fas fa-trash"></i> 删除
                </button>
            </div>
        </div>
    `;
}

// 获取岗位状态文本
function getJobStatusText(status) {
    const statusMap = {
        'ACTIVE': '活跃',
        'INACTIVE': '暂停',
        'CLOSED': '关闭'
    };
    return statusMap[status] || '未知';
}

// 获取岗位状态样式类
function getJobStatusClass(status) {
    const classMap = {
        'ACTIVE': 'active',
        'INACTIVE': 'inactive',
        'CLOSED': 'closed'
    };
    return classMap[status] || '';
}

// 获取工作类型文本
function getEmploymentTypeText(type) {
    const typeMap = {
        'FULL_TIME': '全职',
        'PART_TIME': '兼职',
        'CONTRACT': '合同工',
        'INTERN': '实习'
    };
    return typeMap[type] || '全职';
}

// 查看岗位详情
function viewJobDetail(jobId) {
    const job = allJobs.find(j => j.jobId === jobId);
    if (!job) return;
    
    openJobModal(job, false); // 只读模式
}

// 编辑岗位
function editJob(jobId) {
    const job = allJobs.find(j => j.jobId === jobId);
    if (!job) return;
    
    openJobModal(job, true); // 编辑模式
}

// 删除岗位确认
function confirmDeleteJob(jobId, jobTitle) {
    const message = `确定要删除岗位 "${jobTitle}" 吗？此操作不可恢复。`;
    
    confirmAction(message, async () => {
        try {
            await deleteJob(jobId);
            showSuccess('岗位删除成功');
            await loadJobList();
        } catch (error) {
            console.error('删除失败:', error);
            showErrorMessage('删除失败: ' + error.message);
        }
    });
}

// 查找匹配的简历
async function findMatchingResumes(jobId) {
    try {
        showSuccess('正在查找匹配的简历...');
        
        // 这里可以实现跳转到简历匹配页面或显示匹配结果
        // 暂时显示提示信息
        showSuccess('匹配功能开发中，敬请期待！');
        
    } catch (error) {
        console.error('查找匹配简历失败:', error);
        showErrorMessage('查找匹配简历失败: ' + error.message);
    }
}

// 打开岗位模态框
function openJobModal(job = null, editMode = true) {
    const modal = document.getElementById('jobModal');
    const modalTitle = document.getElementById('modalTitle');
    const form = document.getElementById('jobForm');
    
    isEditMode = editMode;
    currentJobId = job ? job.jobId : null;
    
    // 设置标题
    if (job) {
        modalTitle.textContent = editMode ? '编辑岗位' : '岗位详情';
    } else {
        modalTitle.textContent = '添加岗位';
    }
    
    // 清空表单
    form.reset();
    
    // 如果是查看或编辑现有岗位，填充数据
    if (job) {
        fillJobForm(job);
    }
    
    // 设置表单为只读或可编辑
    setFormReadonly(!editMode);
    
    modal.style.display = 'block';
}

// 添加岗位模态框
function openAddJobModal() {
    openJobModal(null, true);
}

// 关闭岗位模态框
function closeJobModal() {
    const modal = document.getElementById('jobModal');
    modal.style.display = 'none';
    currentJobId = null;
    isEditMode = false;
}

// 填充岗位表单
function fillJobForm(job) {
    document.getElementById('jobTitle').value = job.title || '';
    document.getElementById('jobCompany').value = job.company || '';
    document.getElementById('jobDepartment').value = job.department || '';
    document.getElementById('jobLocation').value = job.location || '';
    document.getElementById('jobEmploymentType').value = job.employmentType || 'FULL_TIME';
    document.getElementById('jobExperience').value = job.experience || '';
    document.getElementById('jobEducation').value = job.education || '';
    document.getElementById('jobSalaryRange').value = job.salaryRange || '';
    document.getElementById('jobStatus').value = job.status || 'ACTIVE';
    document.getElementById('jobDescription').value = job.description || '';
    
    // 填充列表项
    fillListContainer('responsibilitiesContainer', job.responsibilities || []);
    fillListContainer('requirementsContainer', job.requirements || []);
    fillListContainer('skillsContainer', job.skills || []);
    fillListContainer('benefitsContainer', job.benefits || []);
}

// 填充列表容器
function fillListContainer(containerId, items) {
    const container = document.getElementById(containerId);
    container.innerHTML = '';
    
    if (items.length === 0) {
        addListItem(containerId);
    } else {
        items.forEach(item => {
            const listItem = createListItem(item);
            container.appendChild(listItem);
        });
    }
}

// 创建列表项元素
function createListItem(value = '') {
    const div = document.createElement('div');
    div.className = 'list-item';
    div.innerHTML = `
        <input type="text" value="${value}" placeholder="输入内容...">
        <button type="button" class="remove-item" onclick="removeListItem(this)">删除</button>
    `;
    return div;
}

// 添加列表项
function addListItem(containerId) {
    const container = document.getElementById(containerId);
    const listItem = createListItem();
    container.appendChild(listItem);
}

// 删除列表项
function removeListItem(button) {
    const container = button.parentElement.parentElement;
    button.parentElement.remove();
    
    // 确保至少有一个输入框
    if (container.children.length === 0) {
        const containerId = container.id;
        addListItem(containerId);
    }
}

// 设置表单只读状态
function setFormReadonly(readonly) {
    const form = document.getElementById('jobForm');
    const inputs = form.querySelectorAll('input, select, textarea');
    const buttons = form.querySelectorAll('button[type="button"]');
    const submitBtn = form.querySelector('button[type="submit"]');
    
    inputs.forEach(input => {
        input.readOnly = readonly;
        input.disabled = readonly;
    });
    
    buttons.forEach(btn => {
        btn.style.display = readonly ? 'none' : 'inline-block';
    });
    
    if (submitBtn) {
        submitBtn.style.display = readonly ? 'none' : 'inline-block';
    }
}

// 从列表容器获取值
function getListValues(containerId) {
    const container = document.getElementById(containerId);
    const inputs = container.querySelectorAll('input');
    return Array.from(inputs)
        .map(input => input.value.trim())
        .filter(value => value.length > 0);
}

// 表单提交处理
document.getElementById('jobForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    if (!isEditMode) return;
    
    try {
        const formData = new FormData(this);
        const jobData = {
            title: formData.get('title'),
            company: formData.get('company'),
            department: formData.get('department'),
            location: formData.get('location'),
            employmentType: formData.get('employmentType'),
            experience: formData.get('experience'),
            education: formData.get('education'),
            salaryRange: formData.get('salaryRange'),
            status: formData.get('status'),
            description: formData.get('description'),
            responsibilities: getListValues('responsibilitiesContainer'),
            requirements: getListValues('requirementsContainer'),
            skills: getListValues('skillsContainer'),
            benefits: getListValues('benefitsContainer')
        };
        
        let result;
        if (currentJobId) {
            // 更新岗位
            result = await updateJob(currentJobId, jobData);
            showSuccess('岗位更新成功');
        } else {
            // 添加新岗位
            result = await addJob(jobData);
            showSuccess('岗位添加成功');
        }
        
        closeJobModal();
        await loadJobList();
        
    } catch (error) {
        console.error('保存岗位失败:', error);
        showErrorMessage('保存失败: ' + error.message);
    }
});

// 初始化筛选器
function initializeFilters() {
    const searchInput = document.getElementById('searchInput');
    const statusFilter = document.getElementById('statusFilter');
    const locationFilter = document.getElementById('locationFilter');
    const sortBy = document.getElementById('sortBy');
    
    // 防抖搜索
    const debouncedFilter = debounce(filterJobs, 300);
    searchInput.addEventListener('input', debouncedFilter);
    statusFilter.addEventListener('change', filterJobs);
    locationFilter.addEventListener('change', filterJobs);
    sortBy.addEventListener('change', sortJobs);
}

// 筛选岗位
function filterJobs() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const statusFilter = document.getElementById('statusFilter').value;
    const locationFilter = document.getElementById('locationFilter').value;
    
    filteredJobs = allJobs.filter(job => {
        const matchesSearch = !searchTerm || 
            (job.title && job.title.toLowerCase().includes(searchTerm)) ||
            (job.company && job.company.toLowerCase().includes(searchTerm));
        
        const matchesStatus = !statusFilter || job.status === statusFilter;
        const matchesLocation = !locationFilter || job.location === locationFilter;
        
        return matchesSearch && matchesStatus && matchesLocation;
    });
    
    currentPage = 1;
    renderJobGrid();
    updatePagination();
}

// 排序岗位
function sortJobs() {
    const sortBy = document.getElementById('sortBy').value;
    
    filteredJobs.sort((a, b) => {
        switch (sortBy) {
            case 'title':
                return (a.title || '').localeCompare(b.title || '');
            case 'company':
                return (a.company || '').localeCompare(b.company || '');
            case 'updateTime':
                return new Date(b.updateTime || 0) - new Date(a.updateTime || 0);
            case 'createTime':
            default:
                return new Date(b.createTime || 0) - new Date(a.createTime || 0);
        }
    });
    
    renderJobGrid();
}

// 更新分页
function updatePagination() {
    const totalPages = Math.ceil(filteredJobs.length / itemsPerPage);
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
    const totalPages = Math.ceil(filteredJobs.length / itemsPerPage);
    const newPage = currentPage + direction;
    
    if (newPage >= 1 && newPage <= totalPages) {
        currentPage = newPage;
        renderJobGrid();
        updatePagination();
        
        // 滚动到顶部
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

// API调用函数

// 获取岗位列表
async function getJobList() {
    return await apiCall(`${API_BASE}/jobs`);
}

// 获取岗位详情
async function getJobById(jobId) {
    return await apiCall(`${API_BASE}/jobs/${jobId}`);
}

// 添加岗位
async function addJob(jobData) {
    return await apiCall(`${API_BASE}/jobs`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(jobData)
    });
}

// 更新岗位
async function updateJob(jobId, jobData) {
    return await apiCall(`${API_BASE}/jobs/${jobId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(jobData)
    });
}

// 删除岗位
async function deleteJob(jobId) {
    return await apiCall(`${API_BASE}/jobs/${jobId}`, {
        method: 'DELETE'
    });
}

// 点击模态框外部关闭
window.onclick = function(event) {
    const modal = document.getElementById('jobModal');
    if (event.target === modal) {
        closeJobModal();
    }
}