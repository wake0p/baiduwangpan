<template>
    <div class="user-management-container">
        <!-- 顶部工具栏 -->
        <div class="toolbar">
            <div class="toolbar-left">
                <el-button type="primary" @click="handleAddUser">
                    <el-icon><Plus /></el-icon>
                    添加用户
                </el-button>
                <el-button @click="handleBatchDelete" :disabled="!selectedUsers.length" type="danger">
                    <el-icon><Delete /></el-icon>
                    批量删除
                </el-button>
            </div>
            <div class="toolbar-right">
                <el-input
                    v-model="searchKeyword"
                    placeholder="搜索用户"
                    prefix-icon="Search"
                    clearable
                    @input="handleSearch"
                    style="width: 200px; margin-right: 10px;"
                />
                <el-button @click="handleRefresh">
                    <el-icon><Refresh /></el-icon>
                </el-button>
            </div>
        </div>

        <!-- 用户列表 -->
        <div class="user-list-container">
            <el-table
                :data="filteredUserList"
                @selection-change="handleSelectionChange"
                style="width: 100%"
                v-loading="loading"
            >
                <el-table-column type="selection" width="55" />
                <el-table-column label="用户信息" min-width="150">
                    <template #default="{ row }">
                        <div class="user-info">
                            <el-avatar :size="40" :src="row.avatar">
                                {{ row.username.charAt(0).toUpperCase() }}
                            </el-avatar>
                            <div class="user-details">
                                <div class="username">{{ row.username }}</div>
                                <div class="email">{{ row.email }}</div>
                            </div>
                            <el-tag v-if="row.isAdmin" size="small" type="danger">管理员</el-tag>
                            <el-tag v-else size="small" type="success">普通用户</el-tag>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="nickname" label="昵称" width="120" />
                <el-table-column prop="phone" label="手机号" width="130" />
                <el-table-column prop="status" label="状态" width="100">
                    <template #default="{ row }">
                        <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
                            {{ row.status === 'active' ? '正常' : '禁用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="createdTime" label="注册时间" width="180">
                    <template #default="{ row }">
                        {{ formatDate(row.createdTime) }}
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="350" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" @click="handleEditUser(row)">编辑</el-button>
                        <el-button size="small" @click="handleViewPassword(row)">查看密码</el-button>
                        <el-button 
                            size="small" 
                            :type="row.status === 'active' ? 'warning' : 'success'"
                            @click="handleToggleStatus(row)"
                        >
                            {{ row.status === 'active' ? '禁用' : '启用' }}
                        </el-button>
                        <el-button size="small" type="danger" @click="() => handleDeleteUser(row)">删除用户</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 添加/编辑用户对话框 -->
        <el-dialog 
            v-model="userDialogVisible" 
            :title="'设置用户权限'" 
            width="400px"
        >
            <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
                <el-form-item label="用户名" prop="username">
                    <el-input v-model="userForm.username" disabled />
                </el-form-item>
                <el-form-item label="用户角色" prop="isAdmin">
                    <el-radio-group v-model="userForm.isAdmin">
                        <el-radio :label="false">普通用户</el-radio>
                        <el-radio :label="true">管理员</el-radio>
                    </el-radio-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="userDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmUserAction">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 重置密码对话框 -->
        <el-dialog v-model="resetPasswordDialogVisible" title="重置密码" width="400px">
            <el-form :model="resetPasswordForm" :rules="resetPasswordRules" ref="resetPasswordFormRef" label-width="100px">
                <el-form-item label="新密码" prop="newPassword">
                    <el-input v-model="resetPasswordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                    <el-input v-model="resetPasswordForm.confirmPassword" type="password" placeholder="请确认新密码" show-password />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="resetPasswordDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmResetPassword">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 查看密码对话框 -->
        <el-dialog v-model="viewPasswordDialogVisible" title="查看密码" width="400px">
            <div style="font-size: 18px; text-align: center;">
                用户 {{ viewPasswordUser?.username }} 的密码：<br/>
                <span style="font-weight: bold; color: #d32f2f;">{{ viewPasswordUser?.password }}</span>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="viewPasswordDialogVisible = false">关闭</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    Plus,
    Delete,
    Key,
    Search,
    Refresh,
    ArrowDown
} from '@element-plus/icons-vue'
import request, { baseURL } from '@/utils/index'

// 响应式数据
const loading = ref(false)
const searchKeyword = ref('')
const selectedUsers = ref([])
const userDialogVisible = ref(false)
const resetPasswordDialogVisible = ref(false)
const viewPasswordDialogVisible = ref(false)
const isEdit = ref(false)
const userFormRef = ref()
const resetPasswordFormRef = ref()
const viewPasswordUser = ref(null)

// 用户列表数据
const userList = ref([])

// 用户表单
const userForm = reactive({
    username: '',
    nickname: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
    storageLimit: 10,
    isAdmin: false,
    status: 'active'
})

// 重置密码表单
const resetPasswordForm = reactive({
    newPassword: '',
    confirmPassword: ''
})

// 表单验证规则
const userRules = {
    username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
    ],
    nickname: [
        { required: true, message: '请输入昵称', trigger: 'blur' }
    ],
    email: [
        { message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
    ],
    phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        {
            validator: (rule, value, callback) => {
                if (value !== userForm.password) {
                    callback(new Error('两次输入密码不一致'))
                } else {
                    callback()
                }
            },
            trigger: 'blur'
        }
    ]
}

const resetPasswordRules = {
    newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
    ],
    confirmPassword: [
        { required: true, message: '请确认新密码', trigger: 'blur' },
        {
            validator: (rule, value, callback) => {
                if (value !== resetPasswordForm.newPassword) {
                    callback(new Error('两次输入密码不一致'))
                } else {
                    callback()
                }
            },
            trigger: 'blur'
        }
    ]
}

// 计算属性：根据关键词过滤用户
const filteredUserList = computed(() => {
    if (!searchKeyword.value) return userList.value
    return userList.value.filter(user =>
        (user.username && user.username.includes(searchKeyword.value)) ||
        (user.nickname && user.nickname.includes(searchKeyword.value)) ||
        (user.email && user.email.includes(searchKeyword.value)) ||
        (user.phone && user.phone.includes(searchKeyword.value))
    )
})

// 方法
const handleAddUser = () => {
    isEdit.value = false
    resetUserForm()
    userDialogVisible.value = true
}

const handleEditUser = (user) => {
    isEdit.value = true
    Object.assign(userForm, {
        username: user.username,
        isAdmin: user.isAdmin
    })
    userDialogVisible.value = true
}

const handleBatchDelete = () => {
    if (selectedUsers.value.length === 0) {
        ElMessage.warning('请选择要删除的用户')
        return
    }
    
    ElMessageBox.confirm(
        `确定要删除选中的 ${selectedUsers.value.length} 个用户吗？`,
        '确认删除',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(() => {
        ElMessage.success('删除成功')
        loadUserList()
    })
}

const handleBatchResetPassword = () => {
    if (selectedUsers.value.length === 0) {
        ElMessage.warning('请选择要重置密码的用户')
        return
    }
    
    ElMessage.success(`已重置 ${selectedUsers.value.length} 个用户的密码`)
    loadUserList()
}

const handleResetPassword = (user) => {
    resetPasswordForm.newPassword = ''
    resetPasswordForm.confirmPassword = ''
    resetPasswordDialogVisible.value = true
}

const handleToggleStatus = async (user) => {
    const action = user.status === 'active' ? '禁用' : '启用'
    const api = user.status === 'active' ? '/banUser' : '/unbanUser'
    try {
        const res = await request.post(api, { username: user.username })
        if (res.success) {
            ElMessage.success(`已${action}用户: ${user.username}`)
            loadUserList()
        } else {
            ElMessage.error(res.message || `${action}用户失败`)
        }
    } catch (e) {
        ElMessage.error(`${action}用户失败`)
    }
}

const handleSearch = () => {
    // 前端过滤，computed已自动处理
}

const handleRefresh = () => {
    loadUserList()
}

const handleSelectionChange = (selection) => {
    selectedUsers.value = selection
}

const handleDeleteUser = (user) => {
    ElMessageBox.confirm(
        `确定要删除用户 ${user.username} 吗？`,
        '确认删除',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            const res = await request.post('/deleteUser', { username: user.username })
            ElMessage.success('用户已删除')
            loadUserList()
        } catch (e) {
            ElMessage.error('删除用户失败')
        }
    })
}

const confirmUserAction = async () => {
    if (!userFormRef.value) return
    try {
        await userFormRef.value.validate()
        if (isEdit.value) {
            // 只修改权限
            const res = await request.post('/setAdmin', {
                username: userForm.username,
                isAdmin: userForm.isAdmin
            })
            if (res.success) {
                ElMessage.success('用户权限设置成功')
                userDialogVisible.value = false
                loadUserList()
            } else {
                ElMessage.error(res.message || '权限设置失败')
            }
        }
    } catch (error) {
        console.error('表单验证失败:', error)
    }
}

const confirmResetPassword = async () => {
    if (!resetPasswordFormRef.value) return
    
    try {
        await resetPasswordFormRef.value.validate()
        ElMessage.success('密码重置成功')
        resetPasswordDialogVisible.value = false
    } catch (error) {
        console.error('表单验证失败:', error)
    }
}

const resetUserForm = () => {
    Object.assign(userForm, {
        username: '',
        nickname: '',
        email: '',
        phone: '',
        password: '',
        confirmPassword: '',
        storageLimit: 10,
        isAdmin: false,
        status: 'active'
    })
}

const loadUserList = async () => {
    loading.value = true
    try {
        const res = await request.get('/userList')
        // 适配后端返回的userList
        userList.value = (res.userList || []).map(user => {
            // 处理 avatar 字段，保证为完整URL
            let avatar = user.avatar || ''
            if (avatar && !/^https?:\/\//.test(avatar)) {
                const idx = avatar.indexOf('uploads/')
                const purePath = idx !== -1 ? avatar.substring(idx) : avatar
                avatar = baseURL.replace(/\/$/, '') + '/' + purePath.replace(/^\//, '')
            }
            
            return {
                id: user.id,
                username: user.username,
                email: user.email,
                phone: user.phone,
                avatar: avatar,
                nickname: user.nickname,
                isAdmin: user.isAdmin === 1,
                status: user.status === 1 ? 'active' : 'inactive',
                createdTime: user.createTime,
                password: user.password
            }
        })
    } catch (e) {
        ElMessage.error('获取用户列表失败')
    } finally {
        loading.value = false
    }
}

const formatFileSize = (size) => {
    if (size === 0) return '0 B'
    const units = ['B', 'KB', 'MB', 'GB', 'TB']
    let index = 0
    let fileSize = size
    
    while (fileSize >= 1024 && index < units.length - 1) {
        fileSize /= 1024
        index++
    }
    
    return `${fileSize.toFixed(1)} ${units[index]}`
}

const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('zh-CN')
}

const handleViewPassword = (user) => {
    viewPasswordUser.value = user
    viewPasswordDialogVisible.value = true
}

// 生命周期
onMounted(() => {
    loadUserList()
})
</script>

<style lang="scss" scoped>
.user-management-container {
    padding: 20px;
    background-color: #f5f5f5;
    min-height: 100vh;
}

.toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: white;
    padding: 16px 20px;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    margin-bottom: 16px;

    .toolbar-left {
        display: flex;
        gap: 12px;
    }

    .toolbar-right {
        display: flex;
        align-items: center;
    }
}

.user-list-container {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    overflow: hidden;
}

.user-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .user-details {
        flex: 1;
        
        .username {
            font-weight: 500;
            margin-bottom: 4px;
        }
        
        .email {
            font-size: 12px;
            color: #666;
        }
    }
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}

:deep(.el-table) {
    .el-table__header {
        background-color: #fafafa;
    }
    
    .el-table__row {
        cursor: pointer;
        
        &:hover {
            background-color: #f5f7fa;
        }
    }
}
</style> 