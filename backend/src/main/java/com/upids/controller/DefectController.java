package com.upids.controller;

import com.upids.common.result.PageResult;
import com.upids.common.result.Result;
import com.upids.entity.Defect;
import com.upids.service.DefectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 缺陷控制器
 */
@Tag(name = "缺陷管理", description = "缺陷查询接口")
@RestController
@RequestMapping("/api/defects")
@RequiredArgsConstructor
public class DefectController {

    private final DefectService defectService;

    @Operation(summary = "分页查询缺陷")
    @GetMapping
    public Result<PageResult<Defect>> listDefects(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String defectType,
            @RequestParam(required = false) Integer severityLevel,
            @RequestParam(required = false) String pipelineId) {
        return Result.success(defectService.listDefects(page, pageSize, defectType, severityLevel, pipelineId));
    }

    @Operation(summary = "获取GeoJSON格式缺陷数据（GIS图层）")
    @GetMapping("/geojson")
    public Result<Map<String, Object>> getDefectsGeoJson() {
        return Result.success(defectService.getDefectsGeoJson());
    }

    @Operation(summary = "获取缺陷详情")
    @GetMapping("/{defectId}")
    public Result<Defect> getDefectDetail(@PathVariable Long defectId) {
        return Result.success(defectService.getDefectDetail(defectId));
    }
}
