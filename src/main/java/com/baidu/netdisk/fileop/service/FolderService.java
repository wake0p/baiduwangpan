package com.baidu.netdisk.fileop.service;
import com.baidu.netdisk.entity.File;
import com.baidu.netdisk.entity.Folder;
import com.baidu.netdisk.fileop.Mapper.FolderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FolderService {
    @Autowired
    private FolderMapper folderMapper;

    @Autowired
    private FileService fileService;

    // 创建文件夹（含路径生成）
    @Transactional
    public Folder createFolder(@Valid Folder folder) {
        // 1. 校验文件夹名称
        String folderName = folder.getFolderName();
        if (folderName == null || folderName.trim().isEmpty()) {
            throw new IllegalArgumentException("文件夹名称不能为空");
        }

        // 2. 过滤非法文件名字符
        String safeName = folderName.replaceAll("[\\\\/:*?\"<>|]", "");
        if (safeName.isEmpty()) {
            throw new IllegalArgumentException("文件夹名称包含非法字符");
        }
        folder.setFolderName(safeName);

        // 3. 处理父文件夹逻辑
        Long parentId = folder.getParentId();
        if (parentId == null || parentId == 0) {
            // 根文件夹处理
            folder.setParentId(null);
            folder.setFolderPath(folderName + "/");
        } else {
            // 非根文件夹处理：校验父文件夹是否存在
            Folder parentFolder = folderMapper.getFolderById(parentId);
            if (parentFolder == null) {
                throw new IllegalArgumentException("父文件夹不存在，ID: " + parentId);
            }

            // 拼接路径
            String parentPath = parentFolder.getFolderPath();
            if (!parentPath.endsWith("/")) {
                parentPath += "/";
            }
            folder.setFolderPath(parentPath + folderName + "/");

            // 校验同名文件夹
            Folder existingFolder = folderMapper.getFolderByNameAndParentId(
                    folderName, parentId
            );
            if (existingFolder != null) {
                throw new IllegalArgumentException("已存在同名文件夹，名称: " + folderName);
            }
        }

        // 4. 设置基础属性
        folder.setStatus(true);
        folder.setCreateTime(LocalDateTime.now());
        folder.setUpdateTime(LocalDateTime.now());
        folder.setIsDeleted(false);

        // 5. 插入数据库
        folderMapper.insertFolder(folder);
        return folder;
    }

    // 新增：递归创建缺失的父文件夹
    private Folder createMissingParentFolder(Long parentId) {
        // 查询父文件夹是否存在
        Folder parentFolder = folderMapper.getFolderById(parentId);
        if (parentFolder != null) {
            return parentFolder;
        }

        // 获取父文件夹的父 ID（递归关键）
        Folder parentInfo = folderMapper.getParentInfo(parentId);
        Long grandParentId = parentInfo != null ? parentInfo.getParentId() : null;

        // 递归创建上级文件夹
        Folder grandParent = null;
        if (grandParentId != null) {
            grandParent = createMissingParentFolder(grandParentId);
        }

        // 创建当前缺失的父文件夹
        Folder newParent = new Folder();
        newParent.setFolderName("自动创建的父文件夹_" + parentId);
        newParent.setParentId(grandParent != null ? grandParent.getId() : null);
        // 注意：这里需要从安全上下文中获取真实用户ID，而非硬编码
        newParent.setUserId(1L);
        return createFolder(newParent);
    }

    // 获取用户根文件夹
    public Folder getRootFolder(Long userId) {
        return folderMapper.getRootFolderByUserId(userId);
    }

    // 递归删除文件夹（含子文件和子文件夹）
    @Transactional
    public boolean deleteFolder(Long folderId,Long userId) {
        Folder folder = folderMapper.getFolderById(folderId);
        if (folder != null) {
            // 删除子文件（传递userId）
            fileService.deleteFilesByFolderId(folderId, folder.getUserId());
            // 删除子文件夹
            List<Folder> subFolders = folderMapper.getFoldersByFolderId(folderId);
            for (Folder subFolder : subFolders) {
                deleteFolder(subFolder.getId(),userId);
            }
            // 删除当前文件夹
            folderMapper.deleteFolder(folderId,userId);
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
            String parentPath = "";

            // 处理根文件夹路径
            if (folder.getParentId() != null) {
                Folder parentFolder = folderMapper.getFolderById(folder.getParentId());
                if (parentFolder != null) {
                    parentPath = parentFolder.getFolderPath();
                }
            }

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
            if (folder == null || !folder.getUserId().equals(userId)) {
                continue; // 文件夹不存在或用户不匹配，跳过
            }
            // 标记当前文件夹为已删除
            folder.setIsDeleted(true);
            folder.setDeletedTime(now);
            folder.setDeletedBy(userId);
            folder.setUpdateTime(now);
            folderMapper.updateFolder(folder);

            // 递归处理子文件夹和文件
            markSubItemsAsDeleted(folderId, userId, now);
        }
    }

    /**
     * 递归标记子文件夹和文件为已删除
     */
    private void markSubItemsAsDeleted(Long folderId, Long userId, LocalDateTime deleteTime) {
        // 1. 子文件移入回收站
        fileService.moveToRecycleBin(
                fileService.getFilesByFolderId(folderId).stream()
                        .map(File::getId)
                        .collect(Collectors.toList()),
                userId
        );

        // 2. 子文件夹递归处理
        List<Folder> subFolders = folderMapper.getFoldersByFolderId(folderId);
        for (Folder subFolder : subFolders) {
            if (!subFolder.getUserId().equals(userId)) {
                continue; // 子文件夹用户不匹配，跳过（避免越权）
            }
            subFolder.setIsDeleted(true);
            subFolder.setDeletedTime(deleteTime);
            subFolder.setDeletedBy(userId);
            subFolder.setUpdateTime(deleteTime);
            folderMapper.updateFolder(subFolder);

            // 递归处理更深层子文件夹
            markSubItemsAsDeleted(subFolder.getId(), userId, deleteTime);
        }
    }

    /**
     * 从回收站恢复文件夹（含递归恢复子文件夹/文件）
     */
    @Transactional
    public void restoreFromRecycleBin(List<Long> folderIds, Long userId) {
        for (Long folderId : folderIds) {
            Folder folder = folderMapper.getFolderById(folderId);
            if (folder == null || !folder.getUserId().equals(userId) || !folder.getIsDeleted()) {
                continue; // 文件夹不存在、用户不匹配或未删除，跳过
            }
            // 清除当前文件夹删除标记
            folder.setIsDeleted(false);
            folder.setDeletedTime(null);
            folder.setDeletedBy(null);
            folder.setUpdateTime(LocalDateTime.now());
            folderMapper.updateFolder(folder);

            // 递归恢复子文件夹和文件
            restoreSubItems(folderId, userId);
        }
    }

    /**
     * 递归恢复子文件夹和文件
     */
    private void restoreSubItems(Long folderId, Long userId) {
        // 1. 恢复子文件
        fileService.restoreFromRecycleBin(
                fileService.getFilesByFolderId(folderId).stream()
                        .map(File::getId)
                        .collect(Collectors.toList()),
                userId
        );

        // 2. 恢复子文件夹
        List<Folder> subFolders = folderMapper.getFoldersByFolderId(folderId);
        for (Folder subFolder : subFolders) {
            if (subFolder.getIsDeleted() && subFolder.getUserId().equals(userId)) {
                subFolder.setIsDeleted(false);
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
     * 彻底删除文件夹（含递归物理删除子文件夹/文件）
     */
    @Transactional
    public void deletePermanently(List<Long> folderIds, Long userId) {
        for (Long folderId : folderIds) {
            Folder folder = folderMapper.getFolderById(folderId);
            if (folder == null || !folder.getUserId().equals(userId) || !folder.getIsDeleted()) {
                continue; // 文件夹不存在、用户不匹配或未删除，跳过
            }
            // 1. 物理删除子文件
            fileService.deletePermanently(
                    fileService.getFilesByFolderId(folderId).stream()
                            .map(File::getId)
                            .collect(Collectors.toList()),
                    userId
            );

            // 2. 递归物理删除子文件夹
            List<Folder> subFolders = folderMapper.getFoldersByFolderId(folderId);
            for (Folder subFolder : subFolders) {
                deletePermanently(Arrays.asList(subFolder.getId()), userId);
            }

            // 3. 物理删除当前文件夹
            folderMapper.deleteFolder(folderId, userId);
        }
    }

    /**
     * 获取回收站文件夹列表（分页）
     */
    public List<Folder> getRecycleBinFolders(Long userId, Integer page, Integer pageSize) {
        int offset = (page - 1) * pageSize;
        return folderMapper.getRecycleBinFolders(userId, offset, pageSize);
    }

    /**
     * 获取回收站文件夹总数（用于分页计算）
     */
    public int getRecycleBinFolderCount(Long userId) {
        return folderMapper.getRecycleBinFolderCount(userId);
    }

    // ===================== 层级结构相关方法 =====================

    /**
     * 获取用户所有文件夹（用于层级结构）
     */
    public List<Folder> getAllFoldersByUserId(Long userId) {
        return folderMapper.getAllFoldersByUserId(userId);
    }

    /**
     * 获取指定文件夹下的所有子文件夹（包括子文件夹的子文件夹）
     */
    public List<Folder> getAllFoldersByFolderId(Long folderId, Long userId) {
        return folderMapper.getAllFoldersByFolderId(folderId, userId);
    }
}