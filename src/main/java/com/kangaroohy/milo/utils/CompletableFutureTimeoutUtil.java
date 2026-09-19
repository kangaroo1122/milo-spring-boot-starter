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
     * @param timeout 超时时长
     * @param unit 超时时间单位
     * @param <T> 异步结果类型
     * @return 在超时时间后异常完成的 Future
     */
    public static <T> CompletableFuture<T> timeoutAfter(long timeout, TimeUnit unit) {
        CompletableFuture<T> result = new CompletableFuture<>();
        CompletableFutureTimeoutUtil.Delayer.delayer.schedule(() -> result.completeExceptionally(new TimeoutException()), timeout, unit);
        return result;
    }

    /**
     * 使用 applyToEither 方法,将 future 和 timeoutFuture 两个 CompletableFuture 合并,谁先完成就使用谁的结果
     *
     * @param defaultVale 异常发生后返回的默认值
     * @param future 参与执行的业务 Future
     * @param timeout 超时时间
     * @param unit 超时单位
     * @param runnable 超时或异常后的可选回调
     * @param <T> 异步结果类型
     * @return 业务 Future 和超时 Future 中先完成的结果
     */
    public static <T> CompletableFuture<T> completeOnTimeout(T defaultVale, CompletableFuture<T> future, Double timeout, TimeUnit unit, Runnable runnable) {
        final CompletableFuture<T> timeoutFuture = timeoutAfter(timeout.longValue(), unit);
        CompletableFuture<T> result = future.applyToEither(timeoutFuture, Function.identity());
        result.whenComplete((value, throwable) -> timeoutFuture.cancel(false));
        return result.exceptionally((throwable) -> {
            Optional.ofNullable(runnable).ifPresent(Runnable::run);
            return defaultVale;
        });
    }
}
