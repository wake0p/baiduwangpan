package com.baidu.netdisk.fileop.Mapper;

import com.baidu.netdisk.entity.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMapper {

    // 1. 基础查询方法
    List<File> getFilesByFolderId(@Param("folderId") Long folderId);

    File getFileById(@Param("fileId") Long fileId);

    File getFileByNameAndFolderId(
            @Param("fileName") String fileName,
            @Param("folderId") Long folderId
    );

    // 2. 去重查询（MD5）
    File getFileByMd5(@Param("fileMd5") String fileMd5);

    // 3. 批量操作
    void insertFiles(@Param("files") List<File> files);

    // 新增：更新文件信息（解决报错关键）
    void updateFile(File file);

    // 4. 逻辑删除（状态更新）
    void updateFileStatus(
            @Param("id") Long id,
            @Param("status") Integer status
    );

    void deleteFilesByFolderId(@Param("folderId") Long folderId);

    // 5. 新增：彻底删除文件（物理删除）
    void deleteFile(@Param("fileId") Long fileId);

    // 6. 回收站相关（标记删除）
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
}