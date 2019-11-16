package com.cloud.user.listen.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求内存队列
 */
public class RequestQueue {
    /**
     * 标识位map
     *
     * 我们知道HashMap是线程不安全的，在多线程环境下，使用Hashmap进行put操作会引起死循环，
     * 导致CPU利用率接近100%，所以在并发情况下不能使用HashMap。
     *
     * ConcurrentHashMap：主要就是为了应对hashmap在并发环境下不安全而诞生的，
     * ConcurrentHashMap的设计与实现非常精巧，大量的利用了volatile，final，CAS等lock-free技术来减少锁竞争对于性能的影响
     */
    private Map<Long, Boolean> flagMap = new ConcurrentHashMap<>();

    /**
     * 内存队列:初始化得请求内存队列，除了分配到每个线程，同时也会在这存着，方便分配请求
     */
    private List<ArrayBlockingQueue<Request>> queues = new ArrayList<ArrayBlockingQueue<Request>>();

    /**
     * 添加一个内存队列
     *
     * @param queue
     */
    public void addQueue(ArrayBlockingQueue<Request> queue) {
        this.queues.add(queue);
    }

    /**
     * 获取内存队列的数量
     * @return
     */
    public int queueSize() {
        return queues.size();
    }

    /**
     * 获取内存队列
     * @param index
     * @return
     */
    public ArrayBlockingQueue<Request> getQueue(int index) {
        return queues.get(index);
    }

    public Map<Long, Boolean> getFlagMap() {
        return flagMap;
    }



    /**
     * jvm的机制去保证多线程并发安全
     * 内部类的初始化，一定只会发生一次，不管多少个线程并发去初始化
     *
     * @return
     */
    public static RequestQueue getInstance() {
        return Singleton.getInstance();
    }


    /**
     * 单例有很多种方式去实现：我采取绝对线程安全的一种方式
     * <p>
     * 静态内部类的方式，去初始化单例
     *
     * @author Administrator
     */
    private static class Singleton {

        private static RequestQueue instance;

        static {
            instance = new RequestQueue();
        }

        public static RequestQueue getInstance() {
            return instance;
        }

    }
}
