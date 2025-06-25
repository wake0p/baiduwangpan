# 百度网盘系统本地测试指南

## 环境准备

1. 确保已安装JDK 8或以上版本
2. 确保已安装Maven
3. 确保已安装MySQL 8.0或以上版本

## 数据库设置

1. 创建数据库
```sql
CREATE DATABASE IF NOT EXISTS netdisk DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置用户权限（如果需要）
```sql
CREATE USER 'netdisk_user'@'localhost' IDENTIFIED BY 'yourpassword';
GRANT ALL PRIVILEGES ON netdisk.* TO 'netdisk_user'@'localhost';
FLUSH PRIVILEGES;
```

3. 修改application.properties文件中的数据库配置
```
spring.datasource.url=jdbc:mysql://localhost:3306/netdisk?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root   # 或者使用你创建的用户
spring.datasource.password=root   # 替换为你的密码
```

4. 确保创建上传文件的目录
```
# 对应application.properties中的配置
file.upload.dir=D:/netdisk/uploads
```
如果是不同的操作系统，请根据情况修改路径格式。

## 构建和运行

1. 编译项目
```bash
mvn clean package
```

2. 运行项目
```bash
mvn spring-boot:run
```
或
```bash
java -jar target/Netdisk-0.0.1-SNAPSHOT.jar
```

3. 访问系统
在浏览器中访问: http://localhost:8080

## 测试功能

### 用户相关
- 注册/登录
- 个人设置（头像修改、密码修改、电话修改）
- 查看存储空间使用情况

### 文件管理
- 上传/下载文件
- 文件分类浏览
- 文件收藏/取消收藏
- 文件搜索

### 分享功能
- 创建分享链接
- 管理分享（我的分享、收到的分享）
- 复制分享链接
- 查看分享详情
- 清理无效分享 