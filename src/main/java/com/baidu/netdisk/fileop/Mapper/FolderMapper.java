package com.baidu.netdisk.fileop.Mapper;

import com.baidu.netdisk.entity.Folder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FolderMapper {

    // 1. 基础查询
    Folder getRootFolderByUserId(@Param("userId") Long userId);

    Folder getFolderById(@Param("folderId") Long folderId);

    List<Folder> getFoldersByFolderId(@Param("folderId") Long folderId);

    Folder getFolderByNameAndParentId(
            @Param("folderName") String folderName,
            @Param("parentId") Long parentId
    );

    // 2. 递归查询（目录树）
    List<Folder> getFolderTreeByParentId(@Param("parentId") Long parentId);

    // 3. 逻辑删除 - 修改 status 参数类型为 Boolean
    void updateFolderStatus(
            @Param("id") Long id,
            @Param("status") Boolean status
    );

    void deleteFoldersByParentId(@Param("parentId") Long parentId);

    // 4. 增删改
    void insertFolder(Folder folder);

    void deleteFolder(
            @Param("folderId") Long folderId,
            @Param("userId") Long userId
    );

    void updateFolder(Folder folder);

    // 5. 回收站查询（分页）
    List<Folder> getRecycleBinFolders(
            @Param("userId") Long userId,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    // 回收站文件夹总数（用于分页计算）
    int getRecycleBinFolderCount(@Param("userId") Long userId);

    Folder getParentInfo(Long parentId);

    // ===================== 层级结构相关方法 =====================

    /**
     * 获取用户所有文件夹（用于层级结构）
     */
    List<Folder> getAllFoldersByUserId(@Param("userId") Long userId);

    /**
     * 获取指定文件夹下的所有子文件夹（包括子文件夹的子文件夹）
     */
    List<Folder> getAllFoldersByFolderId(
            @Param("folderId") Long folderId,
            @Param("userId") Long userId
    );
}