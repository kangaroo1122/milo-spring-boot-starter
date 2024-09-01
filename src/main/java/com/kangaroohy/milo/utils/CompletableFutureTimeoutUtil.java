package com.kangaroohy.milo.utils;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * 类 CompletableFutureTimeoutUtil 功能描述：<br/>
 *
 * @author hy
 * @version 0.0.1
 * @date 2024/9/1 01:25
 */
public class CompletableFutureTimeoutUtil {
    private CompletableFutureTimeoutUtil() {
    }

    /**
     * 延迟类
     */
    static final class Delayer {

        static final class CompletableFutureDelaySchedulerFactory implements ThreadFactory {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("CompletableFutureDelaySchedulerFactory");
                return t;
            }
        }

        // 任务执行器
        static final ScheduledThreadPoolExecutor delayer;

        static {
            (delayer = new ScheduledThreadPoolExecutor(
                    1, new CompletableFutureTimeoutUtil.Delayer.CompletableFutureDelaySchedulerFactory())).
                    setRemoveOnCancelPolicy(true);
        }
    }

    /**
     * 通过ScheduledThreadPoolExecutor schedule定的延迟时间之后执行一次性任务
     *
     * @param timeout
     * @param unit
     * @param <T>
     * @return
     */
    public static <T> CompletableFuture<T> timeoutAfter(long timeout, TimeUnit unit) {
        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFutureTimeoutUtil.Delayer.delayer.schedule(() -> result.completeExceptionally(new TimeoutException()), timeout, unit);
        return result;
    }

    /**
     * 使用 applyToEither 方法,将 future 和 timeoutFuture 两个 CompletableFuture 合并,谁先完成就使用谁的结果
     *
     * @param defaultVale 异常发生后返回默认值
     * @param future      参与执行的业务的任务
     * @param timeout     超时时间
     * @param unit        超时单位
     * @param runnable    函数接口  可以保存执行保存日志发送Mq 消息
     * @param <T>
     * @return
     */
    public static <T> CompletableFuture<T> completeOnTimeout(T defaultVale, CompletableFuture<T> future, Double timeout, TimeUnit unit, Runnable runnable) {
        final CompletableFuture<T> timeoutFuture = timeoutAfter(timeout.longValue(), unit);
        return future.applyToEither(timeoutFuture, Function.identity()).exceptionally((throwable) -> {
            Optional.ofNullable(runnable).ifPresent(Runnable::run);
            return defaultVale;
        });
    }
}
