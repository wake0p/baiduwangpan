package com.baidu.netdisk.preview.service;

import com.baidu.netdisk.entity.File;

public interface FileService {
    File getFileById(Long fileId);
    byte[] previewFile(Long fileId);
}