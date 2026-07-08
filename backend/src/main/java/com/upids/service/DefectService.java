package com.upids.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.upids.common.result.PageResult;
import com.upids.entity.Defect;

import java.util.List;
import java.util.Map;

/**
 * 缺陷服务接口
 */
public interface DefectService extends IService<Defect> {

    /**
     * 分页查询缺陷
     */
    PageResult<Defect> listDefects(Integer page, Integer pageSize, String defectType,
                                    Integer severityLevel, String pipelineId);

    /**
     * 获取GeoJSON格式的缺陷数据（GIS图层）
     */
    Map<String, Object> getDefectsGeoJson();

    /**
     * 获取缺陷详情
     */
    Defect getDefectDetail(Long defectId);
}
