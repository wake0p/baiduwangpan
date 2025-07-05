<template>
    <div class="file-management-container">
        <!-- 顶部工具栏 -->
        <div class="toolbar">
            <div class="toolbar-left">
                <el-button type="primary" @click="handleUpload">
                    <el-icon><Upload /></el-icon>
                    上传文件
                </el-button>
                <el-button @click="handleNewFolder">
                    <el-icon><FolderAdd /></el-icon>
                    新建文件夹
                </el-button>
                <el-button @click="handleDownload" :disabled="!selectedFiles.length">
                    <el-icon><Download /></el-icon>
                    下载
                </el-button>
                <el-button @click="handleDelete" :disabled="!selectedFiles.length" type="danger">
                    <el-icon><Delete /></el-icon>
                    删除
                </el-button>
            </div>
            <div class="toolbar-right">
                <el-input
                    v-model="searchKeyword"
                    placeholder="搜索文件"
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

        <!-- 面包屑导航 -->
        <div class="breadcrumb">
            <el-breadcrumb separator="/">
                <el-breadcrumb-item 
                    v-for="(item, index) in breadcrumbList" 
                    :key="index"
                    @click="handleBreadcrumbClick(index)"
                    :class="{ 'clickable': index < breadcrumbList.length - 1 }"
                >
                    {{ item.name }}
                </el-breadcrumb-item>
            </el-breadcrumb>
        </div>

        <!-- 文件列表 -->
        <div class="file-list-container">
            <el-table
                :data="filteredFileList"
                @selection-change="handleSelectionChange"
                @row-dblclick="handleRowDblClick"
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
                    </div>
                </template>
            </el-table-column>
            <el-table-column prop="size" label="大小" width="120">
                <template #default="{ row }">
                    {{ formatFileSize(row.size) }}
                </template>
            </el-table-column>
            <el-table-column prop="modifiedTime" label="修改时间" width="180">
                <template #default="{ row }">
                    {{ formatDate(row.modifiedTime) }}
                </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
                <template #default="{ row }">
                    <el-button v-if="row.type !== 'folder'" size="small" @click="handlePreview(row)">预览</el-button>
                    <el-button size="small" @click="handleDownloadSingle(row)">下载</el-button>
                    <el-button size="small" type="danger" @click="() => handleDeleteSingle(row)">删除</el-button>
                </template>
            </el-table-column>
            </el-table>
        </div>

        <!-- 上传对话框 -->
        <el-dialog v-model="uploadDialogVisible" title="上传文件" width="500px">
            <el-upload
                ref="uploadRef"
                :auto-upload="false"
                :file-list="uploadFileList"
                :http-request="handleFileUpload"
                :on-change="handleFileChange"
                multiple
                drag
            >
                <el-icon class="el-icon--upload"><Upload /></el-icon>
                <div class="el-upload__text">
                    将文件拖到此处，或<em>点击上传</em>
                </div>
            </el-upload>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="uploadDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmUpload">开始上传</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 新建文件夹对话框 -->
        <el-dialog v-model="newFolderDialogVisible" title="新建文件夹" width="400px">
            <el-form :model="newFolderForm" label-width="80px">
                <el-form-item label="文件夹名">
                    <el-input v-model="newFolderForm.name" placeholder="请输入文件夹名称" />
                </el-form-item>
            </el-form>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="newFolderDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmNewFolder">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <el-dialog v-model="previewDialogVisible" title="文件预览" width="60vw" top="5vh">
            <template v-if="previewType === 'image'">
                <img :src="previewUrl" style="max-width:100%;max-height:70vh;display:block;margin:auto;" />
            </template>
            <template v-else-if="previewType === 'video'">
                <video :src="previewUrl" controls style="max-width:100%;max-height:70vh;display:block;margin:auto;" />
            </template>
            <template v-else-if="previewType === 'audio'">
                <audio :src="previewUrl" controls style="width:100%;" />
            </template>
            <template v-else-if="previewType === 'pdf'">
                <iframe :src="previewUrl" style="width:100%;height:70vh;border:none;"></iframe>
            </template>
            <template v-else>
                暂不支持预览该类型文件
            </template>
        </el-dialog>
    </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
    Document,
    Folder,
    Picture,
    VideoPlay,
    Headset,
    Files,
    ArrowDown,
    Upload,
    FolderAdd,
    Download,
    Delete,
    Search,
    Refresh
} from '@element-plus/icons-vue'
import request from '@/utils/index'
import { useUserStore } from '@/store/user'

// 响应式数据
const loading = ref(false)
const searchKeyword = ref('')
const selectedFiles = ref([])
const uploadDialogVisible = ref(false)
const newFolderDialogVisible = ref(false)
const uploadFileList = ref([])
const uploadRef = ref()
const previewDialogVisible = ref(false)
const previewUrl = ref('')
const previewType = ref('')

// 当前路径
const currentPath = ref('/')
const breadcrumbList = ref([
    { name: '我的文件', path: '/' }
])

// 新建文件夹表单
const newFolderForm = reactive({
    name: ''
})

// 文件列表数据
const fileList = ref([
    {
        id: 1,
        name: '我的文档',
        type: 'folder',
        size: 0,
        modifiedTime: '2024-01-15 10:30:00'
    },
    {
        id: 2,
        name: '工作资料',
        type: 'folder',
        size: 0,
        modifiedTime: '2024-01-14 15:20:00'
    },
    {
        id: 3,
        name: '项目报告.docx',
        type: 'document',
        size: 1024000,
        modifiedTime: '2024-01-13 09:15:00'
    },
    {
        id: 4,
        name: '会议照片.jpg',
        type: 'image',
        size: 2048000,
        modifiedTime: '2024-01-12 16:45:00'
    },
    {
        id: 5,
        name: '演示视频.mp4',
        type: 'video',
        size: 52428800,
        modifiedTime: '2024-01-11 14:30:00'
    }
])

// 计算属性
const filteredFileList = computed(() => {
    if (!searchKeyword.value) return fileList.value
    return fileList.value.filter(file => 
        file.name.toLowerCase().includes(searchKeyword.value.toLowerCase())
    )
})

// 方法
const handleUpload = () => {
    uploadDialogVisible.value = true
}

const handleNewFolder = () => {
    newFolderDialogVisible.value = true
}

const handleDownload = () => {
    if (selectedFiles.value.length === 0) {
        ElMessage.warning('请选择要下载的文件')
        return
    }
    ElMessage.success(`开始下载 ${selectedFiles.value.length} 个文件`)
}

const handleDelete = () => {
    if (selectedFiles.value.length === 0) {
        ElMessage.warning('请选择要删除的文件')
        return
    }
    ElMessageBox.confirm(
        `确定要删除选中的 ${selectedFiles.value.length} 个文件（夹）吗？`,
        '确认删除',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            const fileIds = selectedFiles.value.filter(f => f.type !== 'folder').map(f => f.id)
            const folderIds = selectedFiles.value.filter(f => f.type === 'folder').map(f => f.id)
            const payload = {
                userId: userId,
                fileIds,
                folderIds
            }
            await request.post('/recycle/bin', payload)
            ElMessage.success('已移入回收站')
            loadFileList()
        } catch (e) {
            ElMessage.error('移入回收站失败')
        }
    })
}

const handleSearch = () => {
    console.log('搜索关键词:', searchKeyword.value)
}

const handleRefresh = () => {
    loadFileList()
}

const handleSelectionChange = (selection) => {
    selectedFiles.value = selection
}

const handleBreadcrumbClick = (index) => {
    if (index < breadcrumbList.value.length - 1) {
        const targetPath = breadcrumbList.value[index].path
        navigateToPath(targetPath)
    }
}

const handleFileUpload = async (option) => {
    const formData = new FormData()
    formData.append('file', option.file)
    formData.append('folderId', currentFolderId.value == null ? 0 : currentFolderId.value)
    formData.append('userId', userId)

    try {
        await request.post('/files/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
        ElMessage.success('上传成功')
        uploadDialogVisible.value = false
        uploadFileList.value = []
        loadFileList()
    } catch (e) {
        ElMessage.error('上传失败')
    }
}

const confirmUpload = () => {
    if (uploadFileList.value.length === 0) {
        ElMessage.warning('请选择要上传的文件')
        return
    }
    uploadRef.value.submit()
}

const currentFolderId = ref(null) // 根目录为null

const enterFolder = (folder) => {
    const newPath = currentPath.value === '/' ? `/${folder.name}` : `${currentPath.value}/${folder.name}`
    navigateToPath(newPath, folder.id)
}

const navigateToPath = (path, folderId = null) => {
    currentPath.value = path
    currentFolderId.value = folderId
    updateBreadcrumb(path)
    loadFileList()
}

const updateBreadcrumb = (path) => {
    const pathParts = path.split('/').filter(part => part)
    breadcrumbList.value = [
        { name: '我的文件', path: '/' },
        ...pathParts.map((part, index) => ({
            name: part,
            path: '/' + pathParts.slice(0, index + 1).join('/')
        }))
    ]
}

const handleDownloadSingle = (file) => {
    if (file.type === 'folder') {
        ElMessage.warning('文件夹暂不支持下载')
        return
    }
    window.open(`${request.defaults.baseURL}/files/download/${file.id}`)
}

const userStore = useUserStore()
const userId = userStore.userId

function isImageType(type) {
    return ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg'].includes((type || '').toLowerCase())
}

function isVideoType(type) {
    return ['mp4', 'webm', 'ogg'].includes((type || '').toLowerCase())
}

function isDocumentType(type) {
    return ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt'].includes((type || '').toLowerCase())
}

const loadFileList = async () => {
    loading.value = true
    try {
        let res
        if (currentFolderId.value == null || currentFolderId.value === 0) {
            // 根目录
            res = await request.get(`/user/${userId}/hierarchy`)
        } else {
            // 子文件夹
            res = await request.get(`/user/${userId}/folder/${currentFolderId.value}/hierarchy`)
        }
        const { folders = [], files = [] } = res
        // 只显示当前目录下的内容
        fileList.value = [
            // 当前目录下的文件夹
            ...folders.filter(folder => {
                if (currentFolderId.value == null) {
                    return folder.parentId == null
                } else {
                    return folder.parentId === currentFolderId.value
                }
            }).map(folder => ({
                id: folder.id,
                name: folder.name,
                type: 'folder',
                size: 0,
                modifiedTime: folder.updateTime
            })),
            // 当前目录下的文件
            ...files.filter(file => {
                if (currentFolderId.value == null) {
                    return file.folderId === 0
                } else {
                    return file.folderId === currentFolderId.value
                }
            }).map(file => ({
                id: file.id,
                name: file.name,
                type: isImageType(file.type) ? 'image'
                    : isVideoType(file.type) ? 'video'
                    : isDocumentType(file.type) ? 'document'
                    : (file.type || 'document'),
                size: file.size,
                modifiedTime: file.updateTime
            }))
        ]
    } catch (e) {
        ElMessage.error('获取文件列表失败')
    } finally {
        loading.value = false
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

const confirmNewFolder = async () => {
    if (!newFolderForm.name.trim()) {
        ElMessage.warning('请输入文件夹名称')
        return
    }

    try {
        const payload = {
            folderName: newFolderForm.name,
            folderPath: currentPath.value, // 例如 "/"
            parentId: currentFolderId.value ?? null, // 根目录为null
            userId: userId
        }
        await request.post('/folders', payload)
        ElMessage.success(`创建文件夹: ${newFolderForm.name}`)
        newFolderDialogVisible.value = false
        newFolderForm.name = ''
        loadFileList()
    } catch (e) {
        ElMessage.error('创建文件夹失败')
    }
}

const handleRowDblClick = (row) => {
    if (row.type === 'folder') {
        enterFolder(row)
    } else {
        handlePreview(row)
    }
}

const handlePreview = (file) => {
    if (file.type === 'folder') return
    if (file.type === 'image') {
        previewDialogVisible.value = true
        previewUrl.value = `${request.defaults.baseURL}/api/files/preview/${file.id}`
        previewType.value = 'image'
    } else if (file.type === 'video') {
        previewDialogVisible.value = true
        previewUrl.value = `${request.defaults.baseURL}/api/files/preview/${file.id}`
        previewType.value = 'video'
    } else if (file.type === 'audio') {
        previewDialogVisible.value = true
        previewUrl.value = `${request.defaults.baseURL}/api/files/preview/${file.id}`
        previewType.value = 'audio'
    } else if (file.type === 'document') {
        previewDialogVisible.value = true
        previewUrl.value = `${request.defaults.baseURL}/api/files/preview/${file.id}`
        previewType.value = 'pdf'
    } else {
        window.open(`${request.defaults.baseURL}/api/files/preview/${file.id}`)
    }
}

const handleFileChange = (file, fileList) => {
    uploadFileList.value = fileList
}

const handleDeleteSingle = (file) => {
    ElMessageBox.confirm(
        `确定要删除${file.type === 'folder' ? '文件夹' : '文件'} ${file.name} 吗？`,
        '确认删除',
        {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
        }
    ).then(async () => {
        try {
            const payload = {
                userId: userId,
                fileIds: file.type === 'folder' ? [] : [file.id],
                folderIds: file.type === 'folder' ? [file.id] : []
            }
            await request.post('/recycle/bin', payload)
            ElMessage.success('已移入回收站')
            loadFileList()
        } catch (e) {
            ElMessage.error('移入回收站失败')
        }
    })
}

// 生命周期
onMounted(() => {
    currentFolderId.value = null // 初始为根目录
    loadFileList()
})
</script>

<style lang="scss" scoped>
.file-management-container {
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

.breadcrumb {
    background: white;
    padding: 12px 20px;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    margin-bottom: 16px;

    .clickable {
        cursor: pointer;
        color: #409eff;

        &:hover {
            color: #66b1ff;
        }
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

.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
}

:deep(.el-upload-dragger) {
    width: 100%;
    height: 200px;
}
</style> 