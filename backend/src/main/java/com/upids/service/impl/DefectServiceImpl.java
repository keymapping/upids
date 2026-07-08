package com.upids.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.upids.common.exception.BusinessException;
import com.upids.common.result.PageResult;
import com.upids.entity.Defect;
import com.upids.mapper.DefectMapper;
import com.upids.service.DefectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 缺陷服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefectServiceImpl extends ServiceImpl<DefectMapper, Defect> implements DefectService {

    @Override
    public PageResult<Defect> listDefects(Integer page, Integer pageSize, String defectType,
                                           Integer severityLevel, String pipelineId) {
        LambdaQueryWrapper<Defect> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(defectType)) {
            wrapper.eq(Defect::getDefectType, defectType);
        }
        if (severityLevel != null) {
            wrapper.eq(Defect::getSeverityLevel, severityLevel);
        }
        if (StringUtils.hasText(pipelineId)) {
            wrapper.eq(Defect::getPipelineId, pipelineId);
        }
        wrapper.orderByDesc(Defect::getDetectedAt);

        Page<Defect> pageResult = page(new Page<>(page, pageSize), wrapper);
        return PageResult.of(pageResult.getRecords(), pageResult.getTotal(),
                page, pageSize);
    }

    @Override
    public Map<String, Object> getDefectsGeoJson() {
        // 使用 PostGIS 函数直接获取坐标，避免类型处理器问题
        List<Map<String, Object>> defectRows = getBaseMapper().selectDefectsWithCoords();

        List<Map<String, Object>> features = new ArrayList<>();
        for (Map<String, Object> row : defectRows) {
            Object lngObj = row.get("lng");
            Object latObj = row.get("lat");
            if (lngObj == null || latObj == null) continue;

            double lng = ((Number) lngObj).doubleValue();
            double lat = ((Number) latObj).doubleValue();
            if (lng == 0 && lat == 0) continue;

            Map<String, Object> feature = new HashMap<>();
            feature.put("type", "Feature");

            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", "Point");
            geometry.put("coordinates", new double[]{lng, lat});
            feature.put("geometry", geometry);

            Map<String, Object> properties = new HashMap<>();
            properties.put("defectId", row.get("defectId"));
            properties.put("recordId", row.get("recordId"));
            properties.put("pipelineId", row.get("pipelineId"));
            Object defectTypeObj = row.get("defectType");
            properties.put("defectType", defectTypeObj != null ? defectTypeObj.toString().toLowerCase() : null);
            properties.put("severityLevel", row.get("severityLevel"));
            properties.put("confidenceScore", row.get("confidenceScore"));
            properties.put("source", row.get("source"));
            properties.put("detectedAt", row.get("detectedAt"));
            feature.put("properties", properties);

            features.add(feature);
        }

        Map<String, Object> geoJson = new HashMap<>();
        geoJson.put("type", "FeatureCollection");
        geoJson.put("features", features);
        return geoJson;
    }

    @Override
    public Defect getDefectDetail(Long defectId) {
        Defect defect = getById(defectId);
        if (defect == null) {
            throw BusinessException.notFound("缺陷不存在: " + defectId);
        }
        return defect;
    }

    /**
     * 解析 WKT POINT 或 EWKB 格式为 GeoJSON 坐标 [lng, lat]
     */
    private double[] parseWktPoint(String wkt) {
        try {
            if (wkt == null || wkt.isEmpty()) return null;
            String trimmed = wkt.trim();
            // 检测是否为十六进制 EWKB 格式 (纯十六进制字符，长度>=18)
            if (isHexEWKB(trimmed)) {
                double[] result = parseEWKBPoint(trimmed);
                if (result != null && (result[0] != 0 || result[1] != 0)) return result;
                log.warn("EWKB parse returned [0,0] for: " + trimmed.substring(0, Math.min(40, trimmed.length())));
                return result;
            }
            // 处理 EWKT 格式: SRID=4326;POINT(...)
            if (trimmed.startsWith("SRID=")) {
                trimmed = trimmed.substring(trimmed.indexOf(';') + 1);
            }
            String upper = trimmed.toUpperCase();
            if (upper.startsWith("POINT(")) {
                String point = trimmed.substring(6, trimmed.length() - 1).trim();
                String[] parts = point.split("\\s+");
                double lng = Double.parseDouble(parts[0]);
                double lat = Double.parseDouble(parts[1]);
                return new double[]{lng, lat};
            }
            log.warn("Unrecognized geometry format: " + trimmed.substring(0, Math.min(40, trimmed.length())));
            return new double[]{0, 0};
        } catch (Exception e) {
            log.warn("Failed to parse geometry: " + wkt + " | error: " + e.getMessage());
            return new double[]{0, 0};
        }
    }

    private boolean isHexEWKB(String value) {
        if (value.length() < 18) return false;
        for (char c : value.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    private double[] parseEWKBPoint(String hex) {
        boolean littleEndian = hex.substring(0, 2).equalsIgnoreCase("01");
        String typeHex = hex.substring(2, 10);
        int geomType = littleEndian ? (int) littleEndianHexToLong(typeHex) : Integer.parseUnsignedInt(typeHex, 16);
        int offset = 10;
        if ((geomType & 0x20000000) != 0) offset += 8; // skip SRID
        if ((geomType & 0xFF) == 1) { // Point
            String xHex = hex.substring(offset, offset + 16);
            String yHex = hex.substring(offset + 16, offset + 32);
            double x = Double.longBitsToDouble(littleEndian ? littleEndianHexToLong(xHex) : Long.parseUnsignedLong(xHex, 16));
            double y = Double.longBitsToDouble(littleEndian ? littleEndianHexToLong(yHex) : Long.parseUnsignedLong(yHex, 16));
            return new double[]{x, y};
        }
        return new double[]{0, 0};
    }

    private static long littleEndianHexToLong(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = hex.length() - 2; i >= 0; i -= 2) {
            sb.append(hex, i, i + 2);
        }
        return Long.parseUnsignedLong(sb.toString(), 16);
    }
}
