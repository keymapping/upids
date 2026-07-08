package com.upids.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.upids.common.result.PageResult;
import com.upids.dto.request.InspectionQueryRequest;
import com.upids.dto.request.PageRequest;
import com.upids.entity.InspectionRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 巡检服务接口
 */
public interface InspectionService extends IService<InspectionRecord> {

    /**
     * 上传单张巡检图像，立即返回结果并异步创建检测任务
     *
     * @param file       图像文件
     * @param pipelineId 关联管线ID
     * @return { recordId, taskId, imagePath, taskStatus }
     */
    Map<String, Object> uploadImage(MultipartFile file, String pipelineId);

    /**
     * 批量上传（ZIP 包），解压后逐张处理
     *
     * @param file       ZIP 文件
     * @param pipelineId 关联管线ID
     * @return 每张图像的处理结果列表
     */
    List<Map<String, Object>> uploadBatch(MultipartFile file, String pipelineId);

    /**
     * 分页查询巡检记录（支持筛选）
     */
    PageResult<InspectionRecord> getInspectionList(InspectionQueryRequest query);

    /**
     * 获取巡检记录详情
     */
    InspectionRecord getInspectionDetail(Long recordId);
}
