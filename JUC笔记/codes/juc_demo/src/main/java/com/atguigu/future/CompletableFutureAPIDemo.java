package com.atguigu.future;

import java.sql.Time;
import java.util.concurrent.*;

/**
 * 创建 CompletableFuture
 * 【静态工厂方法】
 * <p>
 * CompletableFuture<Void> runAsync(Runnable runnable)
 * <p>
 * 使用默认的 ForkJoinPool 异步运行一个任务，不返回结果。
 * <p>
 * CompletableFuture<Void> runAsync(Runnable runnable, Executor executor)
 * <p>
 * 使用指定的 Executor 异步运行一个任务，不返回结果。
 * <p>
 * CompletableFuture<T> supplyAsync(Supplier<T> supplier)
 * <p>
 * 使用默认的 ForkJoinPool 异步执行一个任务，并返回结果。
 * <p>
 * CompletableFuture<T> supplyAsync(Supplier<T> supplier, Executor executor)
 * <p>
 * 使用指定的 Executor 异步执行一个任务，并返回结果。
 * <p>
 * 【手动完成】
 * <p>
 * CompletableFuture<T> completedFuture(T value)
 * <p>
 * 返回一个已完成的 CompletableFuture，其结果是给定的值。
 * <p>
 * CompletableFuture<T> complete(T value)
 * <p>
 * 手动完成 CompletableFuture，并以给定值作为结果。
 * <p>
 * 组合多个 CompletableFuture
 * <p>
 * CompletableFuture<Void> allOf(CompletableFuture<?>... cfs)
 * <p>
 * 返回一个新的 CompletableFuture，当所有给定的 CompletableFuture 都完成时完成。
 * <p>
 * CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs)
 * <p>
 * 返回一个新的 CompletableFuture，当任意一个给定的 CompletableFuture 完成时完成。
 * <p>
 * 【链式操作】
 * thenApply/thenApplyAsync
 * <p>
 * CompletableFuture<U> thenApply(Function<? super T,? extends U> fn)
 * <p>
 * 当这个 CompletableFuture 完成时应用一个函数，并返回一个新的 CompletableFuture。
 * <p>
 * CompletableFuture<U> thenApplyAsync(Function<? super T,? extends U> fn)
 * <p>
 * 异步地应用一个函数，并返回一个新的 CompletableFuture。
 * <p>
 * thenAccept/thenAcceptAsync
 * <p>
 * CompletableFuture<Void> thenAccept(Consumer<? super T> action)
 * <p>
 * 当这个 CompletableFuture 完成时消费结果。
 * <p>
 * CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action)
 * <p>
 * 异步地消费结果。
 * <p>
 * thenRun/thenRunAsync
 * <p>
 * CompletableFuture<Void> thenRun(Runnable action)
 * <p>
 * 当这个 CompletableFuture 完成时运行一个指定的 Runnable，不关心结果。
 * <p>
 * CompletableFuture<Void> thenRunAsync(Runnable action)
 * <p>
 * 异步地运行一个指定的 Runnable，不关心结果。
 * <p>
 * thenCompose/thenComposeAsync
 * <p>
 * CompletableFuture<U> thenCompose(Function<? super T,? extends CompletionStage<U>> fn)
 * <p>
 * 当这个 CompletableFuture 完成时应用一个函数，该函数返回另一个 CompletionStage，并返回一个新的 CompletableFuture。
 * <p>
 * CompletableFuture<U> thenComposeAsync(Function<? super T,? extends CompletionStage<U>> fn)
 * <p>
 * 异步地应用一个函数，该函数返回另一个 CompletionStage，并返回一个新的 CompletableFuture。
 * <p>
 * handle/handleAsync
 * <p>
 * CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
 * <p>
 * 当这个 CompletableFuture 完成（正常或异常）时应用一个函数，并返回一个新的 CompletableFuture。
 * <p>
 * CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn)
 * <p>
 * 异步地应用一个函数，并返回一个新的 CompletableFuture。
 * <p>
 * exceptionally
 * <p>
 * CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn)
 * 当这个 CompletableFuture 完成异常时应用一个函数，并返回一个新的 CompletableFuture。
 *
 * @Author: wujie
 * @Date: 2024/6/15 21:30
 */
public class CompletableFutureAPIDemo {
    public static void main(String[] args) throws Exception {
        m5();
    }

    /**
     * 异步执行步骤1，再执行步骤2，最后都完成后执行步骤3，同时返回future结果，其中由exceptionally捕获异常
     */
    private static void m0() {
        Integer futureRes = CompletableFuture
                .supplyAsync(() -> {
                    return 1;
                })
                .thenApply(res -> res + 1)
                .whenComplete((res, e) -> {
                    if (e == null) {
                        System.out.println(String.format("线程名:%s,result:%s", Thread.currentThread().getName(), res));
                    }
                }).exceptionally(e -> {
                    e.printStackTrace();
                    return 500;
                }).join();
        System.out.println(futureRes);
    }

    /**
     * 异步获得结果和触发计算
     */
    public static void m1() throws Exception {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 20, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(50), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            System.out.println(String.format("%s 线程执行了", Thread.currentThread().getName()));
            return 1;
        }, threadPoolExecutor);

        //获得结果
//        System.out.println(completableFuture.get());
//        System.out.println(completableFuture.get(2,TimeUnit.SECONDS));
        //立即返回一个结果，若还没得到future结果，则给默认值
//        System.out.println(completableFuture.getNow(9999));
        //手动完成
        System.out.println(completableFuture.complete(1));
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        threadPoolExecutor.shutdown();
    }

    /**
     * handle对结果进行处理
     */
    private static void m2() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 20, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(50), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        CompletableFuture
                .supplyAsync(() -> 1, threadPoolExecutor)
                .handle((res, e) -> {
                    System.out.println("handle1");
                    return res + 1;
                })
                .handle((res, e) -> {
                    System.out.println("handle2");
                    return res + 1;
                })
                .handle((res, e) -> {
                    System.out.println("handle3");
                    return res + 1;
                })
                .whenComplete((res, e) -> {
                    System.out.println("完成-结果:" + res);
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return 500;
                }).join();
        threadPoolExecutor.shutdown();
    }

    /**
     * thenCombine
     * 执行a后执行b，a和b的结果传到combine中
     */
    private static void m3() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 20, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(50), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        Integer res = CompletableFuture
                .supplyAsync(() -> 1, threadPoolExecutor)
                .thenCombine(CompletableFuture.supplyAsync(() -> 2, threadPoolExecutor), (r1, r2) -> r1 + r2)
                .join();
        System.out.println(res);
        threadPoolExecutor.shutdown();
    }

    /**
     * applyToEither
     * 执行a完成后执行b，把b的结果传入
     */
    private static void m4() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 20, 1L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(50), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        Integer res = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return 1;
                }, threadPoolExecutor)
                .applyToEither(CompletableFuture.supplyAsync(() -> 2, threadPoolExecutor), r -> r )
                .join();
        System.out.println(res);
        try { TimeUnit.SECONDS.sleep(3); } catch (InterruptedException e) { e.printStackTrace(); }

        threadPoolExecutor.shutdown();
    }

    /**
     * 对计算结果进行消费
     */
    public static void m5()
    {
        CompletableFuture.supplyAsync(() -> {
            return 1;
        }).thenApply(f -> {
            return f+2;
        }).thenApply(f -> {
            return f+3;
        }).thenAccept(r -> System.out.println(r));


        System.out.println(CompletableFuture.supplyAsync(() -> "resultA").thenRun(() -> {}).join());


        System.out.println(CompletableFuture.supplyAsync(() -> "resultA").thenAccept(resultA -> {}).join());


        System.out.println(CompletableFuture.supplyAsync(() -> "resultA").thenApply(resultA -> resultA + " resultB").join());
    }
}
