package com.example.timetable.data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 全局线程池单例
 * 统一管理应用中所有异步操作，避免各处重复创建 ExecutorService 导致资源浪费
 *
 * 使用方式：
 * <pre>{@code
 *   AppExecutors.getInstance().diskIO().execute(() -> { ... });
 * }</pre>
 */
public class AppExecutors {

    private static volatile AppExecutors INSTANCE;

    /** 磁盘 IO / 数据库操作的串行线程池（保证写入顺序） */
    private final ExecutorService diskIO;

    private AppExecutors() {
        // 单线程池保证数据库写入操作的串行顺序，避免竞态条件
        diskIO = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Timetable-DiskIO");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            t.setDaemon(true); // 守护线程，不阻止 JVM 退出
            return t;
        });
    }

    public static AppExecutors getInstance() {
        if (INSTANCE == null) {
            synchronized (AppExecutors.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppExecutors();
                }
            }
        }
        return INSTANCE;
    }

    /** 返回用于磁盘/数据库操作的串行线程池 */
    public ExecutorService diskIO() {
        return diskIO;
    }

    /**
     * 手动关闭线程池（仅应在 Application.onTerminate 或测试清理时调用）
     * 正常情况下守护线程会自动随进程退出
     */
    public void shutdown() {
        if (!diskIO.isShutdown()) {
            diskIO.shutdown();
        }
    }
}
