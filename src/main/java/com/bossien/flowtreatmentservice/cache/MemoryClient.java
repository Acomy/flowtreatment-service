package com.bossien.flowtreatmentservice.cache;


import com.bossien.flowtreatmentservice.dao.CompanyRepository;
import com.bossien.flowtreatmentservice.entity.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @Title: 内存管理客户端
 * @Description:company内存管理客户端
 * @author:gaobo
 * @Date:16:00 2018/4/25
 * @throws
 */
public enum MemoryClient {

    instance;
    /**
     * 线程 核心数量
     */
    private static final int COREPOOLSIZE = 0;
    /**
     * 最大数量
     */
    private static final int MAXPOOLSIZE = 5;
    /**
     * 线程空闲时间
     */
    private static final Long KEEPALIVETIME = 0L;
    /**
     * 线程池
     */
    private ExecutorService executors;

    CompanyRepository companyRepository;
    /**
     * 缓存公司信息
     */
    private CacheManager<Long, Company> companyCache;
    /**
     * 缓存公司id 信息减小缓存
     */
    private CacheManager<String, List<Long>> companyIdsCache;

    private CacheManager<String, List<Long>> companyIdsCacheReverse;


    public MemoryClient addCompanyRepository(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
        return this;
    }

    public MemoryClient addCompanyCache(CacheManager<Long, Company> companyCache) {
        this.companyCache = companyCache;
        return this;
    }

    public MemoryClient addCompanyIdsCache(CacheManager<String, List<Long>> companyIdsCache) {
        this.companyIdsCache = companyIdsCache;
        return this;
    }
    public MemoryClient addCompanyIdsCacheReverse(CacheManager<String, List<Long>> companyIdsCacheReverse) {
        this.companyIdsCacheReverse = companyIdsCacheReverse;
        return this;
    }

    public MemoryClient build() {
        Assert.notNull(companyRepository, "mongo 操作为空!!!");
        Assert.notNull(companyCache, "内存区1 操作为空!!!");
        Assert.notNull(companyIdsCache, "内存区(2)索引 操作为空!!!");
        executors = createExecutors();
        return this;
    }

    public void refresh() {
        if (executors != null) {
            if (!executors.isShutdown()) {
                executors.shutdownNow();
            }
            executors = null;
        }
        executors = createExecutors();
        companyIdsCache.clear();
        companyCache.clear();
        companyIdsCacheReverse.clear();
        loadDataIntoMem();
    }

    public void loadDataIntoMem() {
        final List<Company> byAndGroupByHql = companyRepository.findByAndGroupByHql();
        CopyOnWriteArrayList<Company> safeCompanyList = new CopyOnWriteArrayList(byAndGroupByHql);
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (final Company company : safeCompanyList) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    Long id = company.getPid();
                    List<Company> byPidIs = companyRepository.findByPidIs(id);
                    List<Long> longList = new ArrayList<>();
                    for (Company byPidI : byPidIs) {
                        longList.add(byPidI.getId());
                    }
                    company.setChild(byPidIs);
                    companyCache.put(id, company);
                    companyIdsCache.put("index_" + id, longList);
                }
            });
        }
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(25, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            List<Company> byHql = companyRepository.findByHql();
            recCompany(byHql);
        } catch (InterruptedException e) {
            System.out.println("awaitTermination interrupted: " + e);
            executorService.shutdownNow();
        } finally {
            System.out.println("内存区1数量:" + companyCache.count());
            System.out.println("内存区2数量:" + companyIdsCache.count());
            System.out.println("内存区3数量:" + companyIdsCacheReverse.count());
        }
    }

    private void recCompany(List<Company> companies) {
        Map<Long,Long> maps =new HashMap<>();
        for (Company company : companies) {
            Long id = company.getId();
            Long pid = company.getPid();
            maps.put(id,pid);
        }
        for (Company company : companies) {
            List<Long> ids =new ArrayList<>();
            recIds(maps,company.getId(),ids);
            companyIdsCacheReverse.put("index_"+company.getId(),ids);
        }
    }

   private void recIds(Map<Long,Long> maps,Long startId,List<Long> ids){
       Long pid = maps.get(startId);
       if(pid!=null){
           ids.add(pid);
           recIds(maps,pid,ids);
       }
   }
    private ExecutorService createExecutors() {
        return new ThreadPoolExecutor(COREPOOLSIZE, MAXPOOLSIZE,
                KEEPALIVETIME, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            int threadSeq = 0;

            @Override
            public Thread newThread(Runnable run) {
                Thread t = new Thread(run, "statistics:" + "-Thread-" + threadSeq);
                threadSeq++;
                return t;
            }
        });
    }

    public CacheManager<Long, Company> getCompanyCache() {
        return companyCache;
    }

    public CacheManager<String, List<Long>> getCompanyIdsCache() {
        return companyIdsCache;
    }

    public CacheManager<String, List<Long>> getCompanyIdsCacheReverse() {
        return companyIdsCacheReverse;
    }
}
