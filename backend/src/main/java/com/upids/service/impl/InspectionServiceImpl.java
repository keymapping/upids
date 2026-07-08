package com.upids.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.upids.common.enums.TaskStatusEnum;
import com.upids.common.exception.BusinessException;
import com.upids.common.result.PageResult;
import com.upids.dto.request.InspectionQueryRequest;
import com.upids.dto.request.PageRequest;
import com.upids.entity.DetectionTask;
import com.upids.entity.InspectionRecord;
import com.upids.mapper.DetectionTaskMapper;
import com.upids.mapper.InspectionRecordMapper;
import com.upids.queue.DetectionTaskConsumer;
import com.upids.service.FileStorageService;
import com.upids.service.InspectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 巡检服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionServiceImpl extends ServiceImpl<InspectionRecordMapper, InspectionRecord> implements InspectionService {

    private final FileStorageService fileStorageService;
    private final DetectionTaskMapper detectionTaskMapper;
    private final DetectionTaskConsumer detectionTaskConsumer;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "bmp");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> uploadImage(MultipartFile file, String pipelineId) {
        // 1. 存储文件
        String relativePath = fileStorageService.store(file);

        // 2. 保存巡检记录
        Long currentUserId = getCurrentUserId();

        InspectionRecord record = new InspectionRecord();
        record.setPipelineId(pipelineId);
        record.setUserId(currentUserId);
        record.setImagePath(relativePath);
        record.setImageName(file.getOriginalFilename());
        record.setInspectTime(LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        save(record);

        // 3. 创建检测任务
        DetectionTask task = new DetectionTask();
        task.setRecordId(record.getRecordId());
        task.setStatus(TaskStatusEnum.PENDING);
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        detectionTaskMapper.insert(task);

        // 4. 异步入队
        detectionTaskConsumer.enqueue(task.getTaskId());

        // 5. 构造返回
        Map<String, Object> result = new HashMap<>();
        result.put("recordId", record.getRecordId());
        result.put("taskId", task.getTaskId());
        result.put("imagePath", relativePath);
        result.put("taskStatus", task.getStatus().getValue());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> uploadBatch(MultipartFile file, String pipelineId) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("上传文件不能为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
            throw BusinessException.badRequest("批量上传仅支持ZIP格式");
        }

        List<Map<String, Object>> results = new ArrayList<>();

        // 解压到临时目录
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("upids-batch-");
        } catch (IOException e) {
            throw BusinessException.of("创建临时目录失败");
        }

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String entryName = entry.getName();
                String ext = getExtension(entryName).toLowerCase();

                if (!IMAGE_EXTENSIONS.contains(ext)) {
                    log.warn("跳过非图片文件: {}", entryName);
                    continue;
                }

                // 读取 ZIP 条目到临时文件
                Path tempFile = tempDir.resolve(UUID.randomUUID() + "." + ext);
                try (OutputStream os = Files.newOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        os.write(buffer, 0, len);
                    }
                }

                // 转为 MultipartFile 处理
                byte[] fileBytes = Files.readAllBytes(tempFile);
                MultipartFile imageFile = new InMemoryMultipartFile(
                        "file", entryName, "image/" + ext, fileBytes);

                Map<String, Object> result = uploadImage(imageFile, pipelineId);
                results.add(result);

                // 清理临时文件
                Files.deleteIfExists(tempFile);
            }
        } catch (IOException e) {
            log.error("解压ZIP文件失败", e);
            throw BusinessException.badRequest("解压ZIP文件失败: " + e.getMessage());
        } finally {
            // 清理临时目录
            try {
                Files.deleteIfExists(tempDir);
            } catch (IOException ignored) {
            }
        }

        log.info("批量上传完成，共处理 {} 张图像", results.size());
        return results;
    }

    @Override
    public PageResult<InspectionRecord> getInspectionList(InspectionQueryRequest query) {
        Page<InspectionRecord> page = new Page<>(query.getPage(), query.getPageSize());

        LambdaQueryWrapper<InspectionRecord> wrapper = new LambdaQueryWrapper<InspectionRecord>()
                .orderByDesc(InspectionRecord::getCreatedAt);

        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(InspectionRecord::getPipelineId, query.getKeyword())
                    .or().like(InspectionRecord::getImageName, query.getKeyword()));
        }

        if (query.getDetectionResult() != null && !query.getDetectionResult().isBlank()) {
            wrapper.eq(InspectionRecord::getDetectionResult, query.getDetectionResult());
        }

        if (query.getStartTime() != null && !query.getStartTime().isBlank()) {
            wrapper.ge(InspectionRecord::getCreatedAt, query.getStartTime());
        }

        if (query.getEndTime() != null && !query.getEndTime().isBlank()) {
            wrapper.le(InspectionRecord::getCreatedAt, query.getEndTime() + " 23:59:59");
        }

        Page<InspectionRecord> result = page(page, wrapper);

        return PageResult.of(result.getRecords(), result.getTotal(),
                query.getPage(), query.getPageSize());
    }

    @Override
    public InspectionRecord getInspectionDetail(Long recordId) {
        if (recordId == null) {
            throw BusinessException.badRequest("记录ID不能为空");
        }

        InspectionRecord record = getById(recordId);
        if (record == null) {
            throw BusinessException.notFound("巡检记录不存在: " + recordId);
        }
        return record;
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * 简单的内存 MultipartFile 实现，用于 ZIP 解压后的文件处理
     */
    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getOriginalFilename() { return originalFilename; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return content == null || content.length == 0; }

        @Override
        public long getSize() { return content.length; }

        @Override
        public byte[] getBytes() { return content; }

        @Override
        public InputStream getInputStream() { return new ByteArrayInputStream(content); }

        @Override
        public void transferTo(File dest) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}
