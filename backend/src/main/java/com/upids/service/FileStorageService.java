package com.upids.service;

import com.upids.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 文件存储服务
 */
@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "bmp");
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final DateTimeFormatter DATE_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Value("${upids.upload.path:./uploads}")
    private String uploadBasePath;

    /**
     * 存储上传文件，返回相对路径
     * 存储规则: uploads/yyyy/MM/dd/UUID.ext
     */
    public String store(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + "." + extension;

        // 按日期构建子目录
        String dateDir = LocalDate.now().format(DATE_DIR_FORMAT);
        Path targetDir = Paths.get(uploadBasePath, dateDir);

        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", targetDir, e);
            throw BusinessException.of("创建上传目录失败");
        }

        Path targetFile = targetDir.resolve(storedFilename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("存储文件失败: {}", targetFile, e);
            throw BusinessException.of("文件存储失败");
        }

        String relativePath = dateDir + "/" + storedFilename;
        log.info("文件存储成功: {}", relativePath);
        return relativePath;
    }

    /**
     * 根据相对路径加载文件为字节数组
     */
    public byte[] load(String relativePath) {
        Path filePath = Paths.get(uploadBasePath, relativePath);
        if (!Files.exists(filePath)) {
            throw BusinessException.notFound("文件不存在: " + relativePath);
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            throw BusinessException.of("读取文件失败");
        }
    }

    /**
     * 根据相对路径获取文件的 MediaType
     */
    public String getContentType(String relativePath) {
        String extension = getExtension(relativePath);
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "bmp" -> "image/bmp";
            default -> "application/octet-stream";
        };
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("上传文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw BusinessException.badRequest("文件大小不能超过20MB");
        }

        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) {
            throw BusinessException.badRequest("文件名不能为空");
        }

        String extension = getExtension(filename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw BusinessException.badRequest("不支持的文件类型: " + extension + "，仅支持 jpg, jpeg, png, bmp");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
