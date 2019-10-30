package com.cloud.oauth.config;

import com.cloud.model.user.LoginAppUser;
import com.cloud.oauth.service.impl.RandomAuthenticationKeyGenerator;
import com.cloud.oauth.service.impl.RedisAuthorizationCodeServices;
import com.cloud.oauth.service.impl.RedisClientDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.code.JdbcAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import java.util.HashMap;
import java.util.Map;

/**
 * 资源服务器配置
 * @EnableAuthorizationServer 开启授权服务器
 *
 * 配置：
 * AuthorizationServerConfigurerAdapter 类中3个不同的configure方法分别
     * configure(ClientDetailsServiceConfigurer clients)
           * 用来配置客户端详情服务（ClientDetailsService），客户端详情信息在这里进行初始化，你能够把客户端详情信息写死在这里或者是通过数据库来存储调取详情信息
     * configure(AuthorizationServerEndpointsConfigurer endpoints)
           * 用来配置授权（authorization）以及令牌（token）的访问端点和令牌服务(token services)，还有token的存储方式(tokenStore)
     * configure(AuthorizationServerSecurityConfigurer security)
           * 用来配置令牌端点(Token Endpoint)的安全约束
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {
    /**
     * 认证管理器
     *
     * @see SecurityConfig 的authenticationManagerBean()
     */
    @Autowired
    private AuthenticationManager authenticationManager;
    /**
     * redis的链接
     */
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    /**
     * redis存储授权码的service类
     */
    @Autowired
    private RedisAuthorizationCodeServices redisAuthorizationCodeServices;
    /**
     * @see com.cloud.oauth.service.impl.UserDetailServiceImpl 用户信息相关的实现
     */
    @Autowired
    public UserDetailsService userDetailsService;
    /**
     * jwt签名key，可随意指定<br>
     * 如配置文件里不设置的话，冒号后面的是默认值
     */
    @Value("${access_token.jwt-signing-key:liuming}")
    private String signingKey;
    /**
     * 配置文件：使用jwt或者redis<br>
     * 默认redis
     */
    @Value("${access_token.store-jwt:false}")
    private boolean storeWithJwt;
    /**
     * 配置文件：登陆后返回的json数据是否追加当前用户信息<br>
     * 默认false
     */
    @Value("${access_token.add-userinfo:false}")
    private boolean addUserInfo;

    /**
     *
     * 配置令牌 管理
     * AuthorizationServerEndpointsConfigurer：
     *  用来配置授权（authorization）
     *  以及令牌（token）的访问端点
     *  和令牌服务(token services)
     *  还有token的存储方式(tokenStore)
     *
     *
     *  配置授权服务一个比较重要的方面就是提供一个授权码给一个OAuth客户端（通过 authorization_code 授权类型），
     *  一个授权码的获取是OAuth客户端跳转到一个授权页面，然后通过验证授权之后服务器重定向到OAuth客户端，并且在重定向连接中附带返回一个授权码。
     * @param endpoints
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        ////启用oauth2管理，认证管理器，当你选择了资源所有者密码（password）授权类型的时候，
        // 请设置这个属性注入一个 AuthenticationManager 对象，不配做密码模式下报错Unsupported grant type: password
        endpoints.authenticationManager(this.authenticationManager);

        //令牌服务（token）的存储方式(tokenStore)
        endpoints.tokenStore(tokenStore());

        // 配置授权（authorization）,授权码模式下，code存储-redis  authorizationCodeServices：这个属性是用来设置授权码服务的（即 AuthorizationCodeServices 的实例对象），主要用于 "authorization_code" 授权码类型模式。
        endpoints.authorizationCodeServices(redisAuthorizationCodeServices);

        if (storeWithJwt) {
            // 配置JwtAccessToken转换器：AccessToken转换器-定义token的生成方式，这里使用JWT生成token，对称加密只需要加入key等其他信息（自定义）。
            // 如果采用非对称加密，则需要服务器端先生成公钥和密钥，客户端访问带上公钥进行校验
            // 如果不想要JWT，将此行及jwtAccessTokenConverter()方法删除即可，这里我们设置了可以配置
            endpoints.accessTokenConverter(accessTokenConverter());
        } else {
            //TokenEhancer（令牌增强器）： ehance方法
            //将当前用户信息追加到登陆后返回数据里
            endpoints.tokenEnhancer((accessToken, authentication) -> {
                addLoginUserInfo(accessToken, authentication);
                return accessToken;
            });
        }

        // refresh_token需要userDetailsService
//        endpoints.reuseRefreshTokens(false).userDetailsService(userDetailsService);
//        endpoints.userDetailsService(userDetailsService);//若无，refresh_token会有UserDetailsService is required错误
    }
    /**
     * token 令牌存储实现-token存放位置:jwt/redis
     * tokenStore通常情况为自定义实现，一般放置在缓存或者数据库中。此处可以利用自定义tokenStore来实现多种需求，如：
     *     同已用户每次获取token，获取到的都是同一个token，只有token失效后才会获取新token。
     *     同一用户每次获取token都生成一个完成周期的token并且保证每次生成的token都能够使用（多点登录）。
     *     同一用户每次获取token都保证只有最后一个token能够使用，之前的token都设为无效（单点token）。
     */
    @Bean
    public TokenStore tokenStore() {
        //如果配置觉得是否使用 jwt/redis存储token
        if (storeWithJwt) {
            return new JwtTokenStore(accessTokenConverter());
        }

        RedisTokenStore redisTokenStore = new RedisTokenStore(redisConnectionFactory);
        // 解决同一username每次登陆access_token都相同的问题
        redisTokenStore.setAuthenticationKeyGenerator(new RandomAuthenticationKeyGenerator());
        return redisTokenStore;
    }
    /**
     * Jwt资源令牌转换器<br>
     * 参数access_token.store-jwt为true时用到
     *
     * @return accessTokenConverter
     */
    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter() {
            //JwtAccessTokenConverter也实现了TokenEnhancer增加器，用来扩展令牌信息,
            // 如果如果使用redis存储的token则使用的是redisToken实现，他没有和JwtAccessTokenConverter一样实现TokenEnhancer增加器，
            // 所以需要在我们也会设置令牌增强器，直接使用AuthorizationServerEndpointsConfigurer配置，如果使用JWT就在内部配置就行了
            @Override
            public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
                OAuth2AccessToken oAuth2AccessToken = super.enhance(accessToken, authentication);
                // 将当前用户信息追加到登陆后返回数据里
                addLoginUserInfo(oAuth2AccessToken, authentication);
                return oAuth2AccessToken;
            }
        };
        DefaultAccessTokenConverter defaultAccessTokenConverter = (DefaultAccessTokenConverter) jwtAccessTokenConverter
                .getAccessTokenConverter();

        //设置用户信息的具体实现
        DefaultUserAuthenticationConverter userAuthenticationConverter = new DefaultUserAuthenticationConverter();
        userAuthenticationConverter.setUserDetailsService(userDetailsService);

        defaultAccessTokenConverter.setUserTokenConverter(userAuthenticationConverter);

        // 设置JWT签名：这里务必设置一个，否则多台认证中心的话，一旦使用jwt方式，access_token将解析错误
        jwtAccessTokenConverter.setSigningKey(signingKey);

        return jwtAccessTokenConverter;
    }
    /**
     * 将当前用户信息追加到登陆后返回的json数据里<br>
     * 通过参数access_token.add-userinfo控制<br>
     *
     * @param accessToken
     * @param authentication
     */
    private void addLoginUserInfo(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        if (!addUserInfo) {
            return;
        }

        if (accessToken instanceof DefaultOAuth2AccessToken) {
            DefaultOAuth2AccessToken defaultOAuth2AccessToken = (DefaultOAuth2AccessToken) accessToken;

            Authentication userAuthentication = authentication.getUserAuthentication();
            Object principal = userAuthentication.getPrincipal();
            if (principal instanceof LoginAppUser) {
                LoginAppUser loginUser = (LoginAppUser) principal;

                Map<String, Object> map = new HashMap<>(defaultOAuth2AccessToken.getAdditionalInformation()); // 旧的附加参数
                map.put("loginUser", loginUser); // 追加当前登陆用户

                defaultOAuth2AccessToken.setAdditionalInformation(map);
            }
        }
    }


    /**
     *  AuthorizationServerSecurityConfigurer：用来配置令牌端点(Token Endpoint)的安全约束.
     * @param security
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        //默认是不支持表单提交，这里修改提交权限,允许表单形式的认证
        security.allowFormAuthenticationForClients();
    }



    /**
     * client_details认证信息处理的service类
     */
    @Autowired
    private RedisClientDetailsService redisClientDetailsService;

    /**
     * 配置客户端详情信息（Client Details)
     * ClientDetailsServiceConfigurer：用来配置客户端详情服务（ClientDetailsService），客户端详情信息在这里进行初始化，
     *
     * ClientDetailsServiceConfigurer (AuthorizationServerConfigurer 的一个回调配置项)
     * 能够使用内存或者JDBC来实现客户端详情服务（ClientDetailsService），
     * Spring Security OAuth2的配置方法是编写@Configuration类继承AuthorizationServerConfigurerAdapter，
     * 然后重写void configure(ClientDetailsServiceConfigurer clients)方法
     *
     * 能够使用内存或 JDBC 方式实现获取已注册的客户端详情，有几个重要的属性：
         * clientId：客户端标识 ID
         * secret：客户端安全码
         * scope：客户端访问范围，默认为空则拥有全部范围
         * authorizedGrantTypes：客户端使用的授权类型，默认为空
         * authorities：客户端可使用的权限
     * 我们将client信息存储到oauth_client_details表里<br>
     * 并将数据缓存到redis
     *
     * @param clients
     * @throws Exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        //详细看下redisClientDetailsService这个实现类
        //将oauth_client_details表数据缓存到redis，毕竟该表改动非常小，而且数据很少，这里做个缓存优化
        //通过JdbcClientDetailsService从数据库读取相应的配置，用来配置密码、授权码模式等
        clients.withClientDetails(redisClientDetailsService);
        // 第一次将ClientDetails配置全表刷入redis
        redisClientDetailsService.loadAllClientToCache();
    }

}
