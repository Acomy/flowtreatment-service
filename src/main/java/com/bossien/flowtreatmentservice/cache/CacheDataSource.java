package com.bossien.flowtreatmentservice.cache;

import com.google.common.collect.ImmutableMap;

import java.util.concurrent.ExecutionException;

/**
 * 缓存资源基础操作
 * @author gb
 */
public interface CacheDataSource<K,V> {

    public V get();

    public V get(K k);

    public ImmutableMap<K, V> get(Iterable<? extends K> var1) throws ExecutionException;

    public void put(K k, V v);

    public long count();

    public void clear();

    public  void print() ;
}
