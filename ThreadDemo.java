import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ThreadDemo {

    private static void demo1() {
        Thread A = new Thread(new Runnable() {
            @Override
            public void run() {
                printNumber("A");
            }
        });
        Thread B = new Thread(new Runnable() {
            @Override
            public void run() {
                printNumber("B");
            }
        });
        A.start();
        B.start();
    }

    private static void printNumber(String threadName) {
        int i = 0;
        while (i++ < 3) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(threadName + " print: " + i);
        }
    }

    private static void demo3() {
        Object lock = new Object();
        Thread A = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("INFO: A 等待锁 ");
                synchronized (lock) {
                    System.out.println("INFO: A 得到了锁 lock");
                    System.out.println("A 1");
                    try {
                        System.out.println("INFO: A 准备进入等待状态，放弃锁 lock 的控制权 ");
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("INFO: 有人唤醒了 A, A 重新获得锁 lock");
                    System.out.println("A 2");
                    System.out.println("A 3");
                }
            }
        });
        Thread B = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("INFO: B 等待锁 ");
                synchronized (lock) {
                    System.out.println("INFO: B 得到了锁 lock");
                    System.out.println("B 1");
                    System.out.println("B 2");
                    System.out.println("B 3");
                    System.out.println("INFO: B 打印完毕，调用 notify 方法 ");
                    lock.notify();
                }
            }
        });
        A.start();
        B.start();
    }

    /**
     * 
     * 四个线程 A B C D，其中 D 要等到 A B C 全执行完毕后才执行，而且 A B C 是同步运行的
     * 
     *
     * 
     *
     * 最开始我们介绍了 thread.join()，可以让一个线程等另一个线程运行完毕后再继续执行，那我们可以在 D 线程里依次 join A B
     * C，不过这也就使得 A B C 必须依次执行，而我们要的是这三者能同步运行。
     * 
     * 或者说，我们希望达到的目的是：A B C 三个线程同时运行，各自独立运行完后通知 D；对 D 而言，只要 A B C 都运行完了，D
     * 再开始运行。针对这种情况，我们可以利用 CountdownLatch 来实现这类通信方式。它的基本用法是：
     * 
     * 创建一个计数器，设置初始值，CountdownLatch countDownLatch = new CountDownLatch(2); 在 等待线程
     * 里调用 countDownLatch.await() 方法，进入等待状态，直到计数值变成 0； 在 其他线程 里，调用
     * countDownLatch.countDown() 方法，该方法会将计数值减小 1； 当 其他线程 的 countDown() 方法把计数值变成 0
     * 时，等待线程 里的 countDownLatch.await() 立即退出，继续执行下面的代码。
     */

    private static void runDAfterABC() {
        int worker = 3;
        CountDownLatch countDownLatch = new CountDownLatch(worker);
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName() + "D" + countDownLatch.getCount());
                System.out.println("D is waiting for other three threads");
                try {
                    countDownLatch.await();
                    System.out.println(Thread.currentThread().getName() + "D" + countDownLatch.getCount());
                    System.out.println("All done, D starts working");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        for (char threadName = 'A'; threadName <= 'C'; threadName++) {
            final String tN = String.valueOf(threadName);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println(tN + " is working");
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    System.out.println(tN + " finished");
                    countDownLatch.countDown();
                    System.out.println(Thread.currentThread().getName() + countDownLatch.getCount());
                }
            }).start();
        }
    }

    /**
     * 三个运动员各自准备，等到三个人都准备好后，再一起跑 上面是一个形象的比喻，针对 线程 A B C 各自开始准备，直到三者都准备完毕，然后再同时运行
     * 。也就是要实现一种 线程之间互相等待 的效果，那应该怎么来实现呢？
     *
     * 上面的 CountDownLatch 可以用来倒计数，但当计数完毕，只有一个线程的 await() 会得到响应，无法让多个线程同时触发。
     *
     * 为了实现线程间互相等待这种需求，我们可以利用 CyclicBarrier 数据结构，它的基本用法是：
     *
     * 先创建一个公共 CyclicBarrier 对象，设置 同时等待 的线程数，CyclicBarrier cyclicBarrier = new
     * CyclicBarrier(3); 这些线程同时开始自己做准备，自身准备完毕后，需要等待别人准备完毕，这时调用
     * cyclicBarrier.await(); 即可开始等待别人； 当指定的 同时等待 的线程数都调用了
     * cyclicBarrier.await();时，意味着这些线程都准备完毕好，然后这些线程才 同时继续执行。
     * 实现代码如下，设想有三个跑步运动员，各自准备好后等待其他人，全部准备好后才开始跑：
     */
    private static void runABCWhenAllReady() {
        int runner = 3;
        CyclicBarrier cyclicBarrier = new CyclicBarrier(runner);
        final Random random = new Random();
        for (char runnerName = 'A'; runnerName <= 'C'; runnerName++) {
            final String rN = String.valueOf(runnerName);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long prepareTime = random.nextInt(10000) + 100;
                    System.out.println(rN + " is preparing for time: " + prepareTime);
                    try {
                        Thread.sleep(prepareTime);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        System.out.println(rN + " is prepared, waiting for others");
                        cyclicBarrier.await(); // 当前运动员准备完毕，等待别人准备好
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (BrokenBarrierException e) {
                        e.printStackTrace();
                    }
                    System.out.println(rN + " starts running"); // 所有运动员都准备好了，一起开始跑
                }
            }).start();
        }
    }

    /**
     * 子线程完成某件任务后，把得到的结果回传给主线程 实际的开发中，我们经常要创建子线程来做一些耗时任务，然后把任务执行结果回传给主线程使用
     */
    private static void doTaskWithResultInWorker() {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                System.out.println("Task starts");
                Thread.sleep(1000);
                int result = 0;
                for (int i = 0; i <= 100; i++) {
                    result += i;
                }
                System.out.println("Task finished and return result");
                return result;
            }
        };
        FutureTask<Integer> futureTask = new FutureTask<>(callable);
        new Thread(futureTask).start();
        try {
            System.out.println("Before futureTask.get()");
            System.out.println("Result: " + futureTask.get());
            System.out.println("After futureTask.get()");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // demo1();
        // demo3();
        // runDAfterABC();
        // runABCWhenAllReady();
        doTaskWithResultInWorker();
    }
}
