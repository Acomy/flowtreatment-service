package com.bossien.flowtreatmentservice.cache;

import com.google.common.cache.*;
import com.google.common.collect.ImmutableMap;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理者
 *
 * @author gb
 */
public class CacheManager<K, V> implements CacheDataSource<K, V> {

    private DataHelper<V> dataHelper;

    private Cache<K, V> totalCache;

    private CacheBuilder<K, V> kvCacheBuilder;

    private CacheManager() {
        kvCacheBuilder = CacheBuilder.newBuilder()
                .concurrencyLevel(16)
                .initialCapacity(500)
                .maximumSize(Integer.MAX_VALUE / 5)
                .recordStats()
                .removalListener(new RemovalListener<K, V>() {
                    @Override
                    public void onRemoval(RemovalNotification<K, V> notification) {
                        System.out.println(notification.getKey() + "removed|cause by" + notification.getCause());
                    }
                });
        totalCache = kvCacheBuilder.build();

    }

    public static CacheManager newCache() {
        return new CacheManager();
    }

    public void initDataFill(final DataHelper<V> dataHelper) {
        this.dataHelper = dataHelper;
        totalCache = kvCacheBuilder.build(new CacheLoader<K, V>() {
                                              @Override
                                              public V load(K k) throws Exception {
                                                  return dataHelper.loadData();
                                              }
                                          }
        );
    }

    public V get(K k, Callable<? extends V> var2) {
        V v = null;
        try {
            v = totalCache.get(k, var2);
        } catch (ExecutionException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        } finally {
            return v;
        }

    }

    @Override
    public V get() {
        return null;
    }

    @Override
    public V get(K k) {
        return totalCache.getIfPresent(k);
    }

    @Override
    public ImmutableMap<K, V> get(Iterable<? extends K> var1) throws ExecutionException {
        return totalCache.getAllPresent(var1);

    }

    @Override
    public void put(K k, V v) {
        totalCache.put(k, v);
    }

    @Override
    public long count() {
        return totalCache.size();
    }

    @Override
    public void clear() {
        totalCache.cleanUp();
    }

    @Override
    public void print() {
        totalCache.toString();
    }
}
