package com.bossien.flowtreatmentservice.service;


import com.bossien.flowtreatmentservice.cache.CacheDataSource;
import com.bossien.flowtreatmentservice.cache.DataHelper;
import com.google.common.cache.Cache;
import org.springframework.util.Assert;



/**
 * @author gb
 */
public abstract class AbstractStrategyFactory<K,V> {
    /**
     * 指定的操作来源
     */
    public CacheDataSource<K,V> cache;

    /**
     * 添加来源
     * @param dataHelper
     */
    public void additionalDataSources(DataHelper<CacheDataSource<K,V>> dataHelper) {
        Assert.notNull(dataHelper, "添加来源数据");
        cache = dataHelper.loadData();
    }

    /**
     * 查询信息
     *
     * @param startId
     * @return
     */
    public abstract V calculation(K startId);
}
