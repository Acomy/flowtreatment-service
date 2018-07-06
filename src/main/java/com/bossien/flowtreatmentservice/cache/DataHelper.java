package com.bossien.flowtreatmentservice.cache;

/**
 * 填充数据类型
 * @author gb
 */
public interface DataHelper<T> {
    /**
     * 加载数据
     * @return
     */
   T loadData();
}
