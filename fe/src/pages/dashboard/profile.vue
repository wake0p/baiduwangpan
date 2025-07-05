<template>
    <div class="profile-container">
        <el-card class="profile-card">
            <div class="profile-main">
                <div class="avatar-section">
                    <el-avatar :size="120" :src="userStore.avatar" @error="handleAvatarError">
                        {{ userInfo.nickname?.charAt(0)?.toUpperCase() || userInfo.username?.charAt(0)?.toUpperCase() || 'U' }}
                    </el-avatar>
                    <div class="nickname">{{ userInfo.nickname || userInfo.username }}</div>
                    <el-button size="small" @click="handleUploadAvatar" class="avatar-btn">更换头像</el-button>
                </div>
                <div class="info-section">
                    <div class="section-title">基本信息</div>
                    <el-divider class="divider" />
                    <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-width="72px" class="info-form">
                        <el-form-item label="用户名">
                            <el-input v-model="userInfo.username" disabled />
                        </el-form-item>
                        <el-form-item label="昵称" prop="nickname">
                            <el-input v-model="editForm.nickname" placeholder="请输入昵称" disabled/>
                        </el-form-item>
                        <el-form-item label="邮箱" prop="email">
                            <el-input v-model="editForm.email" placeholder="请输入邮箱" disabled/>
                        </el-form-item>
                        <el-form-item label="手机号" prop="phone">
                            <el-input v-model="editForm.phone" placeholder="请输入手机号" />
                        </el-form-item>
                        <el-form-item label="密码" prop="password">
                            <el-input v-model="editForm.password" placeholder="请输入新密码" show-password />
                        </el-form-item>
                        <el-form-item>
                            <el-button type="primary" @click="confirmEditBasic">保存修改</el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </el-card>

        <!-- 头像上传对话框 -->
        <el-dialog v-model="avatarDialogVisible" title="更换头像" width="400px">
            <el-upload
                ref="avatarUploadRef"
                :auto-upload="false"
                :on-change="handleAvatarChange"
                :file-list="avatarFileList"
                accept="image/*"
                drag
            >
                <el-icon class="el-icon--upload"><Upload /></el-icon>
                <div class="el-upload__text">
                    将头像拖到此处，或<em>点击上传</em>
                </div>
                <template #tip>
                    <div class="el-upload__tip">
                        只能上传 jpg/png 文件，且不超过 2MB
                    </div>
                </template>
            </el-upload>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="avatarDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmUploadAvatar">确定</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { Upload } from '@element-plus/icons-vue'
import request, { baseURL } from '@/utils/index'

const userStore = useUserStore()

const editFormRef = ref()
const avatarUploadRef = ref()
const avatarFileList = ref([])
const avatarDialogVisible = ref(false)

const userInfo = ref({
    id: 1,
    username: 'admin',
    nickname: '系统管理员',
    email: 'admin@example.com',
    phone: '13800138000',
    avatar: '',
})

const editForm = reactive({
    nickname: '',
    email: '',
    phone: '',
    password: ''
})

const editRules = {
    nickname: [
        { message: '请输入昵称', trigger: 'blur' },
        { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
    ],
    email: [
        { message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
    ],
    phone: [
        { pattern: /^1[2-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    password: [
        { min: 6, max: 32, message: '密码长度在 6 到 32 个字符', trigger: 'blur' }
    ]
}

// 新增：获取用户最新信息
const fetchProfile = async () => {
    try {
        const res = await request.get('/profile', { params: { username: userInfo.value.username } })
        if (res.success && res.user) {
            Object.assign(userInfo.value, res.user)
            userStore.setUser(res.user)
            // 同步表单
            editForm.nickname = res.user.nickname
            editForm.email = res.user.email
            editForm.phone = res.user.phone
            editForm.password = res.user.password
        }
    } catch (e) {}
}

const handleUploadAvatar = () => {
    avatarFileList.value = []
    avatarDialogVisible.value = true
}

const handleAvatarChange = (file, fileList) => {
    avatarFileList.value = fileList
}

const confirmUploadAvatar = async () => {
    if (avatarFileList.value.length === 0) {
        ElMessage.warning('请选择要上传的头像')
        return
    }
    try {
        const formData = new FormData()
        formData.append('username', userInfo.value.username)
        formData.append('newAvatar', avatarFileList.value[0].raw)
        const res = await request.post('/changeAvatar', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (res.success) {
            ElMessage.success('头像上传成功')
            avatarDialogVisible.value = false
            avatarFileList.value = []
            await fetchProfile() // 上传成功后刷新用户信息
        } else {
            ElMessage.error(res.message || '头像上传失败')
        }
    } catch (e) {
        ElMessage.error('头像上传失败')
    }
}

const handleAvatarError = () => {
    ElMessage.warning('头像加载失败')
}

const confirmEditBasic = async () => {
    if (!editFormRef.value) return
    try {
        await editFormRef.value.validate()
        // 检测密码是否更改且不为空
        if (editForm.password && editForm.password !== userInfo.value.password) {
            const res = await request.post('/changePassword', null, {
                params: {
                    username: userInfo.value.username,
                    oldPassword: userInfo.value.password,
                    newPassword: editForm.password
                }
            })
            if (res.success) {
                ElMessage.success('密码修改成功')
                editForm.password = ''
            } else {
                ElMessage.error(res.message || '密码修改失败')
                return
            }
        }
        // 检测手机号是否更改且不为空
        if (editForm.phone && editForm.phone !== userInfo.value.phone) {
            const res = await request.post('/changePhone', null, {
                params: {
                    username: userInfo.value.username,
                    newPhone: editForm.phone
                }
            })
            if (res.success) {
                ElMessage.success('电话修改成功')
            } else {
                ElMessage.error(res.message || '电话修改失败')
                return
            }
        }
        await fetchProfile()
    } catch (error) {
        // 表单校验失败
    }
}

const getAvatarUrl = (avatar) => {
    if (!avatar) return ''
    let url = ''
    if (/^https?:\/\//.test(avatar)) {
        url = encodeURI(avatar)
    } else {
        url = encodeURI(baseURL.replace(/\/$/, '') + '/' + avatar.replace(/^\//, ''))
    }
    // 加时间戳防止缓存
    return url + '?t=' + Date.now()
}

onMounted(() => {
    if (userStore.user) {
        Object.assign(userInfo.value, userStore.user)
    }
    editForm.nickname = userInfo.value.nickname
    editForm.email = userInfo.value.email
    editForm.phone = userInfo.value.phone
    editForm.password = userInfo.value.password
    // 自动拉取一次最新信息
    fetchProfile()
})
</script>

<style lang="scss" scoped>
.profile-container {
    display: flex;
    justify-content: center;
    align-items: flex-start;
    min-height: 80vh;
    padding: 60px 0 0 0;
    background: #f6f8fa;
}

.profile-card {
    width: 600px;
    border-radius: 26px;
    box-shadow: 0 10px 48px 0 rgba(80, 120, 200, 0.13), 0 2px 8px 0 rgba(80,120,200,0.06);
    padding: 48px 56px 40px 56px;
    background: #fff;
    border: none;
    transition: box-shadow 0.2s;
}

.profile-card:hover {
    box-shadow: 0 16px 64px 0 rgba(80, 120, 200, 0.16), 0 4px 16px 0 rgba(80,120,200,0.09);
}

.profile-main {
    display: flex;
    gap: 56px;
    align-items: flex-start;
}

.avatar-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 22px;
    min-width: 160px;
}
.avatar-section .el-avatar {
    box-shadow: 0 4px 16px rgba(64,158,255,0.13);
    border: 4px solid #eaf3ff;
    background: #f7faff;
    width: 120px !important;
    height: 120px !important;
    font-size: 48px;
}
.nickname {
    font-size: 20px;
    font-weight: bold;
    color: #222;
    margin-top: 2px;
    margin-bottom: 2px;
    letter-spacing: 1px;
}
.avatar-btn {
    margin-top: 2px;
    padding: 0 22px;
    font-size: 15px;
    border-radius: 18px;
    border: 1px solid #dbeafe;
    background: #f5faff;
    color: #409eff;
    transition: all 0.2s;
    height: 34px;
}
.avatar-btn:hover {
    background: #e8f3ff;
    color: #1769aa;
    border-color: #b6e0fe;
}

.info-section {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    justify-content: center;
}
.section-title {
    font-size: 17px;
    font-weight: 600;
    color: #409eff;
    margin-bottom: 2px;
    letter-spacing: 1px;
}
.divider {
    margin: 8px 0 18px 0;
}
.info-form {
    .el-form-item {
        margin-bottom: 26px;
    }
    .el-form-item__label {
        color: #888;
        font-size: 15px;
        font-weight: 500;
        letter-spacing: 1px;
    }
    .el-input {
        font-size: 16px;
        border-radius: 10px;
        background: #f7faff;
        height: 40px;
    }
    .el-input__inner {
        height: 40px;
        line-height: 40px;
        padding-left: 14px;
    }
    .el-input.is-disabled .el-input__inner {
        color: #b0b3b8;
        background: #f5f6fa;
    }
    .el-button[type="primary"] {
        width: 100%;
        border-radius: 22px;
        font-size: 17px;
        letter-spacing: 2px;
        padding: 12px 0;
        background: linear-gradient(90deg, #409eff 0%, #66b1ff 100%);
        border: none;
        box-shadow: 0 2px 8px rgba(64,158,255,0.08);
        transition: background 0.2s;
    }
    .el-button[type="primary"]:hover {
        background: linear-gradient(90deg, #1769aa 0%, #409eff 100%);
    }
}

@media (max-width: 900px) {
    .profile-card {
        width: 98vw;
        min-width: 0;
        padding: 18px 4vw 18px 4vw;
    }
    .profile-main {
        flex-direction: column;
        align-items: center;
        gap: 24px;
    }
    .info-section {
        width: 100%;
    }
}

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}
:deep(.el-upload-dragger) {
    width: 100%;
    height: 180px;
}
</style> 