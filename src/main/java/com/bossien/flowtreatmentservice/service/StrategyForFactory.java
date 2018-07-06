package com.bossien.flowtreatmentservice.service;

import org.springframework.stereotype.Service;

import java.util.*;


/**
 * 通过for循环的方式查找所有id信息
 *
 * @author gb
 */
@Service
public class StrategyForFactory extends AbstractStrategyFactory<String, List<Long>> {

    @Override
    public List<Long> calculation(String startId) {
        Set<Long> companies = new HashSet<>();
        recAllCompany(Long.parseLong(startId), companies);
        return new ArrayList<>(companies);
    }

    private void recAllCompany(long provinceId, Collection<Long> allCompanyIds) {
        List<Long> longs = cache.get("index_" + provinceId);
        if (null == longs) {
            return;
        }
        for (Long aLong : longs) {
            allCompanyIds.add(aLong);
            List<Long> longs1 = cache.get("index_" + aLong);
            if (longs1 == null) {
                continue;
            }
            for (Long aLong1 : longs1) {
                allCompanyIds.add(aLong1);
                List<Long> longs2 = cache.get("index_" + aLong1);
                if (longs2 == null) {
                    continue;
                }
                for (Long aLong2 : longs2) {
                    allCompanyIds.add(aLong2);
                    List<Long> longs3 = cache.get("index_" + aLong2);
                    if (longs3 == null) {
                        continue;
                    }
                    for (Long aLong3 : longs3) {
                        allCompanyIds.add(aLong3);
                        List<Long> longs4 = cache.get("index_" + aLong3);
                        if (longs4 == null) {
                            continue;
                        }
                        for (Long aLong4 : longs4) {
                            allCompanyIds.add(aLong4);
                            List<Long> longs5 = cache.get("index_" + aLong4);
                            if (longs5 == null) {
                                continue;
                            }
                            for (Long aLong5 : longs5) {
                                allCompanyIds.add(aLong5);
                                List<Long> long6 = cache.get("index_" + aLong5);
                                if (long6 == null) {
                                    continue;
                                }
                                for (Long aLong6 : long6) {
                                    allCompanyIds.add(aLong6);
                                    List<Long> longs7 = cache.get("index_" + aLong6);
                                    if (longs7 == null) {
                                        continue;
                                    }
                                    for (Long aLong7 : longs7) {
                                        allCompanyIds.add(aLong7);
                                        List<Long> longs8 = cache.get("index_" + aLong7);
                                        if (longs8 == null) {
                                            continue;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
