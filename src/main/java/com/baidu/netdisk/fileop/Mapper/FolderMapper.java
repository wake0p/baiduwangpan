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

    Folder getParentInfo(Long parentId);}