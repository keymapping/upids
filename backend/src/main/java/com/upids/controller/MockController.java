package com.upids.controller;

import com.upids.common.result.Result;
import com.upids.service.MockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock数据控制器
 */
@Tag(name = "模拟数据", description = "模拟数据生成接口")
@RestController
@RequestMapping("/api/mock")
@RequiredArgsConstructor
public class MockController {

    private final MockService mockService;

    @Operation(summary = "生成模拟数据")
    @PostMapping("/generate")
    public Result<Map<String, Object>> generateMockData(@RequestBody(required = false) Map<String, Object> params) {
        int pipelineCount = 100;
        int inspectionCount = 500;
        double defectRatio = 0.15;

        if (params != null) {
            if (params.containsKey("pipelineCount")) {
                pipelineCount = ((Number) params.get("pipelineCount")).intValue();
            }
            if (params.containsKey("inspectionCount")) {
                inspectionCount = ((Number) params.get("inspectionCount")).intValue();
            }
            if (params.containsKey("defectRatio")) {
                defectRatio = ((Number) params.get("defectRatio")).doubleValue();
            }
        }

        Map<String, Object> data = mockService.generateMockData(pipelineCount, inspectionCount, defectRatio);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "done");
        response.put("message", String.format("成功生成 %s 条管线、%s 条检测记录、%s 条缺陷、%s 份报告",
                data.get("pipelines"), data.get("inspectionRecords"), data.get("defects"), data.get("reports")));
        response.put("pipelineCount", data.get("pipelines"));
        response.put("inspectionCount", data.get("inspectionRecords"));
        response.put("defectCount", data.get("defects"));
        response.put("reportCount", data.get("reports"));
        return Result.success(response);
    }
}
