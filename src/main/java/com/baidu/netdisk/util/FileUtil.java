package com.baidu.netdisk.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtil {

    //文件上传
    public static boolean uploadFile(MultipartFile file, String filePath) {
        try {
            byte[] bytes = file.getBytes();
            Path path = Paths.get(filePath);
            Files.write(path, bytes);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    //文件下载
    public static void downloadFile(String filePath, OutputStream outputStream) {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 文件删除
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }
    // 文件重命名
    public static boolean renameFile(String oldPath, String newPath) {
        File oldFile = new File(oldPath);
        File newFile = new File(newPath);
        return oldFile.exists() && oldFile.renameTo(newFile);
    }


}
