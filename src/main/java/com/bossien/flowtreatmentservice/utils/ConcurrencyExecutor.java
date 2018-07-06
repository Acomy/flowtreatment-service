package com.bossien.flowtreatmentservice.utils;


import java.util.concurrent.*;

/**
 * 并发帮助类
 */
public class ConcurrencyExecutor {


    /**
     * 线程 核心数量
     */
    private int defaultCorePoolSize = 0;
    /**
     * 最大数量
     */
    private int defaultMaxPoolSize = 5;
    /**
     * 线程空闲时间
     */
    private Long defaultKeepAliveTime = 0L;
    /**
     * 线程池
     */
    private ExecutorService executors;

    private String name = "ConcurrencyExecutor";

    public void setName(String name) {
        this.name = name;
    }

    public ConcurrencyExecutor(int corePoolSize, int maxPoolSize, Long keepAliveTime) {
        this.defaultCorePoolSize = corePoolSize;
        this.defaultMaxPoolSize = maxPoolSize;
        this.defaultKeepAliveTime = keepAliveTime;
        createPool(corePoolSize, maxPoolSize, keepAliveTime);
    }

    private void createPool(int corePoolSize, int maxPoolSize, Long keepAliveTime) {
        executors = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                keepAliveTime, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>()
                , new ThreadFactory() {
            int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(name + (count++));
                return thread;
            }
        });
    }

    public ConcurrencyExecutor() {
        createPool(defaultCorePoolSize, defaultMaxPoolSize, defaultKeepAliveTime);
    }

    public static ConcurrencyExecutor newConcurrencyExecutor(int corePoolSize, int maxPoolSize, Long keepAliveTime) {
        ConcurrencyExecutor concurrencyExecutor = new ConcurrencyExecutor(corePoolSize, maxPoolSize, keepAliveTime);
        return concurrencyExecutor;
    }

    public static ConcurrencyExecutor newConcurrencyExecutor() {
        ConcurrencyExecutor concurrencyExecutor = new ConcurrencyExecutor();
        return concurrencyExecutor;
    }

    /**
     * 直接使用clear,会导致未完成的任务.在safeStop后使用
     */
    public void clear() {
        executors.shutdownNow();
        executors = null;
        createPool(defaultCorePoolSize, defaultMaxPoolSize, defaultKeepAliveTime);
    }

    public void run(Runnable runnable) {
        executors.submit(runnable);
    }

    public void safeStop(long timeout, TimeUnit unit) {
        executors.shutdown();
        try {
            while (!executors.awaitTermination(timeout, unit)) {
                System.out.println("线程池没有关闭");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("线程池已经关闭");
    }

}
