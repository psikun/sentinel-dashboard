package com.alibaba.csp.sentinel.dashboard.datasource.mapper;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.Metric;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MetricMapper extends BaseMapper<Metric> {
}
