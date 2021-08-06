/**
 * 
 * 通过继承Thread实现线程
 * 
 */

public class ThreadTest extends Thread {

    private int i = 0;
    private static ThreadTest threadTest;
    private static ThreadTest threadTest2;

    @Override

    public void run() {

        for (; i < 50; i++) {
          
            System.out.println(Thread.currentThread().getName() + " is running " + i);

        }

    }

    public static void main(String[] args) {

        for (int j = 0; j < 50; j++) {
            if (j == 20) {

                threadTest = new ThreadTest();
                threadTest.setName("线程1");
                threadTest.start();
                try {
                    threadTest.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                threadTest2 = new ThreadTest();
                threadTest2.setName("线程2");
                threadTest2.start();
            }

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


}