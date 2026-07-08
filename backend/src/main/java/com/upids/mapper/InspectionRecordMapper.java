package com.upids.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.upids.entity.InspectionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检测记录 Mapper
 */
@Mapper
public interface InspectionRecordMapper extends BaseMapper<InspectionRecord> {
}