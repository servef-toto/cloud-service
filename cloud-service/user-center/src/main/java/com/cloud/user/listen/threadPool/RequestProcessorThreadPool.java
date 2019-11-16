package com.cloud.user.listen.threadPool;

import com.cloud.user.listen.queue.Request;
import com.cloud.user.listen.queue.RequestQueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 请求处理线程池：单例
 *
 * 请求队列存放使用ArrayBlockingQueue
 * 一个由数组支持的有界队列，此队列按**FIFO（先进先出）**原则对元素进行排序。
 * 新元素插入到队列的尾部，队列获取操作则是从队列头部开始获得元素
 * 这是一个简单的“有界缓存区”，一旦创建，就不能在增加其容量
 * 在向已满队列中添加元素会导致操作阻塞，从空队列中提取元素也将导致阻塞
 * 此类支持对等待的生产者线程和使用者线程进行排序的可选公平策略。默认情况下，不保证是这种排序的。然而通过将公平性（fairness）设置为true，而构造的队列允许按照FIFO顺序访问线程。公平性通常会降低吞吐量，但也减少了可变性和避免了“不平衡性
 *
 *
 */
public class RequestProcessorThreadPool {
    // 在实际项目中，你设置线程池大小是多少，每个线程监控的那个内存队列的大小是多少
    // 都可以做到一个外部的配置文件中
    // 我们这了就给简化了，直接写死了
    private final static int countThread = 10;

    /**
     * 线程池
     */
    private ExecutorService threadPool = Executors.newFixedThreadPool(countThread);
    public RequestProcessorThreadPool() {
        RequestQueue requestQueue = RequestQueue.getInstance();

        for(int i = 0; i < countThread; i++) {
            ArrayBlockingQueue<Request> queue = new ArrayBlockingQueue<Request>(countThread);
            requestQueue.addQueue(queue);
            //开启线程- 使用 submit 能够捕捉到异常
            threadPool.submit(new RequestProcessorThread(queue));
        }
    }


    /**
     * 初始化的便捷方法
     */
    public static void init() {
        getInstance();
    }
    /**
     * jvm的机制去保证多线程并发安全
     * 因为Singleton 是 静态内部类的方式，保证了RequestProcessorThreadPool只会创建一次。
     * 当第二次之后调用就都是拿之前创建好的对象，保证了多线程并发安全。
     * 内部类的初始化，一定只会发生一次，不管多少个线程并发去初始化
     *
     * @return
     */
    public static RequestProcessorThreadPool getInstance() {
        return Singleton.getInstance();
    }
    /**
     * 单例有很多种方式去实现：采取绝对线程安全的一种方式
     * 静态内部类的方式，去初始化单例
     * 创建单例线程池
     */
    private static class Singleton {
        private static RequestProcessorThreadPool instance;

        /**
         * 静态代码块
         * 特点：随着类的加载而执行，且只执行一次，并优先于主函数。用于给类初始化的。
         */
        static {
            instance = new RequestProcessorThreadPool();
        }

        public static RequestProcessorThreadPool getInstance() {
            return instance;
        }

    }

}
