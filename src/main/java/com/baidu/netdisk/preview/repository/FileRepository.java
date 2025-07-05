package com.baidu.netdisk.preview.repository;

import com.baidu.netdisk.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    // 仅保留基础的JpaRepository方法，用于通过ID查询文件
}