package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.Metric;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;

import com.alibaba.csp.sentinel.dashboard.datasource.service.MetricService;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Save metrics data to database.
 *
 * @author psikun
 */
@Component
public class InDatabaseMetricsRepository implements MetricsRepository<MetricEntity> {

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @Autowired
    private MetricService metricService;


    /**
     * 保存监控信息
     *
     * @param entity
     */
    @Override
    public void save(MetricEntity entity) {
        if (entity == null || StringUtil.isBlank(entity.getApp())) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
            // 插入数据库
            metricService.save(toPo(entity));
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * 批量保存监控信息
     *
     * @param metrics metrics to save
     */
    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        if (metrics == null) {
            return;
        }
        readWriteLock.writeLock().lock();
        try {
            List<Metric> metricList = new ArrayList<>();
            metrics.forEach(metric -> metricList.add(toPo(metric)));
            metricService.saveBatch(metricList);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * 通过应用名称（app）、资源名称（resource）、timestamp 开始时间 、timestamp 结束时间查询 metric 列表
     *
     * @param app       application name for Sentinel
     * @param resource  resource name
     * @param startTime start timestamp
     * @param endTime   end timestamp
     * @return
     */
    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource,
                                                           long startTime, long endTime) {
        List<MetricEntity> results = new ArrayList<>();

        if (StringUtil.isBlank(app)) {
            return results;
        }

        readWriteLock.readLock().lock();
        try {
            Metric metric = new Metric();
            metric.setApp(app);
            metric.setResource(resource);
            QueryWrapper<Metric> queryWrapper = new QueryWrapper<>(metric);
            queryWrapper.between("timestamp", new Date(startTime), new Date(endTime));
            List<Metric> metricList = metricService.list(queryWrapper);

            if (CollectionUtils.isEmpty(metricList)) {
                return results;
            }

            metricList.forEach(e -> results.add(toPo(e)));
            return results;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * 通过应用名称（app） 查询 MetricMapper 列表
     *
     * @param app application name
     * @return
     */
    @Override
    public List<String> listResourcesOfApp(String app) {
        List<String> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }

        final long minTimeMs = System.currentTimeMillis() - 1000 * 60;
        Map<String, MetricEntity> resourceCount = new ConcurrentHashMap<>(32);

        readWriteLock.readLock().lock();
        try {
            QueryWrapper<Metric> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("app", app);
            queryWrapper.ge("timestamp", new Date(minTimeMs));
            List<Metric> metricList = metricService.list(queryWrapper);

            List<MetricEntity> metricEntityList = new ArrayList<>();
            metricList.forEach(e -> metricEntityList.add(toPo(e)));

            if (CollectionUtils.isEmpty(metricEntityList)) {
                return results;
            }

            for (MetricEntity newEntity : metricEntityList) {
                String resource = newEntity.getResource();
                if (resourceCount.containsKey(resource)) {
                    MetricEntity oldEntity = resourceCount.get(resource);
                    oldEntity.addPassQps(newEntity.getPassQps());
                    oldEntity.addRtAndSuccessQps(newEntity.getRt(), newEntity.getSuccessQps());
                    oldEntity.addBlockQps(newEntity.getBlockQps());
                    oldEntity.addExceptionQps(newEntity.getExceptionQps());
                    oldEntity.addCount(1);
                } else {
                    resourceCount.put(resource, MetricEntity.copyOf(newEntity));
                }
            }
            // Order by last minute b_qps DESC.
            return resourceCount.entrySet()
                    .stream()
                    .sorted((o1, o2) -> {
                        MetricEntity e1 = o1.getValue();
                        MetricEntity e2 = o2.getValue();
                        int t = e2.getBlockQps().compareTo(e1.getBlockQps());
                        if (t != 0) {
                            return t;
                        }
                        return e2.getPassQps().compareTo(e1.getPassQps());
                    })
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private Metric toPo(MetricEntity metricEntity) {
        Metric metric = new Metric();
        BeanUtils.copyProperties(metricEntity, metric, Metric.class);
        return metric;
    }

    private MetricEntity toPo(Metric metric) {
        MetricEntity metricEntity = new MetricEntity();
        BeanUtils.copyProperties(metric, metricEntity, MetricEntity.class);
        return metricEntity;
    }

}
