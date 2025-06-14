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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * 文件/文件夹移入回收站
     */
    @PostMapping("/recycle/bin")
    public ResponseEntity<Void> moveToRecycleBin(
            @RequestBody MoveToRecycleRequest request,
            @RequestAttribute("userId") Long userId
    ) {
        fileService.moveToRecycleBin(request.getFileIds(), userId);
        folderService.moveToRecycleBin(request.getFolderIds(), userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 从回收站恢复文件/文件夹
     */
    @PostMapping("/recycle/bin/restore")
    public ResponseEntity<Void> restoreFromRecycleBin(
            @RequestBody RestoreRequest request,
            @RequestAttribute("userId") Long userId
    ) {
        fileService.restoreFromRecycleBin(request.getFileIds(), userId);
        folderService.restoreFromRecycleBin(request.getFolderIds(), userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 彻底删除文件/文件夹
     */
    @DeleteMapping("/recycle/bin")
    public ResponseEntity<Void> deletePermanently(
            @RequestBody DeleteRequest request,
            @RequestAttribute("userId") Long userId
    ) {
        fileService.deletePermanently(request.getFileIds(), userId);
        folderService.deletePermanently(request.getFolderIds(), userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取回收站列表
     */
    @GetMapping("/recycle/bin")
    public ResponseEntity<RecycleBinResponse> getRecycleBinItems(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        RecycleBinResponse response = new RecycleBinResponse();
        response.setFiles(fileService.getRecycleBinFiles(userId, page, pageSize));
        response.setFolders(folderService.getRecycleBinFolders(userId, page, pageSize));
        return ResponseEntity.ok(response);
    }

    // 回收站请求/响应类
    static class MoveToRecycleRequest {
        private List<Long> fileIds;
        private List<Long> folderIds;

        public List<Long> getFileIds() {
            return fileIds;
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
}