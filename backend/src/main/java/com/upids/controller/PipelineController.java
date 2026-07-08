package com.upids.controller;

import com.upids.common.result.PageResult;
import com.upids.common.result.Result;
import com.upids.dto.request.PageRequest;
import com.upids.dto.request.PipelineQueryRequest;
import com.upids.entity.Pipeline;
import com.upids.service.PipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 管线管理接口
 */
@Tag(name = "管线管理", description = "管线数据的导入、查询、删除等操作")
@RestController
@RequestMapping("/api/pipelines")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineService pipelineService;

    @Operation(summary = "批量导入管线")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public Result<Map<String, Object>> importPipelines(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件类型: geojson / excel") @RequestParam("fileType") String fileType) {
        int count = pipelineService.importPipelines(file, fileType);
        return Result.success("导入成功，共 " + count + " 条", Map.of("imported", count));
    }

    @Operation(summary = "分页查询管线列表")
    @GetMapping
    public Result<PageResult<Pipeline>> getPipelineList(PipelineQueryRequest query) {
        PageResult<Pipeline> result = pipelineService.getPipelineList(query);
        return Result.success(result);
    }

    @Operation(summary = "获取管线详情")
    @GetMapping("/{pipelineId}")
    public Result<Pipeline> getPipelineDetail(
            @Parameter(description = "管线ID") @PathVariable String pipelineId) {
        Pipeline pipeline = pipelineService.getPipelineDetail(pipelineId);
        return Result.success(pipeline);
    }

    @Operation(summary = "获取GIS GeoJSON图层数据")
    @GetMapping("/geojson")
    public Result<Map<String, Object>> getGeoJSON(
            @Parameter(description = "最小经度") @RequestParam(required = false) Double minLng,
            @Parameter(description = "最小纬度") @RequestParam(required = false) Double minLat,
            @Parameter(description = "最大经度") @RequestParam(required = false) Double maxLng,
            @Parameter(description = "最大纬度") @RequestParam(required = false) Double maxLat) {
        Map<String, Object> geoJSON = pipelineService.getGeoJSON(minLng, minLat, maxLng, maxLat);
        return Result.success(geoJSON);
    }

    @Operation(summary = "删除管线（软删除）")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{pipelineId}")
    public Result<Void> deletePipeline(
            @Parameter(description = "管线ID") @PathVariable String pipelineId) {
        pipelineService.deletePipeline(pipelineId);
        return Result.success();
    }
}
