package com.baidu.netdisk.fileop.service;

import com.baidu.netdisk.entity.Folder;
import com.baidu.netdisk.entity.File;
import com.baidu.netdisk.fileop.Mapper.FolderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class FolderService {
    @Autowired
    private FolderMapper folderMapper;

    @Autowired
    private FileService fileService;

    // 创建文件夹（含路径生成）
    public Folder createFolder(Folder folder) {
        // 生成文件夹路径（例如：父路径/新文件夹名）
        if (folder.getParentId() == null || folder.getParentId() == 0) {
            folder.setFolderPath(folder.getFolderName() + "/");
        } else {
            Folder parentFolder = folderMapper.getFolderById(folder.getParentId());
            folder.setFolderPath(parentFolder.getFolderPath() + folder.getFolderName() + "/");
        }
        folder.setStatus(1); // 正常状态
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());
        folderMapper.insertFolder(folder);
        return folder;
    }

    // 获取用户根文件夹
    public Folder getRootFolder(Long userId) {
        return folderMapper.getRootFolderByUserId(userId);
    }

    // 递归删除文件夹（含子文件和子文件夹）
    @Transactional
    public boolean deleteFolder(Long folderId) {
        Folder folder = folderMapper.getFolderById(folderId);
        if (folder != null) {
            // 删除子文件
            fileService.deleteFilesByFolderId(folderId);
            // 删除子文件夹
            List<Folder> subFolders = folderMapper.getFoldersByFolderId(folderId);
            for (Folder subFolder : subFolders) {
                deleteFolder(subFolder.getId());
            }
            // 删除当前文件夹
            folderMapper.deleteFolder(folderId);
            return true;
        }
        return false;
    }

    public List<Folder> getFoldersByFolderId(Long folderId) {
        return folderMapper.getFoldersByFolderId(folderId);
    }

    // 重命名文件夹
    public boolean renameFolder(Long folderId, String newName) {
        Folder folder = folderMapper.getFolderById(folderId);
        if (folder != null) {
            // 检查新名称是否已存在
            Folder existingFolder = folderMapper.getFolderByNameAndParentId(
                    newName,
                    folder.getParentId()
            );

            if (existingFolder != null) {
                return false;
            }

            // 更新文件夹名称和路径
            String oldPath = folder.getFolderPath();
            String parentPath = folder.getParentId() == null ? "" :
                    folderMapper.getFolderById(folder.getParentId()).getFolderPath();

            folder.setFolderName(newName);
            folder.setFolderPath(parentPath + newName + "/");
            folder.setUpdateTime(LocalDateTime.now());

            // 更新文件夹信息
            folderMapper.updateFolder(folder);

            // 递归更新子文件夹路径
            updateSubFolderPaths(folderId, oldPath, folder.getFolderPath());

            return true;
        }
        return false;
    }

    // 递归更新子文件夹路径
    private void updateSubFolderPaths(Long parentId, String oldPath, String newPath) {
        List<Folder> subFolders = folderMapper.getFoldersByFolderId(parentId);
        for (Folder subFolder : subFolders) {
            String newSubPath = newPath + subFolder.getFolderName() + "/";
            subFolder.setFolderPath(newSubPath);
            folderMapper.updateFolder(subFolder);

            // 递归更新更深层的子文件夹
            updateSubFolderPaths(subFolder.getId(), oldPath, newSubPath);
        }
    }

    public List<Folder> getFolderTreeByParentId(Long parentId) {
        return folderMapper.getFolderTreeByParentId(parentId);
    }

    // ===================== 回收站相关方法 =====================

    /**
     * 将文件夹移入回收站
     */
    @Transactional
    public void moveToRecycleBin(List<Long> folderIds, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        for (Long folderId : folderIds) {
            Folder folder = folderMapper.getFolderById(folderId);
            if (folder != null && folder.getUserId().equals(userId)) {
                // 标记为已删除
                folder.setIsDeleted(1);
                folder.setDeletedTime(now);
                folder.setDeletedBy(userId);
                folder.setUpdateTime(now);
                folderMapper.updateFolder(folder);

                // 递归处理子文件夹和文件（仅标记，不物理删除）
                markSubItemsAsDeleted(folderId, userId);
            }
        }
    }

    /**
     * 递归标记子文件夹和文件为已删除
     */
    private void markSubItemsAsDeleted(Long folderId, Long userId) {
        // 标记子文件为已删除
        fileService.moveFilesToRecycleBinByFolderId(folderId, userId);

        // 标记子文件夹为已删除
        List<Folder> subFolders = folderMapper.getFoldersByFolderId(folderId);
        for (Folder subFolder : subFolders) {
            subFolder.setIsDeleted(1);
            subFolder.setDeletedTime(LocalDateTime.now());
            subFolder.setDeletedBy(userId);
            subFolder.setUpdateTime(LocalDateTime.now());
            folderMapper.updateFolder(subFolder);

            // 递归处理更深层子文件夹
            markSubItemsAsDeleted(subFolder.getId(), userId);
        }
    }

    /**
     * 从回收站恢复文件夹
     */
    @Transactional
    public void restoreFromRecycleBin(List<Long> folderIds, Long userId) {
        for (Long folderId : folderIds) {
            Folder folder = folderMapper.getFolderById(folderId);
            if (folder != null && folder.getIsDeleted() == 1 && folder.getUserId().equals(userId)) {
                // 清除删除标记
                folder.setIsDeleted(0);
                folder.setDeletedTime(null);
                folder.setDeletedBy(null);
                folder.setUpdateTime(LocalDateTime.now());
                folderMapper.updateFolder(folder);

                // 递归恢复子文件夹和文件
                restoreSubItems(folderId, userId);
            }
        }
    }

    /**
     * 递归恢复子文件夹和文件
     */
    private void restoreSubItems(Long folderId, Long userId) {
        // 恢复子文件
        fileService.restoreFilesByFolderId(folderId, userId);

        // 恢复子文件夹
        List<Folder> subFolders = folderMapper.getFoldersByFolderId(folderId);
        for (Folder subFolder : subFolders) {
            if (subFolder.getIsDeleted() == 1) {
                subFolder.setIsDeleted(0);
                subFolder.setDeletedTime(null);
                subFolder.setDeletedBy(null);
                subFolder.setUpdateTime(LocalDateTime.now());
                folderMapper.updateFolder(subFolder);

                // 递归恢复更深层子文件夹
                restoreSubItems(subFolder.getId(), userId);
            }
        }
    }

    /**
     * 彻底删除文件夹（物理删除）
     */
    @Transactional
    public void deletePermanently(List<Long> folderIds, Long userId) {
        for (Long folderId : folderIds) {
            Folder folder = folderMapper.getFolderById(folderId);
            if (folder != null && folder.getIsDeleted() == 1 && folder.getUserId().equals(userId)) {
                // 物理删除子文件
                fileService.deleteFilesByFolderId(folderId);

                // 物理删除子文件夹
                List<Folder> subFolders = folderMapper.getFoldersByFolderId(folderId);
                for (Folder subFolder : subFolders) {
                    deletePermanently(Arrays.asList(subFolder.getId()), userId);
                }

                // 物理删除当前文件夹
                folderMapper.deleteFolder(folderId);
            }
        }
    }

    /**
     * 获取回收站文件夹列表
     */
    public List<Folder> getRecycleBinFolders(Long userId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return folderMapper.getRecycleBinFolders(userId, offset, pageSize);
    }
}