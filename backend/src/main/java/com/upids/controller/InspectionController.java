package com.upids.controller;

import com.upids.common.exception.BusinessException;
import com.upids.common.result.PageResult;
import com.upids.common.result.Result;
import com.upids.dto.request.InspectionQueryRequest;
import com.upids.dto.request.PageRequest;
import com.upids.entity.InspectionRecord;
import com.upids.service.FileStorageService;
import com.upids.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 巡检管理接口
 */
@Tag(name = "巡检管理", description = "巡检图像上传、查询等操作")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService inspectionService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "上传单张巡检图像")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/inspections/upload")
    public Result<Map<String, Object>> uploadImage(
            @Parameter(description = "图像文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "管线ID") @RequestParam("pipelineId") String pipelineId) {
        Map<String, Object> result = inspectionService.uploadImage(file, pipelineId);
        return Result.success("上传成功", result);
    }

    @Operation(summary = "批量上传巡检图像（ZIP）")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/inspections/upload-batch")
    public Result<List<Map<String, Object>>> uploadBatch(
            @Parameter(description = "ZIP文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "管线ID") @RequestParam("pipelineId") String pipelineId) {
        List<Map<String, Object>> results = inspectionService.uploadBatch(file, pipelineId);
        return Result.success("批量上传成功，共 " + results.size() + " 张", results);
    }

    @Operation(summary = "分页查询巡检记录")
    @GetMapping("/inspections")
    public Result<PageResult<InspectionRecord>> getInspectionList(InspectionQueryRequest query) {
        PageResult<InspectionRecord> result = inspectionService.getInspectionList(query);
        return Result.success(result);
    }

    @Operation(summary = "获取巡检记录详情")
    @GetMapping("/inspections/{recordId}")
    public Result<InspectionRecord> getInspectionDetail(
            @Parameter(description = "记录ID") @PathVariable Long recordId) {
        InspectionRecord record = inspectionService.getInspectionDetail(recordId);
        return Result.success(record);
    }

    @Operation(summary = "获取巡检图像文件")
    @GetMapping("/files/{recordId}")
    public ResponseEntity<byte[]> getFile(
            @Parameter(description = "记录ID") @PathVariable Long recordId) {
        InspectionRecord record = inspectionService.getInspectionDetail(recordId);
        if (record == null || record.getImagePath() == null) {
            throw BusinessException.notFound("图像文件不存在");
        }

        // Mock数据占位图
        if (record.getImagePath().startsWith("mock/")) {
            String svg = String.format(
                "<svg xmlns='http://www.w3.org/2000/svg' width='800' height='600'>" +
                "<rect width='800' height='600' fill='%s'/>" +
                "<text x='400' y='270' text-anchor='middle' font-size='32' fill='#fff'>模拟检测图像</text>" +
                "<text x='400' y='320' text-anchor='middle' font-size='20' fill='#ddd'>管线: %s | 结果: %s</text>" +
                "</svg>",
                record.getDetectionResult() != null && !"normal".equals(record.getDetectionResult()) ? "#e67e22" : "#27ae60",
                record.getPipelineId(),
                record.getDetectionResult() != null ? record.getDetectionResult() : "N/A"
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/svg+xml")
                    .body(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        byte[] fileData = fileStorageService.load(record.getImagePath());
        String contentType = fileStorageService.getContentType(record.getImagePath());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + record.getImageName() + "\"")
                .body(fileData);
    }
}
