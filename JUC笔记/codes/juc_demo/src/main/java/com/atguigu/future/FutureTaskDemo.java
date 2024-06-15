package com.atguigu.future;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * 启动线程:start VS join
 * start 报错后不往后执行，会中断主线程
 * join 则不会中断主线程，继续往后执行
 * @Author: wujie
 * @Date: 2024/6/15 21:30
 */
public class FutureTaskDemo {
    public static void main(String[] args) throws Exception {
        //创建一个futureTask
        FutureTask<Integer> futureTask = new FutureTask<>(() -> {
            System.out.println(Thread.currentThread().getName());
            return 1;
        });
        //启动线程:start VS join
//        new Thread(futureTask, "t1").start();
        //join不会报错
        new Thread(futureTask, "t1").join();


        //获取futureTask返回值
        //1.get（阻塞）
        System.out.println(futureTask.get());
        System.out.println(futureTask.get(1, TimeUnit.SECONDS));
        //2.轮询（费cpu）
        while (true) {
            if (futureTask.isDone()) {
                System.out.println("----result: " + futureTask.get());
                break;
            } else {
                System.out.println("还在计算中，别催，越催越慢，再催熄火");
            }
        }
    }
}
