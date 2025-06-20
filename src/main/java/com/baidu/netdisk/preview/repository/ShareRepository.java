package com.baidu.netdisk.preview.repository;

import com.baidu.netdisk.entity.Share;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {
    
    // 查询用户创建的分享
    List<Share> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);
    
    // 查询有效的分享（状态为1且未过期）
    @Query("SELECT s FROM Share s WHERE s.userId = :userId AND s.status = 1 AND s.expireTime > :now ORDER BY s.createTime DESC")
    List<Share> findValidSharesByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now, Pageable pageable);
    
    // 查询用户接收到的分享
    @Query("SELECT s FROM Share s WHERE s.receiverId = :userId ORDER BY s.createTime DESC")
    List<Share> findReceivedSharesByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // 根据关键词搜索分享（文件名或分享码）
    @Query("SELECT s FROM Share s JOIN NetdiskFile f ON s.fileId = f.id WHERE s.userId = :userId AND (f.fileName LIKE CONCAT('%', :query, '%') OR s.shareCode LIKE CONCAT('%', :query, '%')) ORDER BY s.createTime DESC")
    List<Share> searchMyShares(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);
    
    // 根据关键词搜索接收到的分享
    @Query("SELECT s FROM Share s JOIN NetdiskFile f ON s.fileId = f.id WHERE s.receiverId = :userId AND (f.fileName LIKE CONCAT('%', :query, '%') OR s.shareCode LIKE CONCAT('%', :query, '%')) ORDER BY s.createTime DESC")
    List<Share> searchReceivedShares(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);
    
    // 根据分享码查找分享
    Share findByShareCode(String shareCode);
} 