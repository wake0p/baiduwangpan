package com.baidu.netdisk.fileop.Mapper;

import com.baidu.netdisk.entity.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMapper {
    // 1. 彻底删除文件（物理删除） - 保留 userId 参数用于权限校验
    int deleteFile(
            @Param("fileId") Long fileId,
            @Param("userId") Long userId
    );

    // 2. 基础查询方法
    List<File> getFilesByFolderId(@Param("folderId") Long folderId);

    File getFileById(@Param("fileId") Long fileId);

    File getFileByNameAndFolderId(
            @Param("fileName") String fileName,
            @Param("folderId") Long folderId
    );

    // 3. 去重查询（MD5）
    File getFileByMd5(@Param("fileMd5") String fileMd5);

    // 4. 批量操作
    void insertFiles(@Param("files") List<File> files);
    void insertFile(File file);

    // 5. 更新文件信息
    void updateFile(File file);

    // 6. 逻辑删除（状态更新） - 重命名方法避免冲突
    void markFileAsDeleted(
            @Param("fileId") Long fileId,
            @Param("userId") Long userId,
            @Param("isDeleted") Boolean isDeleted
    );

    void deleteFilesByFolderId(@Param("folderId") Long folderId);

    // 7. 回收站相关（标记删除）
    void updateToRecycle(
            @Param("fileIds") List<Long> fileIds,
            @Param("userId") Long userId
    );

    void restoreFromRecycle(
            @Param("fileIds") List<Long> fileIds,
            @Param("userId") Long userId
    );

    List<File> getRecycleBinFiles(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    int getRecycleBinFileCount(@Param("userId") Long userId);

    // ===================== 层级结构相关方法 =====================

    /**
     * 获取用户所有文件（用于层级结构）
     */
    List<File> getAllFilesByUserId(@Param("userId") Long userId);

    /**
     * 获取指定文件夹下的所有文件（包括子文件夹中的文件）
     */
    List<File> getAllFilesByFolderId(
            @Param("folderId") Long folderId,
            @Param("userId") Long userId
    );
}