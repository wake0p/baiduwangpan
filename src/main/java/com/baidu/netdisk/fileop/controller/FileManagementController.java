package com.baidu.netdisk.fileop.controller;

import com.baidu.netdisk.entity.File;
import com.baidu.netdisk.entity.Folder;
import com.baidu.netdisk.entity.Share;
import com.baidu.netdisk.entity.ShareRequest;
import com.baidu.netdisk.fileop.service.FileService;
import com.baidu.netdisk.fileop.service.FolderService;
import com.baidu.netdisk.fileop.service.MailService;
import com.baidu.netdisk.fileop.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@RestController
@RequestMapping("/")
public class FileManagementController {

    @Autowired
    private FileService fileService;

    @Autowired
    private FolderService folderService;

    // 获取指定文件夹下的所有文件
    @GetMapping("/files/{folderId}")
    public List<File> getFilesByFolderId(@PathVariable Long folderId) {
        return fileService.getFilesByFolderId(folderId);
    }

    // 获取指定文件夹下的所有子文件夹
    @GetMapping("/folders/{folderId}")
    public List<Folder> getFoldersByFolderId(@PathVariable Long folderId) {
        return folderService.getFoldersByFolderId(folderId);
    }

    // 创建新文件夹
    @PostMapping("/folders")
    public Folder createFolder(@RequestBody Folder folder) {
        return folderService.createFolder(folder);
    }

    @PostMapping("/files")
    public ResponseEntity<File> createFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folderId") Long folderId,
            @RequestParam("fileName") String fileName,
            @RequestParam("fileType") String fileType,
            @RequestParam("status") Boolean status,
            // 可选：如果需要自定义文件名
            @RequestParam("userId") @NotNull Long userId,
            @RequestParam(value = "isDeleted", required = false, defaultValue = "false") Boolean isDeleted
    ) {
        try {
            // 校验参数
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }

            // 调用 FileService 处理文件上传和保存
            File savedFile = fileService.createFile(file,folderId, fileName,fileType,status, userId,isDeleted);

            return ResponseEntity.status(HttpStatus.CREATED).body(savedFile);
        } catch (IllegalArgumentException e) {
            log.error("创建文件失败，参数错误：{}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("创建文件失败，用户ID：{}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    // 重命名文件或文件夹
    @PutMapping("/{itemType}s/{itemId}/rename")
    public ResponseEntity<String> renameItem(@PathVariable String itemType, @PathVariable Long itemId, @RequestBody RenameRequest renameRequest) {
        String newName = renameRequest.getNewName();
        if ("file".equals(itemType)) {
            if (fileService.renameFile(itemId, newName)) {
                return ResponseEntity.ok("重命名成功");
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("当前目录存在同名文件，不能修改");
            }
        } else if ("folder".equals(itemType)) {
            if (folderService.renameFolder(itemId, newName)) {
                return ResponseEntity.ok("重命名成功");
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("当前目录存在同名文件夹，不能修改");
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("无效的操作类型");
    }

    static class RenameRequest {
        private String newName;

        public String getNewName() {
            return newName;
        }

        public void setNewName(String newName) {
            this.newName = newName;
        }
    }

    @Autowired
    private ShareService shareService;

    @Autowired
    private MailService mailService;

    // 邮件分享接口
    @PostMapping("/files/{fileId}/share/email")
    public ResponseEntity<String> shareFileByEmail(
            @PathVariable Long fileId,
            @RequestBody EmailShareRequest request,
            @RequestAttribute(required = false) Long userId
    ) {
        // 1. 校验文件是否存在
        File file = fileService.getFileById(fileId);
        if (file == null) {
            return ResponseEntity.badRequest().body("文件不存在");
        }

        // 2. 生成分享记录（若失败，直接返回）
        Share share;
        try {
            share = shareService.createShare(fileId, userId, request.getShareType(), request.getExpireTime());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        // 3. 构造邮件内容（动态处理密码）
        String passwordPart = share.getShareCode() != null ?
                "\n密码：" + share.getShareCode() : "";
        String mailContent = String.format(
                "XX通过本云盘分享了以下内容：\n文件名：%s\n分享链接：http://your-domain/share/%s%s",
                file.getFileName(), share.getId(), passwordPart
        );

        // 4. 发送邮件（捕获异常）
        try {
            mailService.sendShareMail(request.getRecipient(), "文件分享", mailContent);
        } catch (MailException e) {
            log.error("邮件发送失败，文件ID：{}，收件人：{}", fileId, request.getRecipient(), e);
            return ResponseEntity.internalServerError().body("邮件发送失败，请稍后重试");
        }

        return ResponseEntity.ok("邮件分享成功");
    }

    // 邮件分享请求体
    static class EmailShareRequest {
        private String recipient; // 收件人邮箱
        private String shareType; // public/private
        private LocalDateTime expireTime; // 过期时间

        public String getShareType() {
            return shareType;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }

        public String getRecipient() {
            return recipient;
        }
    }

    @GetMapping("/folders/tree/{parentId}")
    public List<Folder> getFolderTree(@PathVariable Long parentId) {
        return folderService.getFolderTreeByParentId(parentId);
    }

    @GetMapping("/files/{fileId}/preview")
    public ResponseEntity<String> previewFile(@PathVariable Long fileId) {
        File file = fileService.getFileById(fileId);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("文件不存在");
        }
        if (fileService.canPreview(file)) {
            return ResponseEntity.ok("支持预览，预览逻辑需补充完整（如调用预览服务、返回预览URL等）");
        } else {
            return ResponseEntity.ok("此文件暂不支持预览");
        }
    }

    // 创建文件分享
    @PostMapping("/files/{fileId}/share")
    public ResponseEntity<Share> createShare(@PathVariable Long fileId, @RequestBody ShareRequest request) {
        Share share = shareService.createShare(fileId, request.getUserId(), request.getShareType(), request.getExpireTime());
        return ResponseEntity.ok(share);
    }

    // 验证分享密码
    @PostMapping("/shares/{shareId}/verify")
    public ResponseEntity<Boolean> verifySharePassword(@PathVariable Long shareId, @RequestBody String password) {
        return ResponseEntity.ok(shareService.verifyPassword(shareId, password));
    }

    // ===================== 回收站相关接口 =====================

    /**
     * 统一返回结构
     */
    static class Result {
        private boolean success;
        private String message;
        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    /**
     * 文件/文件夹移入回收站
     */
    @PostMapping("/recycle/bin")
    public ResponseEntity<Result> moveToRecycleBin(
            @RequestBody MoveToRecycleRequest request
    ) {
        Long userId = request.getUserId();
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body(new Result(false, "用户ID无效"));
        }
        try {
            if ((request.getFileIds() == null || request.getFileIds().isEmpty()) &&
                (request.getFolderIds() == null || request.getFolderIds().isEmpty())) {
                return ResponseEntity.badRequest().body(new Result(false, "未指定要删除的文件或文件夹"));
            }
            fileService.moveToRecycleBin(request.getFileIds(), userId);
            folderService.moveToRecycleBin(request.getFolderIds(), userId);
            return ResponseEntity.ok(new Result(true, "移入回收站成功"));
        } catch (IllegalArgumentException e) {
            log.error("移入回收站失败，参数错误：{}", e.getMessage());
            return ResponseEntity.badRequest().body(new Result(false, "参数错误：" + e.getMessage()));
        } catch (Exception e) {
            log.error("移入回收站失败，用户ID：{}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Result(false, "服务器异常，移入回收站失败"));
        }
    }

    /**
     * 从回收站恢复文件/文件夹
     */
    @PostMapping("/recycle/bin/restore")
    public ResponseEntity<Result> restoreFromRecycleBin(
            @RequestBody RestoreRequest request,
            @RequestParam("userId") Long userId
    ) {
        try {
            if ((request.getFileIds() == null || request.getFileIds().isEmpty()) &&
                (request.getFolderIds() == null || request.getFolderIds().isEmpty())) {
                return ResponseEntity.badRequest().body(new Result(false, "未指定要恢复的文件或文件夹"));
            }
            fileService.restoreFromRecycleBin(request.getFileIds(), userId);
            folderService.restoreFromRecycleBin(request.getFolderIds(), userId);
            return ResponseEntity.ok(new Result(true, "恢复成功"));
        } catch (IllegalArgumentException e) {
            log.error("恢复失败，参数错误：{}", e.getMessage());
            return ResponseEntity.badRequest().body(new Result(false, "参数错误：" + e.getMessage()));
        } catch (Exception e) {
            log.error("恢复失败，用户ID：{}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Result(false, "服务器异常，恢复失败"));
        }
    }

    /**
     * 彻底删除文件/文件夹
     */
    @DeleteMapping("/recycle/bin")
    public ResponseEntity<Result> deletePermanently(
            @RequestBody DeleteRequest request,
            @RequestParam("userId") Long userId
    ) {
        try {
            if ((request.getFileIds() == null || request.getFileIds().isEmpty()) &&
                (request.getFolderIds() == null || request.getFolderIds().isEmpty())) {
                return ResponseEntity.badRequest().body(new Result(false, "未指定要删除的文件或文件夹"));
            }
            fileService.deletePermanently(request.getFileIds(), userId);
            folderService.deletePermanently(request.getFolderIds(), userId);
            return ResponseEntity.ok(new Result(true, "删除成功"));
        } catch (IllegalArgumentException e) {
            log.error("彻底删除失败，参数错误：{}", e.getMessage());
            return ResponseEntity.badRequest().body(new Result(false, "参数错误：" + e.getMessage()));
        } catch (Exception e) {
            log.error("彻底删除失败，用户ID：{}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new Result(false, "服务器异常，删除失败"));
        }
    }

    /**
     * 获取回收站列表
     */
    @GetMapping("/recycle/bin")
    public ResponseEntity<RecycleBinResponse> getRecycleBinItems(
            @RequestParam("userId") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            // 参数校验
            if (page <= 0 || pageSize <= 0) {
                return ResponseEntity.badRequest().build();
            }

            // 获取回收站内容
            RecycleBinResponse response = new RecycleBinResponse();
            response.setFiles(fileService.getRecycleBinFiles(userId, page, pageSize));
            response.setFolders(folderService.getRecycleBinFolders(userId, page, pageSize));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取回收站列表失败，用户ID：{}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    // 回收站请求/响应类
    static class MoveToRecycleRequest {
        private List<Long> fileIds;
        private List<Long> folderIds;
        private Long userId;

        public List<Long> getFileIds() {
            return fileIds;
        }
        public Long getUserId() {
            return userId;
        }

        public List<Long> getFolderIds() {
            return folderIds;
        }
    }

    static class RestoreRequest {
        private List<Long> fileIds;
        private List<Long> folderIds;

        public List<Long> getFileIds() {
            return fileIds;
        }

        public List<Long> getFolderIds() {
            return folderIds;
        }
    }

    static class DeleteRequest {
        private List<Long> fileIds;
        private List<Long> folderIds;

        public List<Long> getFileIds() {
            return fileIds;
        }

        public List<Long> getFolderIds() {
            return folderIds;
        }
    }

    static class RecycleBinResponse {
        private List<File> files;
        private List<Folder> folders;

        public List<File> getFiles() {
            return files;
        }

        public void setFiles(List<File> files) {
            this.files = files;
        }

        public List<Folder> getFolders() {
            return folders;
        }

        public void setFolders(List<Folder> folders) {
            this.folders = folders;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(FileManagementController.class);

    // ===================== 层级结构相关接口 =====================

    /**
     * 获取用户所有文件和文件夹的层级结构
     */
    @GetMapping("/user/{userId}/hierarchy")
    public ResponseEntity<HierarchyResponse> getUserHierarchy(@PathVariable Long userId) {
        try {
            // 参数校验
            if (userId == null || userId <= 0) {
                return ResponseEntity.badRequest().build();
            }

            // 获取用户所有文件和文件夹
            List<File> allFiles = fileService.getAllFilesByUserId(userId);
            List<Folder> allFolders = folderService.getAllFoldersByUserId(userId);

            // 构建层级结构
            HierarchyResponse response = buildHierarchyStructure(allFiles, allFolders, userId, 1);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户层级结构失败，用户ID：{}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 获取用户指定文件夹下的层级结构
     */
    @GetMapping("/user/{userId}/folder/{folderId}/hierarchy")
    public ResponseEntity<HierarchyResponse> getFolderHierarchy(
            @PathVariable Long userId,
            @PathVariable Long folderId
    ) {
        try {
            // 参数校验
            if (userId == null || userId <= 0 || folderId == null || folderId < 0) {
                return ResponseEntity.badRequest().build();
            }

            // 获取指定文件夹下的所有文件和子文件夹（包括递归）
            List<File> files = fileService.getAllFilesByFolderId(folderId, userId);
            List<Folder> folders = folderService.getAllFoldersByFolderId(folderId, userId);
            System.out.println("folders from db: " + folders);
            // 构建层级结构
            HierarchyResponse response = buildHierarchyStructure(files, folders, userId, 2);
            System.out.println("response: " + response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取文件夹层级结构失败，用户ID：{}，文件夹ID：{}", userId, folderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 构建层级结构的辅助方法
     */
    private HierarchyResponse buildHierarchyStructure(List<File> files, List<Folder> folders, Long userId,int kind) {
        HierarchyResponse response = new HierarchyResponse();
        
        // 过滤当前用户的文件和文件夹（服务层已经过滤，这里再次确认）
        List<File> userFiles = files.stream()
                .filter(file -> file.getUserId().equals(userId) && !file.getIsDeleted())
                .collect(java.util.stream.Collectors.toList());
        
        List<Folder> userFolders = folders.stream()
                .filter(folder -> folder.getUserId().equals(userId) && !folder.getIsDeleted())
                .collect(java.util.stream.Collectors.toList());
        System.out.println(userFolders);
        // 构建文件夹层级结构
        List<FolderNode> folderNodes = buildFolderHierarchy(userFolders, userId, kind);
        System.out.println(folderNodes);
        // 构建文件节点（按文件夹分组）
        List<FileNode> fileNodes = userFiles.stream()
                .map(this::convertToFileNode)
                .collect(java.util.stream.Collectors.toList());

        response.setFolders(folderNodes);
        response.setFiles(fileNodes);
        
        return response;
    }

    /**
     * 构建文件夹层级结构
     */
    private List<FolderNode> buildFolderHierarchy(List<Folder> folders, Long userId, int kind) {
        // 创建文件夹ID到文件夹的映射
        java.util.Map<Long, Folder> folderMap = folders.stream()
                .collect(java.util.stream.Collectors.toMap(Folder::getId, folder -> folder));
        System.out.println("folderMap"+folderMap);
        // 找到根文件夹（parentId为0或null的文件夹）
        List<FolderNode> rootFolders = new java.util.ArrayList<>();

        if (kind==1) {
            for (Folder folder : folders) {
                if (folder.getParentId() == null || folder.getParentId() == 0) {
                    FolderNode node = convertToFolderNode(folder);
                    buildFolderChildren(node, folderMap, userId);
                    rootFolders.add(node);
                }
            }
        }

        if (kind==2) {
            for (Folder folder : folders) {

                FolderNode node = convertToFolderNode(folder);
                buildFolderChildren(node, folderMap, userId);
                rootFolders.add(node);

            }
        }

        System.out.println("rootFolders"+rootFolders);
        return rootFolders;
    }

    /**
     * 递归构建文件夹的子节点
     */
    private void buildFolderChildren(FolderNode parentNode, java.util.Map<Long, Folder> folderMap, Long userId) {
        List<FolderNode> children = new java.util.ArrayList<>();
        
        for (Folder folder : folderMap.values()) {
            if (folder.getParentId() != null && folder.getParentId().equals(parentNode.getId())) {
                FolderNode childNode = convertToFolderNode(folder);
                buildFolderChildren(childNode, folderMap, userId);
                children.add(childNode);
            }
        }
        
        parentNode.setChildren(children);
    }

    /**
     * 将Folder实体转换为FolderNode
     */
    private FolderNode convertToFolderNode(Folder folder) {
        FolderNode node = new FolderNode();
        node.setId(folder.getId());
        node.setName(folder.getFolderName());
        node.setPath(folder.getFolderPath());
        node.setParentId(folder.getParentId());
        node.setCreateTime(folder.getCreateTime());
        node.setUpdateTime(folder.getUpdateTime());
        return node;
    }

    /**
     * 将File实体转换为FileNode
     */
    private FileNode convertToFileNode(File file) {
        FileNode node = new FileNode();
        node.setId(file.getId());
        node.setName(file.getFileName());
        node.setType(file.getFileType());
        node.setSize(file.getFileSize());
        node.setFolderId(file.getFolderId());
        node.setCreateTime(file.getCreateTime());
        node.setUpdateTime(file.getUpdateTime());
        node.setFavorite(file.getIsFavorite());
        return node;
    }

    // 层级结构响应类
    static class HierarchyResponse {
        private List<FolderNode> folders;
        private List<FileNode> files;

        public List<FolderNode> getFolders() {
            return folders;
        }

        public void setFolders(List<FolderNode> folders) {
            this.folders = folders;
        }

        public List<FileNode> getFiles() {
            return files;
        }

        public void setFiles(List<FileNode> files) {
            this.files = files;
        }
    }

    // 文件夹节点类
    static class FolderNode {
        private Long id;
        private String name;
        private String path;
        private Long parentId;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private List<FolderNode> children;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        
        public LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
        
        public List<FolderNode> getChildren() { return children; }
        public void setChildren(List<FolderNode> children) { this.children = children; }
    }

    // 文件节点类
    static class FileNode {
        private Long id;
        private String name;
        private String type;
        private Long size;
        private Long folderId;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private Boolean favorite;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        
        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
        
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
        
        public LocalDateTime getUpdateTime() { return updateTime; }
        public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
        
        public Boolean getFavorite() { return favorite; }
        public void setFavorite(Boolean favorite) { this.favorite = favorite; }
    }

    // ===================== 文件上传与下载 =====================

    /**
     * 文件上传接口
     */
    @PostMapping("/files/upload")
    public ResponseEntity<File> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("folderId") Long folderId,
            @RequestParam("userId") Long userId
    ) {
        try {
            String fileName = file.getOriginalFilename();
            String fileType = fileName != null && fileName.contains(".") ?
                    fileName.substring(fileName.lastIndexOf('.') + 1) : "unknown";
            Boolean status = true;
            Boolean isDeleted = false;
            File savedFile = fileService.createFile(file, folderId, fileName, fileType, status, userId, isDeleted);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedFile);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 文件下载接口
     */
    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<?> downloadFile(@PathVariable Long fileId) {
        File file = fileService.getFileById(fileId);
        if (file == null || file.getIsDeleted() || !file.getStatus()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("文件不存在或已被删除");
        }
        try {
            java.nio.file.Path filePath = fileService.getFullFilePath(file);
            if (!java.nio.file.Files.exists(filePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("文件不存在于服务器");
            }
            String fileName = file.getFileName();
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + java.net.URLEncoder.encode(fileName, "UTF-8") + "\"")
                    .header("Content-Type", "application/octet-stream")
                    .body(new org.springframework.core.io.FileSystemResource(filePath));
        } catch (Exception e) {
            log.error("文件下载失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件下载失败");
        }
    }
}