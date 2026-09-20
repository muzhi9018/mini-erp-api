package com.muzhi.minierp.config;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * {@code ThreadPoolFactory}: 线程池工厂
 * </p>
 *
 * @author Mr.Muzhi
 * @since 2026/9/17 17:06
 */
public final class ThreadPoolFactory {

    /**
     * Oss 线程池
     */
    private static volatile ThreadPoolExecutor ossThreadPoolExecutor;

    /**
     * 关闭 OSS 线程池，等待正在执行的任务结束。
     */
    public static synchronized void shutdownOssThreadPoolExecutor() {
        ThreadPoolExecutor executor = ossThreadPoolExecutor;
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            ossThreadPoolExecutor = null;
        }
    }

    /**
     * 获取 OSS 线程池
     * @author Mr.Muzhi
     * @since 2024/2/21 16:40
     * @return 线程池
     */
    public static ThreadPoolExecutor getOssThreadPoolExecutor() {
        if (ThreadPoolFactory.ossThreadPoolExecutor == null) {
            synchronized (ThreadPoolFactory.class) {
                if (ThreadPoolFactory.ossThreadPoolExecutor == null) {
                    // 核心线程池数量
                    int corePoolSize = 100;
                    // 最大线程池数量
                    int maximumPoolSize = 200;
                    // 空闲线程存活时长
                    long keepAliveTime = 60;
                    // 空闲线程存活时长单位
                    TimeUnit keepAliveTimeUnit = TimeUnit.SECONDS;
                    BlockingQueue<Runnable> linkedBlockingQueue = new LinkedBlockingQueue<>(30);
                    // 线程工厂
                    ThreadFactory threadFactory = new ThreadFactory() {
                        private final AtomicInteger threadNumber = new AtomicInteger(1);
                        @Override
                        public Thread newThread(Runnable runnable) {
                            Thread thread = new Thread(runnable);
                            thread.setName("minierp-oss-thread-pool-" + threadNumber.getAndIncrement());
                            return thread;
                        }
                    };
                    ThreadPoolFactory.ossThreadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveTimeUnit, linkedBlockingQueue, threadFactory);
                }
            }
        }
        return ThreadPoolFactory.ossThreadPoolExecutor;
    }
}
