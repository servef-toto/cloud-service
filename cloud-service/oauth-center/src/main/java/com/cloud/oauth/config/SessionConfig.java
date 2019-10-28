package com.cloud.oauth.config;

import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 开启session共享
 *
 * 用redis做session共享，在授权码模式下，
 * 可能会涉及参数code和state和redirect_url的传递，多台服务器下需要共享session。
 *
 * @author admin008
 */
@EnableRedisHttpSession
public class SessionConfig {

}
