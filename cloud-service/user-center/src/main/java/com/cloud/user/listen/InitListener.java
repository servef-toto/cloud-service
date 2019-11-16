package com.cloud.user.listen;

import com.cloud.user.listen.threadPool.RequestProcessorThreadPool;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * 统初始化监听器
 *
 * 在 Servlet API 中有一个 ServletContextListener 接口，
 *  * 它能够监听 ServletContext 对象的生命周期，实际上就是监听 Web 应用的生命周期。
 *  * 当Servlet 容器启动或终止Web 应用时，会触发ServletContextEvent 事件，该事件由ServletContextListener 来处理。
 *  * 在 ServletContextListener 接口中定义了处理ServletContextEvent 事件的两个方法contextInitialized、contextDestroyed
 *
 */
public class InitListener implements ServletContextListener {
    /**
     * 当Servlet 容器启动Web 应用时调用该方法。在调用完该方法之后，容器再对Filter 初始化，
     * 并且对那些在Web 应用启动时就需要被初始化的Servlet 进行初始化。
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        // 初始化工作线程池和内存队列
        RequestProcessorThreadPool.init();
    }


    /**
     * 当Servlet 容器终止Web 应用时调用该方法。在调用该方法之前，容器会先销毁所有的Servlet 和Filter 过滤器。
     */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

    }
}
