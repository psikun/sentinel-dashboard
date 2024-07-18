package com.alibaba.csp.sentinel.dashboard.datasource.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "interval")
@Data
public class MetricIntervalProperties {

    private Long maxQueryIntervalMs;

    private Long queryIntervalMs;
}
