package com.alibaba.csp.sentinel.dashboard.datasource.service;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.Metric;
import com.alibaba.csp.sentinel.dashboard.datasource.mapper.MetricMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class MetricService extends ServiceImpl<MetricMapper, Metric> {
}
