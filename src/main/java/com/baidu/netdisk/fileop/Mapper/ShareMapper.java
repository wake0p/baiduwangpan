package com.baidu.netdisk.fileop.Mapper;

import com.baidu.netdisk.entity.Share;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShareMapper {
    // 新增分享记录
    void insertShare(Share share);

    // 根据文件ID查询分享记录
    List<Share> getSharesByFileId(@Param("fileId") Long fileId);

    // 根据ID查询分享详情
    Share getShareById(@Param("id") Long id);

    // 删除分享记录
    void deleteShare(@Param("id") Long id);

    // 更新分享状态（如过期后标记失效）
    void updateShareStatus(@Param("id") Long id, @Param("status") Integer status);
}