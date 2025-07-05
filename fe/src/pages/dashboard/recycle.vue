<template>
    <div class="recycle-container">
        <!-- 顶部工具栏 -->
        <div class="toolbar">
            <div class="toolbar-left">
                <el-button type="primary" @click="handleRestore" :disabled="!selectedFiles.length">
                    <el-icon><RefreshLeft /></el-icon>
                    恢复
                </el-button>
                <el-button @click="() =>handleDeletePermanently(selectedFiles.value)" :disabled="!selectedFiles.length" type="danger">
                    <el-icon><Delete /></el-icon>
                    彻底删除
                </el-button>
                <el-button @click="() => handleEmptyRecycle(selectedFiles.value)" type="danger">
                    <el-icon><Delete /></el-icon>
                    清空回收站
                </el-button>
            </div>
            <div class="toolbar-right">
                <el-input
                    v-model="searchKeyword"
                    placeholder="搜索已删除文件"
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

        <!-- 文件列表 -->
        <div class="file-list-container">
            <el-table
                :data="recycleFileList"
                @selection-change="handleSelectionChange"
                style="width: 100%"
                v-loading="loading"
            >
                <el-table-column type="selection" width="55" />
                <el-table-column label="文件名" min-width="300">
                    <template #default="{ row }">
                        <div class="file-item">
                            <el-icon class="file-icon" :class="getFileIconClass(row.type)">
                                <component :is="getFileIcon(row.type)" />
                            </el-icon>
                            <span class="file-name">{{ row.name }}</span>
                            <el-tag size="small" type="info">已删除</el-tag>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column prop="size" label="大小" width="120">
                    <template #default="{ row }">
                        {{ formatFileSize(row.size) }}
                    </template>
                </el-table-column>
                <el-table-column prop="deletedTime" label="删除时间" width="180">
                    <template #default="{ row }">
                        {{ formatDate(row.deletedTime) }}
                    </template>
                </el-table-column>
                <el-table-column prop="originalPath" label="原路径" min-width="200">
                    <template #default="{ row }">
                        <el-tooltip :content="row.originalPath" placement="top">
                            <span class="path-text">{{ row.originalPath }}</span>
                        </el-tooltip>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="200" fixed="right">
                    <template #default="{ row }">
                        <el-button size="small" @click="handleRestore(row)">恢复</el-button>
                        <el-button size="small" type="danger" @click="handleDeletePermanently(row)">彻底删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </div>

        <!-- 清空回收站确认对话框 -->
        <el-dialog v-model="emptyDialogVisible" title="确认清空回收站" width="400px">
            <div class="dialog-content">
                <el-icon class="warning-icon"><Warning /></el-icon>
                <p>确定要清空回收站吗？此操作不可恢复！</p>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="emptyDialogVisible = false">取消</el-button>
                    <el-button type="danger" @click="confirmEmptyRecycle">确定清空</el-button>
                </span>
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    RefreshLeft,
    Delete,
    Search,
    Refresh,
    Warning,
    Document,
    Folder,
    Picture,
    VideoPlay,
    Headset,
    Files
} from '@element-plus/icons-vue'
import request from '@/utils/index'
import { useUserStore } from '@/store/user'

// 响应式数据
const loading = ref(false)
const searchKeyword = ref('')
const selectedFiles = ref([])
const emptyDialogVisible = ref(false)

// 回收站文件列表
const recycleFileList = ref([
    {
        id: 1,
        name: '旧项目文档.docx',
        type: 'document',
        size: 1024000,
        deletedTime: '2024-01-10 14:30:00',
        originalPath: '/我的文档/项目资料/',
        isShared: false
    },
    {
        id: 2,
        name: '测试文件夹',
        type: 'folder',
        size: 0,
        deletedTime: '2024-01-09 16:20:00',
        originalPath: '/工作资料/',
        isShared: false
    },
    {
        id: 3,
        name: '临时图片.jpg',
        type: 'image',
        size: 2048000,
        deletedTime: '2024-01-08 09:15:00',
        originalPath: '/图片/',
        isShared: false
    },
    {
        id: 4,
        name: '演示视频.mp4',
        type: 'video',
        size: 52428800,
        deletedTime: '2024-01-07 11:45:00',
        originalPath: '/视频/',
        isShared: false
    }
])

const userStore = useUserStore()
const userId = userStore.userId

// 方法
const handleRestore = (file) => {
    const files = file ? [file] : selectedFiles.value
    if (files.length === 0) {
        ElMessage.warning('请选择要恢复的文件')
        return
    }
    ElMessageBox.confirm(
        `确定要恢复选中的 ${files.length} 个文件（夹）吗？`,
        '确认恢复',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            const fileIds = files.filter(f => f.type !== 'folder').map(f => f.id)
            const folderIds = files.filter(f => f.type === 'folder').map(f => f.id)
            await request.post('/recycle/bin/restore', {
                fileIds,
                folderIds
            }, {
                params: { userId }
            })
            ElMessage.success('已恢复')
            loadRecycleList()
        } catch (e) {
            ElMessage.error('恢复失败')
        }
    })
}

const handleDeletePermanently = (file) => {
    const files = file ? [file] : selectedFiles.value
    if (files.length === 0) {
        ElMessage.warning('请选择要删除的文件')
        return
    }
    ElMessageBox.confirm(
        `确定要彻底删除选中的 ${files.length} 个文件（夹）吗？此操作不可恢复！`,
        '确认删除',
        {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            const fileIds = files.filter(f => f.type !== 'folder').map(f => f.id)
            const folderIds = files.filter(f => f.type === 'folder').map(f => f.id)
            await request.delete('/recycle/bin', {
                params: { userId },
                data: {
                    fileIds,
                    folderIds
                }
            })
            ElMessage.success('文件已彻底删除')
            loadRecycleList()
        } catch (e) {
            ElMessage.error('彻底删除失败')
        }
    })
}

const handleEmptyRecycle = () => {
    emptyDialogVisible.value = true
}

const confirmEmptyRecycle = async () => {
    if (recycleFileList.value.length === 0) {
        ElMessage.info('回收站已空')
        emptyDialogVisible.value = false
        return
    }
    selectedFiles.value = recycleFileList.value.slice()
    await handleDeletePermanently()
    emptyDialogVisible.value = false
}

const handleSearch = () => {
    console.log('搜索关键词:', searchKeyword.value)
}

const handleRefresh = () => {
    loadRecycleList()
}

const handleSelectionChange = (selection) => {
    selectedFiles.value = selection
}

const loadRecycleList = async () => {
    loading.value = true
    try {
        const res = await request.get('/recycle/bin', {
            params: {
                userId: userId,
                page: 1,
                pageSize: 100
            }
        })
        // 假设返回结构为 { files: [...], folders: [...] }
        const { files = [], folders = [] } = res
        recycleFileList.value = [
            ...folders.map(folder => ({
                id: folder.id,
                name: folder.folderName,
                type: 'folder',
                size: 0,
                deletedTime: folder.deletedTime,
                originalPath: folder.folderPath
            })),
            ...files.map(file => ({
                id: file.id,
                name: file.fileName,
                type: file.fileType || 'document',
                size: file.fileSize,
                deletedTime: file.deletedTime,
                originalPath: file.filePath
            }))
        ]
    } catch (e) {
        ElMessage.error('获取回收站列表失败')
    } finally {
        loading.value = false
        selectedFiles.value = [] // 每次刷新后清空多选
    }
}

const getFileIcon = (type) => {
    const iconMap = {
        folder: Folder,
        document: Document,
        image: Picture,
        video: VideoPlay,
        audio: Headset,
        default: Files
    }
    return iconMap[type] || iconMap.default
}

const getFileIconClass = (type) => {
    return `file-icon-${type}`
}

const formatFileSize = (size) => {
    if (size === 0) return '-'
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

// 生命周期
onMounted(() => {
    loadRecycleList()
})
</script>

<style lang="scss" scoped>
.recycle-container {
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

.file-list-container {
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    overflow: hidden;
}

.file-item {
    display: flex;
    align-items: center;
    gap: 8px;

    .file-icon {
        font-size: 20px;
        
        &.file-icon-folder {
            color: #ffa500;
        }
        
        &.file-icon-document {
            color: #409eff;
        }
        
        &.file-icon-image {
            color: #67c23a;
        }
        
        &.file-icon-video {
            color: #e6a23c;
        }
        
        &.file-icon-audio {
            color: #f56c6c;
        }
    }

    .file-name {
        flex: 1;
        margin-right: 8px;
    }
}

.path-text {
    color: #666;
    font-size: 12px;
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: inline-block;
}

.dialog-content {
    display: flex;
    align-items: center;
    gap: 12px;
    
    .warning-icon {
        font-size: 24px;
        color: #e6a23c;
    }
    
    p {
        margin: 0;
        color: #666;
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