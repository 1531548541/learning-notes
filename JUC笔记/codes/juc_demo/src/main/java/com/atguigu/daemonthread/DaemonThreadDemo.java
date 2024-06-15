package com.atguigu.daemonthread;

/**
 * 守护线程 VS 用户线程
 *
 * 守护线程：为了守护用户线程而存在，当所有用户线程运行完毕后，自动结束，然后JVM才正常退出。【主线程不会等守护线程执行结束】
 * 用户线程：普通线程，【主线程需要等用户线程执行完才能结束】。
 * @Author: wujie
 * @Date: 2024/6/15 21:30
 */
public class DaemonThreadDemo {
    public static void main(String[] args) {
        //普通线程
        new Thread(() -> {
            Thread currentThread = Thread.currentThread();
            String log = String.format("线程名称:%s，线程类型：%s", currentThread.getName(), currentThread.isDaemon() ? "守护线程" : "用户线程");
            System.out.println(log);
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        },"张聪聪线程").start();
        //守护线程
        Thread daemonThread = new Thread(() -> {
            Thread currentThread = Thread.currentThread();
            String log = String.format("线程名称:%s，线程类型：%s", currentThread.getName(), currentThread.isDaemon() ? "守护线程" : "用户线程");
            System.out.println(log);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "守护线程");
        daemonThread.setDaemon(true);
        daemonThread.start();


        System.out.println(Thread.currentThread().getName()+"\t"+" ----task is over");
    }
}
