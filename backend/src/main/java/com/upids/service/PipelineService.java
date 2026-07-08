package com.upids.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.upids.common.result.PageResult;
import com.upids.dto.request.PageRequest;
import com.upids.dto.request.PipelineQueryRequest;
import com.upids.entity.Pipeline;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 管线服务接口
 */
public interface PipelineService extends IService<Pipeline> {

    /**
     * 批量导入管线（GeoJSON / Excel）
     *
     * @param file     上传文件
     * @param fileType 文件类型: geojson / excel
     * @return 导入数量
     */
    int importPipelines(MultipartFile file, String fileType);

    /**
     * 分页查询管线列表（支持筛选）
     */
    PageResult<Pipeline> getPipelineList(PipelineQueryRequest query);

    /**
     * 获取管线详情
     */
    Pipeline getPipelineDetail(String pipelineId);

    /**
     * 获取 GeoJSON FeatureCollection（支持 BBox 过滤）
     *
     * @param minLng 最小经度
     * @param minLat 最小纬度
     * @param maxLng 最大经度
     * @param maxLat 最大纬度
     * @return GeoJSON FeatureCollection
     */
    Map<String, Object> getGeoJSON(Double minLng, Double minLat, Double maxLng, Double maxLat);

    /**
     * 软删除管线
     */
    void deletePipeline(String pipelineId);
}
