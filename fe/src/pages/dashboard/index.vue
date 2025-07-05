<template>
    <div class="dashboard-layout">
        <!-- 侧边栏 -->
        <div class="sidebar">
            <div class="sidebar-header">
                <h2 class="logo">千度网盘系统</h2>
            </div>
            <div class="sidebar-menu">
                <el-menu
                    :default-active="activeMenu"
                    class="sidebar-menu-list"
                    @select="handleMenuSelect"
                >
                    <el-menu-item index="/dashboard">
                        <el-icon><Folder /></el-icon>
                        <span>我的文件</span>
                    </el-menu-item>
                    <el-menu-item index="/dashboard/recycle">
                        <el-icon><Delete /></el-icon>
                        <span>回收站</span>
                    </el-menu-item>
                    <el-menu-item 
                        v-if="userStore.isAdmin" 
                        index="/dashboard/user-management"
                    >
                        <el-icon><User /></el-icon>
                        <span>用户管理</span>
                    </el-menu-item>
                    <el-menu-item index="/dashboard/profile">
                        <el-icon><UserFilled /></el-icon>
                        <span>个人信息</span>
                    </el-menu-item>
                </el-menu>
            </div>
            <div class="sidebar-footer">
                <el-popover
                    placement="top"
                    width="180"
                    trigger="hover"
                    popper-class="user-popover"
                >
                    <template #reference>
                        <div class="user-info" style="cursor:pointer;">
                            <el-avatar :size="32" :src="userStore.user?.avatar">
                                {{ userStore.user?.username?.charAt(0)?.toUpperCase() || 'U' }}
                            </el-avatar>
                            <div class="user-details">
                                <div class="username">{{ userStore.user?.nickname || userStore.user?.username || '用户' }}</div>
                                <div class="role">{{ userStore.isAdmin ? '管理员' : '普通用户' }}</div>
                            </div>
                        </div>
                    </template>
                    <div style="text-align:center;">
                        <div style="margin-bottom: 10px;">
                            {{ userStore.user?.nickname || userStore.user?.username || '用户' }}
                        </div>
                        <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
                    </div>
                </el-popover>
            </div>
        </div>

        <!-- 主内容区域 -->
        <div class="main-content">
            <!-- 路由视图 -->
            <div class="content-area">
                <router-view />
            </div>
        </div>
    </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
    Folder,
    User,
    Delete,
    UserFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 计算属性
const activeMenu = computed(() => {
    return route.path
})

// 方法
const handleMenuSelect = (index) => {
    router.push(index)
}

const handleLogout = () => {
    userStore.logout()
    router.push('/login')
}

// 生命周期
onMounted(() => {
    // 初始化用户信息（如果没有的话）
    if (!userStore.user) {
        userStore.setUser({
            id: 1,
            username: 'admin',
            nickname: '系统管理员',
            isAdmin: true
        })
    }
})
</script>

<style lang="scss" scoped>
.dashboard-layout {
    display: flex;
    height: 100vh;
    background-color: #f5f5f5;
}

.sidebar {
    width: 240px;
    background: white;
    border-right: 1px solid #e4e7ed;
    display: flex;
    flex-direction: column;
    box-shadow: 2px 0 4px rgba(0, 0, 0, 0.1);

    .sidebar-header {
        padding: 20px;
        border-bottom: 1px solid #e4e7ed;
        
        .logo {
            margin: 0;
            color: #409eff;
            font-size: 18px;
            font-weight: 600;
        }
    }

    .sidebar-menu {
        flex: 1;
        padding: 20px 0;
        
        .sidebar-menu-list {
            border: none;
            
            :deep(.el-menu-item) {
                height: 50px;
                line-height: 50px;
                margin: 0 16px;
                border-radius: 8px;
                
                &:hover {
                    background-color: #f0f9ff;
                    color: #409eff;
                }
                
                &.is-active {
                    background-color: #409eff;
                    color: white;
                }
            }
        }
    }

    .sidebar-footer {
        padding: 16px 20px;
        border-top: 1px solid #e4e7ed;
        
        .user-info {
            display: flex;
            align-items: center;
            gap: 12px;
            
            .user-details {
                flex: 1;
                
                .username {
                    font-weight: 500;
                    font-size: 14px;
                    margin-bottom: 2px;
                    color: #000000;
                }
                
                .role {
                    font-size: 12px;
                    color: #666;
                }
            }
        }
    }
}

.main-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.content-area {
    flex: 1;
    margin: 16px;
    overflow: auto;
}

:deep(.user-popover) {
    padding: 18px 12px 12px 12px;
    border-radius: 12px;
}
</style>