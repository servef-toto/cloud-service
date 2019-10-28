package com.cloud.user.config;

import com.cloud.log.autoconfigure.LogAop;
import io.netty.channel.ConnectTimeoutException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

/**
 * 配置 RestTemplate，微信登录功能会用到http调用，会用到RestTemplate进行调用
 * RestTemplate 使用的是 HttpClient
 */
@Configuration
public class RestTemplateConfig {
    private static final Logger logger = LoggerFactory.getLogger(RestTemplateConfig.class);

	@Bean
	public RestTemplate restTemplate() {

		/**
		 * 连接池PoolingHttpClientConnectionManager
		 */
		PoolingHttpClientConnectionManager pollingConnectionManager = new PoolingHttpClientConnectionManager();
		// 设置最大连接数
		pollingConnectionManager.setMaxTotal(200);
		// 将每个路由默认最大连接数
		pollingConnectionManager.setDefaultMaxPerRoute(200);

		/**
		 * 配置HttpClient
		 */
		HttpClientBuilder httpClientBuilder = HttpClients.custom();
		//配置HttpClient使用连接池连接池PoolingHttpClientConnectionManager
		httpClientBuilder.setConnectionManager(pollingConnectionManager);
		// 请求重试处理
		httpClientBuilder.setRetryHandler((e,executionCount,context) -> {
            if (executionCount >= 5) {// 如果已经重试了3次，就放弃
                return false;
            }
            if (e instanceof NoHttpResponseException) {
                // 如果服务器丢掉了连接，那么就重试
                logger.warn("服务器丢掉了连接NoHttpResponseException,继续重试:" + e.getMessage());
                return true;
            }
            if (e instanceof SSLHandshakeException) {
                // 不要重试SSL握手异常
                logger.error("SSL握手异常SSLHandshakeException,不在重试:" + e.getMessage());
                return false;
            }
            if (e instanceof InterruptedIOException) {// 超时
                logger.error("超时InterruptedIOException,不在重试:" + e.getMessage());
                return false;
            }
            if (e instanceof UnknownHostException) {
                // 目标服务器不可达
                logger.error("目标服务器不可达UnknownHostException,不在重试:" + e.getMessage());
                return false;
            }
            if (e instanceof ConnectTimeoutException) {
                // 连接被拒绝
                logger.error("连接被拒绝ConnectTimeoutException,不在重试:" + e.getMessage());
                return false;
            }
            if (e instanceof SSLException) {
                // SSL握手异常
                logger.error("SSL握手异常SSLException,不在重试:" + e.getMessage());
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            if (!(request instanceof HttpEntityEnclosingRequest)) {
                // 如果请求是幂等的，就再次尝试
                logger.warn("请求幂等HttpEntityEnclosingRequest,继续重试:" + e.getMessage());
                return true;
            }
            return false;
		});
		HttpClient httpClient = httpClientBuilder.build();


        /**
         * HttpComponentsClientHttpRequestFactory 用于设置 http 请求超时的设置
         */
		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		// 超时时间
		clientHttpRequestFactory.setConnectTimeout(5000);
		clientHttpRequestFactory.setReadTimeout(5000);
		clientHttpRequestFactory.setConnectionRequestTimeout(5000);

        /**
         * 配置 RestTemplate
         */
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(clientHttpRequestFactory);
		//RestTemplate自定义ErrorHandler
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler());

		return restTemplate;
	}
}
