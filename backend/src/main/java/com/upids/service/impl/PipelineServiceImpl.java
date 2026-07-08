package com.upids.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upids.common.exception.BusinessException;
import com.upids.common.result.PageResult;
import com.upids.dto.request.PageRequest;
import com.upids.dto.request.PipelineQueryRequest;
import com.upids.entity.Pipeline;
import com.upids.mapper.PipelineMapper;
import com.upids.service.PipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 管线服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineServiceImpl extends ServiceImpl<PipelineMapper, Pipeline> implements PipelineService {

    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importPipelines(MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("导入文件不能为空");
        }

        if (!StringUtils.hasText(fileType)) {
            throw BusinessException.badRequest("文件类型不能为空");
        }

        return switch (fileType.toLowerCase()) {
            case "geojson" -> importFromGeoJSON(file);
            case "excel" -> importFromExcel(file);
            default -> throw BusinessException.badRequest("不支持的文件类型: " + fileType + "，仅支持 geojson / excel");
        };
    }

    @Override
    public PageResult<Pipeline> getPipelineList(PipelineQueryRequest query) {
        Page<Pipeline> page = new Page<>(query.getPage(), query.getPageSize());

        LambdaQueryWrapper<Pipeline> wrapper = new LambdaQueryWrapper<Pipeline>()
                .orderByDesc(Pipeline::getCreatedAt);

        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(Pipeline::getPipelineId, query.getKeyword())
                    .or().like(Pipeline::getPipelineName, query.getKeyword()));
        }

        if (query.getMaterialType() != null && !query.getMaterialType().isBlank()) {
            wrapper.eq(Pipeline::getMaterialType, query.getMaterialType());
        }

        if (query.getRegionCode() != null && !query.getRegionCode().isBlank()) {
            wrapper.eq(Pipeline::getRegionCode, query.getRegionCode());
        }

        if (query.getStatus() != null) {
            wrapper.eq(Pipeline::getStatus, query.getStatus());
        }

        if (query.getStartTime() != null && !query.getStartTime().isBlank()) {
            wrapper.ge(Pipeline::getCreatedAt, query.getStartTime());
        }

        if (query.getEndTime() != null && !query.getEndTime().isBlank()) {
            wrapper.le(Pipeline::getCreatedAt, query.getEndTime() + " 23:59:59");
        }

        Page<Pipeline> result = page(page, wrapper);

        return PageResult.of(result.getRecords(), result.getTotal(),
                query.getPage(), query.getPageSize());
    }

    @Override
    public Pipeline getPipelineDetail(String pipelineId) {
        if (!StringUtils.hasText(pipelineId)) {
            throw BusinessException.badRequest("管线ID不能为空");
        }

        Pipeline pipeline = getById(pipelineId);
        if (pipeline == null) {
            throw BusinessException.notFound("管线不存在: " + pipelineId);
        }
        return pipeline;
    }

    @Override
    public Map<String, Object> getGeoJSON(Double minLng, Double minLat, Double maxLng, Double maxLat) {
        // 使用 PostGIS 函数直接获取 GeoJSON，绕过类型处理器
        List<Map<String, Object>> rows = getBaseMapper().selectPipelinesWithGeoJSON();

        List<Map<String, Object>> features = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object geometryObj = row.get("geometry");
            if (geometryObj == null) continue;

            // geometry 是 PostGIS ST_AsGeoJSON 返回的 JSON，MyBatis 可能返回 String 或 Map
            Map<String, Object> geometry;
            if (geometryObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> g = (Map<String, Object>) geometryObj;
                geometry = g;
            } else {
                // 如果返回的是 JSON 字符串，解析它
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> g = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(geometryObj.toString(), Map.class);
                    geometry = g;
                } catch (Exception e) {
                    continue;
                }
            }

            // BBox 过滤
            if (minLng != null && minLat != null && maxLng != null && maxLat != null) {
                if (!intersectsBBox(geometry, minLng, minLat, maxLng, maxLat)) {
                    continue;
                }
            }

            Map<String, Object> properties = new HashMap<>();
            properties.put("pipelineId", row.get("pipelineId"));
            properties.put("pipelineName", row.get("pipelineName"));
            properties.put("materialType", row.get("materialType"));
            properties.put("diameter", row.get("diameter"));
            properties.put("defectCount", row.get("defectCount"));

            Map<String, Object> feature = new HashMap<>();
            feature.put("type", "Feature");
            feature.put("geometry", geometry);
            feature.put("properties", properties);
            features.add(feature);
        }

        Map<String, Object> featureCollection = new HashMap<>();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("features", features);
        return featureCollection;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePipeline(String pipelineId) {
        if (!StringUtils.hasText(pipelineId)) {
            throw BusinessException.badRequest("管线ID不能为空");
        }

        Pipeline pipeline = getById(pipelineId);
        if (pipeline == null) {
            throw BusinessException.notFound("管线不存在: " + pipelineId);
        }

        // 软删除: 将 status 置为 0
        pipeline.setStatus(0);
        pipeline.setUpdatedAt(LocalDateTime.now());
        updateById(pipeline);
        log.info("管线已软删除: {}", pipelineId);
    }

    // ======================== GeoJSON 导入 ========================

    private int importFromGeoJSON(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            Map<String, Object> geojson = objectMapper.readValue(is, new TypeReference<>() {});

            String type = (String) geojson.get("type");
            if (!"FeatureCollection".equals(type)) {
                throw BusinessException.badRequest("无效的GeoJSON格式，需要FeatureCollection");
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> features = (List<Map<String, Object>>) geojson.get("features");
            if (features == null || features.isEmpty()) {
                throw BusinessException.badRequest("GeoJSON中没有要素");
            }

            List<Pipeline> pipelines = new ArrayList<>();
            for (Map<String, Object> feature : features) {
                Pipeline pipeline = parseGeoJSONFeature(feature);
                if (pipeline != null) {
                    pipelines.add(pipeline);
                }
            }

            if (!pipelines.isEmpty()) {
                saveBatch(pipelines);
            }

            log.info("GeoJSON导入成功，共 {} 条管线", pipelines.size());
            return pipelines.size();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("解析GeoJSON文件失败", e);
            throw BusinessException.badRequest("解析GeoJSON文件失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Pipeline parseGeoJSONFeature(Map<String, Object> feature) {
        Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
        Map<String, Object> properties = (Map<String, Object>) feature.get("properties");

        if (geometry == null || properties == null) {
            return null;
        }

        Pipeline pipeline = new Pipeline();
        pipeline.setPipelineId(getStringValue(properties, "pipelineId"));
        pipeline.setPipelineName(getStringValue(properties, "pipelineName"));
        pipeline.setMaterialType(getStringValue(properties, "materialType"));
        pipeline.setRegionCode(getStringValue(properties, "regionCode"));

        // diameter
        Object diameter = properties.get("diameter");
        if (diameter instanceof Number) {
            pipeline.setDiameter(new BigDecimal(diameter.toString()));
        }

        // installTime
        Object installTime = properties.get("installTime");
        if (installTime instanceof String) {
            pipeline.setInstallTime(LocalDate.parse((String) installTime));
        }

        // status
        Object status = properties.get("status");
        if (status instanceof Number) {
            pipeline.setStatus(((Number) status).intValue());
        } else {
            pipeline.setStatus(1); // 默认正常
        }

        // geometry -> WKT
        String wkt = geoJSONGeometryToWKT(geometry);
        pipeline.setGeoCoordinates(wkt);

        pipeline.setCreatedAt(LocalDateTime.now());
        pipeline.setUpdatedAt(LocalDateTime.now());

        return pipeline;
    }

    // ======================== Excel 导入 ========================

    private int importFromExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() <= 1) {
                throw BusinessException.badRequest("Excel文件没有数据行");
            }

            // 读取表头映射
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnMap = parseHeaderRow(headerRow);

            List<Pipeline> pipelines = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Pipeline pipeline = parseExcelRow(row, columnMap);
                if (pipeline != null && StringUtils.hasText(pipeline.getPipelineId())) {
                    pipelines.add(pipeline);
                }
            }

            if (!pipelines.isEmpty()) {
                saveBatch(pipelines);
            }

            log.info("Excel导入成功，共 {} 条管线", pipelines.size());
            return pipelines.size();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("解析Excel文件失败", e);
            throw BusinessException.badRequest("解析Excel文件失败: " + e.getMessage());
        }
    }

    private Map<String, Integer> parseHeaderRow(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String header = cell.getStringCellValue().trim().toLowerCase();
                map.put(header, i);
            }
        }
        return map;
    }

    private Pipeline parseExcelRow(Row row, Map<String, Integer> columnMap) {
        Pipeline pipeline = new Pipeline();
        pipeline.setPipelineId(getCellString(row, columnMap, "pipelineid"));
        pipeline.setPipelineName(getCellString(row, columnMap, "pipelinename"));
        pipeline.setMaterialType(getCellString(row, columnMap, "materialtype"));
        pipeline.setRegionCode(getCellString(row, columnMap, "regioncode"));

        // diameter
        String diameterStr = getCellString(row, columnMap, "diameter");
        if (StringUtils.hasText(diameterStr)) {
            try {
                pipeline.setDiameter(new BigDecimal(diameterStr));
            } catch (NumberFormatException ignored) {
            }
        }

        // installtime
        String installTimeStr = getCellString(row, columnMap, "installtime");
        if (StringUtils.hasText(installTimeStr)) {
            try {
                pipeline.setInstallTime(LocalDate.parse(installTimeStr));
            } catch (Exception ignored) {
            }
        }

        // status
        String statusStr = getCellString(row, columnMap, "status");
        if (StringUtils.hasText(statusStr)) {
            try {
                pipeline.setStatus(Integer.parseInt(statusStr));
            } catch (NumberFormatException e) {
                pipeline.setStatus(1);
            }
        } else {
            pipeline.setStatus(1);
        }

        // geocoordinates (WKT 格式)
        pipeline.setGeoCoordinates(getCellString(row, columnMap, "geocoordinates"));

        pipeline.setCreatedAt(LocalDateTime.now());
        pipeline.setUpdatedAt(LocalDateTime.now());

        return pipeline;
    }

    private String getCellString(Row row, Map<String, Integer> columnMap, String column) {
        Integer colIndex = columnMap.get(column);
        if (colIndex == null) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    // ======================== WKT / GeoJSON 转换工具 ========================

    /**
     * GeoJSON Geometry -> WKT
     * 支持 LineString / MultiLineString
     */
    @SuppressWarnings("unchecked")
    private String geoJSONGeometryToWKT(Map<String, Object> geometry) {
        String type = (String) geometry.get("type");
        Object coords = geometry.get("coordinates");

        if (coords == null) {
            return null;
        }

        if ("LineString".equals(type)) {
            List<List<Double>> coordinates = (List<List<Double>>) coords;
            StringBuilder sb = new StringBuilder("LINESTRING(");
            for (int i = 0; i < coordinates.size(); i++) {
                if (i > 0) sb.append(",");
                List<Double> coord = coordinates.get(i);
                sb.append(coord.get(0)).append(" ").append(coord.get(1));
            }
            sb.append(")");
            return sb.toString();
        } else if ("MultiLineString".equals(type)) {
            List<List<List<Double>>> coordinates = (List<List<List<Double>>>) coords;
            StringBuilder sb = new StringBuilder("MULTILINESTRING(");
            for (int i = 0; i < coordinates.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("(");
                List<List<Double>> line = coordinates.get(i);
                for (int j = 0; j < line.size(); j++) {
                    if (j > 0) sb.append(",");
                    sb.append(line.get(j).get(0)).append(" ").append(line.get(j).get(1));
                }
                sb.append(")");
            }
            sb.append(")");
            return sb.toString();
        } else if ("Point".equals(type)) {
            List<Double> coord = (List<Double>) coords;
            return "POINT(" + coord.get(0) + " " + coord.get(1) + ")";
        }

        return null;
    }

    /**
     * WKT -> GeoJSON Geometry
     */
    private Map<String, Object> wktToGeoJSONGeometry(String wkt) {
        if (wkt == null || wkt.isBlank()) {
            return null;
        }

        wkt = wkt.trim();

        // 检测是否为十六进制 EWKB 格式
        if (isHexEWKB(wkt)) {
            wkt = parseEWKBToWKT(wkt);
            if (wkt == null) return null;
        }

        wkt = wkt.toUpperCase();

        try {
            if (wkt.startsWith("LINESTRING(")) {
                String coordsStr = wkt.substring("LINESTRING(".length(), wkt.length() - 1);
                return buildLineStringGeometry(coordsStr);
            } else if (wkt.startsWith("MULTILINESTRING(")) {
                String coordsStr = wkt.substring("MULTILINESTRING(".length(), wkt.length() - 1);
                return buildMultiLineStringGeometry(coordsStr);
            } else if (wkt.startsWith("POINT(")) {
                String coordsStr = wkt.substring("POINT(".length(), wkt.length() - 1);
                return buildPointGeometry(coordsStr);
            }
        } catch (Exception e) {
            log.warn("WKT解析失败: {}", wkt, e);
        }

        return null;
    }

    private Map<String, Object> buildLineStringGeometry(String coordsStr) {
        String[] pairs = coordsStr.split(",");
        List<List<Double>> coordinates = new ArrayList<>();
        for (String pair : pairs) {
            String[] xy = pair.trim().split("\\s+");
            coordinates.add(List.of(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
        }
        Map<String, Object> geometry = new HashMap<>();
        geometry.put("type", "LineString");
        geometry.put("coordinates", coordinates);
        return geometry;
    }

    private Map<String, Object> buildMultiLineStringGeometry(String coordsStr) {
        // 处理嵌套括号: (x1 y1,x2 y2),(x3 y3,x4 y4)
        List<List<List<Double>>> allLines = new ArrayList<>();
        // 简单解析: 按 ),( 分割
        String[] lineStrs = coordsStr.split("\\)\\s*,\\s*\\(");
        for (String lineStr : lineStrs) {
            String clean = lineStr.replace("(", "").replace(")", "").trim();
            String[] pairs = clean.split(",");
            List<List<Double>> line = new ArrayList<>();
            for (String pair : pairs) {
                String[] xy = pair.trim().split("\\s+");
                if (xy.length >= 2) {
                    line.add(List.of(Double.parseDouble(xy[0]), Double.parseDouble(xy[1])));
                }
            }
            allLines.add(line);
        }

        Map<String, Object> geometry = new HashMap<>();
        geometry.put("type", "MultiLineString");
        geometry.put("coordinates", allLines);
        return geometry;
    }

    private Map<String, Object> buildPointGeometry(String coordsStr) {
        String[] xy = coordsStr.trim().split("\\s+");
        List<Double> coordinates = List.of(Double.parseDouble(xy[0]), Double.parseDouble(xy[1]));
        Map<String, Object> geometry = new HashMap<>();
        geometry.put("type", "Point");
        geometry.put("coordinates", coordinates);
        return geometry;
    }

    /**
     * 简单的 BBox 相交检测: 检查 LineString 坐标是否至少有一个点在 BBox 内
     */
    @SuppressWarnings("unchecked")
    private boolean intersectsBBox(Map<String, Object> geometry, double minLng, double minLat, double maxLng, double maxLat) {
        String type = (String) geometry.get("type");
        Object coords = geometry.get("coordinates");

        if ("Point".equals(type)) {
            List<Double> coord = (List<Double>) coords;
            return coord.get(0) >= minLng && coord.get(0) <= maxLng
                    && coord.get(1) >= minLat && coord.get(1) <= maxLat;
        } else if ("LineString".equals(type)) {
            List<List<Double>> coordinates = (List<List<Double>>) coords;
            for (List<Double> coord : coordinates) {
                if (coord.get(0) >= minLng && coord.get(0) <= maxLng
                        && coord.get(1) >= minLat && coord.get(1) <= maxLat) {
                    return true;
                }
            }
        } else if ("MultiLineString".equals(type)) {
            List<List<List<Double>>> allLines = (List<List<List<Double>>>) coords;
            for (List<List<Double>> line : allLines) {
                for (List<Double> coord : line) {
                    if (coord.get(0) >= minLng && coord.get(0) <= maxLng
                            && coord.get(1) >= minLat && coord.get(1) <= maxLat) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 检测是否为十六进制 EWKB 格式
     */
    private boolean isHexEWKB(String value) {
        if (value.length() < 18) return false;
        for (char c : value.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将 EWKB 十六进制解析为 WKT 字符串
     */
    private String parseEWKBToWKT(String hex) {
        try {
            boolean littleEndian = hex.substring(0, 2).equalsIgnoreCase("01");
            String typeHex = hex.substring(2, 10);
            int geomType = littleEndian ? (int) ewkbHexToLong(typeHex) : Integer.parseUnsignedInt(typeHex, 16);
            int offset = 10;
            if ((geomType & 0x20000000) != 0) offset += 8; // skip SRID

            int baseType = geomType & 0xFF;

            if (baseType == 1) { // Point
                double x = readEWKBDouble(hex, offset, littleEndian);
                double y = readEWKBDouble(hex, offset + 16, littleEndian);
                return String.format("POINT(%f %f)", x, y);
            } else if (baseType == 2) { // LineString
                int numPoints;
                if (littleEndian) {
                    numPoints = (int) ewkbHexToLong(hex.substring(offset, offset + 8));
                } else {
                    numPoints = Integer.parseUnsignedInt(hex.substring(offset, offset + 8), 16);
                }
                offset += 8;
                StringBuilder wkt = new StringBuilder("LINESTRING(");
                for (int i = 0; i < numPoints; i++) {
                    if (i > 0) wkt.append(",");
                    double x = readEWKBDouble(hex, offset, littleEndian);
                    double y = readEWKBDouble(hex, offset + 16, littleEndian);
                    wkt.append(String.format("%f %f", x, y));
                    offset += 32;
                }
                wkt.append(")");
                return wkt.toString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private double readEWKBDouble(String hex, int offset, boolean littleEndian) {
        String h = hex.substring(offset, offset + 16);
        long bits = littleEndian ? ewkbHexToLong(h) : Long.parseUnsignedLong(h, 16);
        return Double.longBitsToDouble(bits);
    }

    private static long ewkbHexToLong(String hex) {
        StringBuilder sb = new StringBuilder();
        for (int i = hex.length() - 2; i >= 0; i -= 2) {
            sb.append(hex, i, i + 2);
        }
        return Long.parseUnsignedLong(sb.toString(), 16);
    }
}
