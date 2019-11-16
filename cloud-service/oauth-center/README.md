####Oauth2基于客户端与认证服务器验证的能力定义了四种客户端类型:

    授权码模式（authorization code）
    简化模式（implicit）
    密码模式（resource owner password credentials）
    客户端模式（client credentials）
 
 * 基础参数定义：
 
         grant_type （发放令牌类型）
         client_id (客户端标识id)
         username（用户帐号）
         password （用户密码）
         client_secret（客户端标识密钥）
         refresh_token （刷新令牌）
         scope（表示权限范围，可选项）
 * ####1：客户端模式：
     > 认证服务器给客户端下发客户端标识--一个代表了注册信息的唯一字符串。客户端标识不是秘密；它被暴露给资源拥有者，并且不能单独用来客户端验证。客户端标识对认证服务器来说是唯一的。
               
               （A）客户端直接向认证服务器获取令牌介。
               （B）认证服务器确认无误后，向客户端提供访问令牌。
               （C） 客户端携带令牌访问资源端
               常用于访问公共资源（无需登录）：网站首页
               该模式没有refresh_token,过期可以直接认证获取匿名令牌。
 + 样例： url: http://localhost:8020/oauth/token
      > 请求参数:

       grant_type:client_credentials
       client_id:46582ae7217343a8b252e3977e7cc421
       client_secret:cgGvf5Rotv7D76m9JaArfY3YG6fDec47
      > 结果:
      
       {
            "access_token": "9fae1382-8d9c-4c64-a01c-d67817137fd4",
            "token_type": "bearer",
            "expires_in": 27689,
            "scope": "read write"
       }



 * ####2: 密码端模式
     >密码模式在客户端基础上，从用户方获取帐号密码，再访问授权服务器认证授权。
              
              （A）客户端从用户方获取帐号密码。
              （B）客户端携带用户信息向认证服务器获取令牌介。
              （B）认证服务器确认无误后，向客户端提供访问令牌。
              （c） 客户端携带令牌访问资源端
              常用于访问个人资源（必须登录）：个人资料
         拼接授权的client_id和client_secret，并使用base64编码之后作为请求头：先拼接：client_id:client_secret 
         如 ：client:secret    使用任意base64工具编码： Y2xpZW50OnNlY3JldA==
         最后在请求头添加："Authorization" : "Basic Y2xpZW50OnNlY3JldA=="，使用post请求：url： http://localhost:8020/oauth/token?grant_type=password&username=admin&password=admin
         也可直接将client信息直接放在url中,也可将请求数据都放在请求体或者是表单中：得配置支持表单提交
 + 样例： url: http://localhost:8020/oauth/token
      > 请求参数:
       
       grant_type:password
       client_id:46582ae7217343a8b252e3977e7cc421
       username:18565783136
       password:AC1DAdo9ZcY4dKAdtyPRzoICWZlkR7WDgtO06S5fVCUS6A/67rMxeW+2mKKbo2N1FQ==
       client_secret:cgGvf5Rotv7D76m9JaArfY3YG6fDec47
      > 结果:
      
       {
            "access_token": "41d74d86-bd30-4935-a6f1-c61614a1b72b",
            "token_type": "bearer",
            "refresh_token": "1ba402f7-394b-420b-9805-39578d6176f8",
            "expires_in": 30063,
            "scope": "read write"
       }


 * ####3:授权码模式（authorization code）
     >授权码模式（authorization code）是功能最完整、流程最严密的授权模式。它的特点就是通过客户端的后台服务器，与"服务提供商"的认证服务器进行互动。
              
              （A）用户访问客户端，后者将前者导向认证服务器（一个获取授权的页面）。
              （B）用户选择是否给予客户端授权。
              （C）假设用户给予授权，认证服务器将用户导向客户端事先指定的"重定向URI"（redirection URI），同时附上一个授权码。
              （D）客户端收到授权码，附上早先的"重定向URI"，向认证服务器申请令牌。这一步是在客户端的后台的服务器上完成的，对用户不可见。
              （E）认证服务器核对了授权码和重定向URI，确认无误后，向客户端发送访问令牌（access token）和更新令牌（refresh token）。
               常用于 第三方登录，例如：QQ登录网易音乐。
 + 样例：
   + 1.在浏览器中访问OAuth2 服务器的认证接口：
     + http://localhost:8020/oauth/authorize?response_type=code&client_id=test&redirect_uri=http://localhost:8080
 
           response_type=code : 代表期望的请求响应类型为authorization code
           client_id=test: client_id为你需要使用的客户端id
           redirect_uri=http://localhost:8080 ： redirect_uri是成功获取token之后，重定向的地址
   + 2.重定向
     + 访问认证接口成功之后，浏览器会跳转到OAuth2配置的登录页或者默认的security登录，正确输入用户名/密码之后。浏览器将会在重定向的地址上返回一个code。如下：
        
            http://localhost:8080?code=W3ixVa
            这里code=W3ixVa : code就是OAuth2服务器返回的
   
   + 3.然后使用获取到的code范围OAuth2认证服务器取到access_token，如下：
     + http://localhost:8020/oauth/token?grant_type=authorization_code&code=W3ixVa&client_id=test&client_secret=secret&redirect_uri=http://localhost:8080
           
           grant_type=authorization_code : grant_type为认证类型，当前为授权码模式
           code=W3ixVa ： code为上面获取到的code
           client_id=test ： client_id 与上面获取code的client_id需要一致
           client_secret=secret : 为client_id对应的客户端的密钥
           redirect_uri=http://localhost:8080 ： ： redirect_uri是成功获取token之后，重定向的地址
     + 结果
           
           {
                "access_token": "41d74d86-bd30-4935-a6f1-c61614a1b72b",
                "token_type": "bearer",
                "refresh_token": "1ba402f7-394b-420b-9805-39578d6176f8",
                "expires_in": 30063,
                "scope": "read write"
           }
           
           
 * ####4:简化模式（implicit）
     >不通过第三方应用程序的服务器，直接在浏览器中向认证服务器申请令牌，跳过了"授权码"这个步骤，即没有code，直接返回令牌。
              
              （A）客户端将用户导向认证服务器。
              （B）用户决定是否给于客户端授权。
              （C）假设用户给予授权，认证服务器将用户导向客户端指定的"重定向URI"，并在URI的Hash部分包含了访问令牌。
              （D）客户端（第三方服务器）获取到令牌。
 + 样例：
        
        GET  oauth-server/authorize?response_type=token&client_id=cgGvf5Rotv7D76m9JaArfY3YG6fDec47&state=userId&redirect_uri=www.baidu.com
 + 重定向：

          www.baidu.com?access_token=41d74d86-bd30-4935-a6f1-c61614a1b72b&state=userId

 * ####5:刷新令牌
     >当访问令牌过期时候，刷新重新获取令牌。
     
 + 样例：url: http://localhost:8020/oauth/token
      > 请求参数
       
       grant_type:refresh_token
       client_id:46582ae7217343a8b252e3977e7cc421
       client_secret:cgGvf5Rotv7D76m9JaArfY3YG6fDec47
       refresh_token:1ba402f7-394b-420b-9805-39578d6176f8
      > 结果
       
       {
            "access_token": "6d8cffd1-a90e-4846-838f-176050ed49b4",
             "token_type": "bearer",
             "refresh_token": "1ba402f7-394b-420b-9805-39578d6176f8",
            "expires_in": 43199,
            "scope": "read write"
       }
       
       
       
       



 ##执行流程剖析:

#### 1:身份验证的入口-AbstractAuthenticationProcessingFilter
	处理所有HTTP Request和Response对象，并将其封装成AuthenticationMananger可以处理的Authentication。
	并且在身份验证成功或失败之后将对应的行为转换为HTTP的Response。同时还要处理一些Web特有的资源比如Session和Cookie。
	总结成一句话，就是替AuthenticationMananger把所有和Authentication没关系的事情全部给包圆了。

	它将大任务拆成了几个子任务并交给了以下组件完成：
		AuthenticationManager用于处理身份验证的核心逻辑；
		AuthenticationSuccessHandler用于处理验证成功的后续流程；
		AuthenticationFailureHandler用于处理失败的后续流程；
		在验证成功后发布一个名为InteractiveAuthenticationSuccessEvent的事件通知给到应用上下文，用于告知身份验证已经成功；
		因为是基于浏览器所以相关的会话管理行为交由 SessionAuthenticationStrategy来进行实现。
		文档上还有一点没有写出来的是，如果用户开启了类似“记住我”之类的免密码登录，AbstractAuthenticationProcessingFilter还有一个名为RememberMeServices来进行管理。

	``` 
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
    		HttpServletRequest request = (HttpServletRequest)req;
    		HttpServletResponse response = (HttpServletResponse)res;
    		if (!this.requiresAuthentication(request, response)) {
    		    chain.doFilter(request, response);
    		} else {
    		    if (this.logger.isDebugEnabled()) {
    			this.logger.debug("Request is to process authentication");
    		    }
    
    		    Authentication authResult;
    		    try {
    			authResult = this.attemptAuthentication(request, response);
    			if (authResult == null) {
    			    return;
    			}
    
    			this.sessionStrategy.onAuthentication(authResult, request, response);
    		    } catch (InternalAuthenticationServiceException var8) {
    			this.logger.error("An internal error occurred while trying to authenticate the user.", var8);
    			this.unsuccessfulAuthentication(request, response, var8);
    			return;
    		    } catch (AuthenticationException var9) {
    			this.unsuccessfulAuthentication(request, response, var9);
    			return;
    		    }
    
    		    if (this.continueChainBeforeSuccessfulAuthentication) {
    			chain.doFilter(request, response);
    		    }
    
    		    this.successfulAuthentication(request, response, chain, authResult);
    		}
    	}
	``` 
	默认的登录过滤器 UsernamePasswordAuthenticationFilter 拦截到登录请求，调用父类 AbstractAuthenticationProcessingFilter 的 doFilter 的方法。
	doFilter()方法：

	requiresAuthentication(request, response) 先是对请求进行校验检查是否是符合规则,是否有必要的参数等等（client_id）
	doFilter 方法调用 UsernamePasswordAuthenticationFilter 自身的 attemptAuthentication 方法进行登录认证。
	AbstractAuthenticationProcessingFilter.successfulAuthentication():认证成功之后，继续回到 AbstractAuthenticationProcessingFilter，执行 successfulAuthentication 方法，存放认证信息到上下文，最终决定登录认证成功之后的操作。

	
	     


#### 2：UsernamePasswordAuthenticationFilter
	UsernamePasswordAuthenticationFilter是登陆用户密码验证过滤器，它继承了AbstractAuthenticationProcessingFilter过滤器（真正的Filter），是spring security3的第4个过滤器。
	UsernamePasswordAuthenticationFilter有3个表单参数，是我们需要知道的
		1、usernameParameter：对应登录时的用户名需要传的参数名称，默认为j_username，比如你输入用户hello，表单提交时是这样的 j_username=hello
		2、passwordParameter：对应登录时的密码提交时的参数名称，默认为j_password，比如你输入密码是123123，表单提交是这样的 j_password=123123
		3、filterProcessesUrl（放在了AbstractAuthenticationProcessingFilter）：表单提交地址，默认为/j_spring_security_check，这个地址才能被UsernamePasswordAuthenticationFilter所截取，进行登录认证。
	
	    ```
        public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
            if (this.postOnly && !request.getMethod().equals("POST")) {
                throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
            } else {
                String username = this.obtainUsername(request);
                String password = this.obtainPassword(request);
                if (username == null) {
                username = "";
                }

                if (password == null) {
                password = "";
                }

                username = username.trim();
                UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
                this.setDetails(request, authRequest);
                return this.getAuthenticationManager().authenticate(authRequest);
            }
        }
        ```
		1：attemptAuthentication方法会对请求进行校验是否是post请求等
		2：代码 UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
		       this.setDetails(request, authRequest);
		   attemptAuthentication方法会将表单提交的用户密码以及一些用户的其他信息（比如remoteAddr，seesionId），先放入UsernamePasswordAuthenticationToken中
		3：attemptAuthentication 继续调用认证管理器 ProviderManager 的 authenticate 方法。




#### 3：ProviderManager
	在ProviderManager的authenticate方法中，轮训成员变量List<AuthenticationProvider> providers。该providers中如果有一个
        AuthenticationProvider的supports函数返回true，那么就会调用该AuthenticationProvider的authenticate函数认证，如果认证成功则整个
        认证过程结束。如果不成功，则继续使用下一个合适的AuthenticationProvider进行认证，只要有一个认证成功则为认证成功。
    ```
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		Class<? extends Authentication> toTest = authentication.getClass();
		AuthenticationException lastException = null;
		Authentication result = null;
		boolean debug = logger.isDebugEnabled();
		Iterator var6 = this.getProviders().iterator();

		while(var6.hasNext()) {
		    AuthenticationProvider provider = (AuthenticationProvider)var6.next();
		    if (provider.supports(toTest)) {
			if (debug) {
			    logger.debug("Authentication attempt using " + provider.getClass().getName());
			}

			try {
			    result = provider.authenticate(authentication);
			    if (result != null) {
				this.copyDetails(authentication, result);
				break;
			    }
			} catch (AccountStatusException var11) {
			    this.prepareException(var11, authentication);
			    throw var11;
			} catch (InternalAuthenticationServiceException var12) {
			    this.prepareException(var12, authentication);
			    throw var12;
			} catch (AuthenticationException var13) {
			    lastException = var13;
			}
		    }
		}

		if (result == null && this.parent != null) {
		    try {
			result = this.parent.authenticate(authentication);
		    } catch (ProviderNotFoundException var9) {
			;
		    } catch (AuthenticationException var10) {
			lastException = var10;
		    }
		}

		if (result != null) {
		    if (this.eraseCredentialsAfterAuthentication && result instanceof CredentialsContainer) {
			((CredentialsContainer)result).eraseCredentials();
		    }

		    this.eventPublisher.publishAuthenticationSuccess(result);
		    return result;
		} else {
		    if (lastException == null) {
			lastException = new ProviderNotFoundException(this.messages.getMessage("ProviderManager.providerNotFound", new Object[]{toTest.getName()}, "No AuthenticationProvider found for {0}"));
		    }

		    this.prepareException((AuthenticationException)lastException, authentication);
		    throw lastException;
		}
	}
    ```
	其中的默认实现是 DaoAuthenticationProvider，继承自 AbstractUserDetailsAuthenticationProvider， 
	所以 AbstractUserDetailsAuthenticationProvider 的 authenticate 方法被调用。


#### 4:AbstractUserDetailsAuthenticationProvider
    该类的public Authentication authenticate(Authentication authentication) throws AuthenticationException方法百常重要，通过这段代码能详细了解整个验证的过程,下面对该方法的代码分段进行说明：
    ```
    (1)     Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                messages.getMessage("AbstractUserDetailsAuthenticationProvider.onlySupports",
                "Only UsernamePasswordAuthenticationToken is supported"));
            //要求传入的authentication对象必须是UsernamePasswordAuthenticationToken类或其子类的实例
    (2)  
            String username = (authentication.getPrincipal() == null) ? "NONE_PROVIDED" : authentication.getName();
            //从authentication中取出登录名
    (3)     boolean cacheWasUsed = true;
            UserDetails user = this.userCache.getUserFromCache(username);
            //默认情况下从缓存中(UserCache接口实现)取出用户信息
    (4)
            if (user == null) {
            //如果从内存中取不到用户，则设置cacheWasUsed 为false，供后面使用
        　　 cacheWasUsed = false;
                try {

        　　　　　　//retrieveUser是抽象方法，通过子类来实现获取用户的信息,以UserDetails接口形式返回
                user = retrieveUser(username, (UsernamePasswordAuthenticationToken) authentication);
                } catch (UsernameNotFoundException notFound) {
                logger.debug("User '" + username + "' not found");
                if (hideUserNotFoundExceptions) {
                    throw new BadCredentialsException(messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials"));
                } else {
                    throw notFound;
                }
                }
                Assert.notNull(user, "retrieveUser returned null - a violation of the interface contract");
            }
    (5)
            try {
                //preAuthenticationChecks和additionalAuthenticationChecks这是UserDetailsChecker接口的实现类
                //验证帐号是否锁定\是否禁用\帐号是否到期
                preAuthenticationChecks.check(user);
        　　　　//由子类来完成更进一步的验证
                additionalAuthenticationChecks(user, (UsernamePasswordAuthenticationToken) authentication);
            } catch (AuthenticationException exception) {

               //下面这段代码体现了老外的细腻之处，意思是说如果在调用某个UserDetailsChecker接口的实现类验证失败后,就判断下用户信息是否从内存中得到，如果之前是从内存中得到的用户信息，那么考虑到可能数据是不实时的，
               //就重新通过retrieveUser方法去取出用户信息，再次重复进行检查验证
                if (cacheWasUsed) {
                // There was a problem, so try again after checking
                // we're using latest data (i.e. not from the cache)
                cacheWasUsed = false;
                user = retrieveUser(username, (UsernamePasswordAuthenticationToken) authentication);
                preAuthenticationChecks.check(user);
                additionalAuthenticationChecks(user, (UsernamePasswordAuthenticationToken) authentication);
                } else {
                throw exception;
                }
            }
    (6)     //在spring的框架设计中经常能看到这样的前置处理和后置处理，此处后置处理只是判断了下用户的密码是否过期，如过期则记入日志
               postAuthenticationChecks.check(user);
             //如果没有缓存则进行缓存,则处的userCache是由NullUserCache类实现的，名如其义，该类的putUserInCache没做任何事
             if (!cacheWasUsed) {
                this.userCache.putUserInCache(user);
            }
    (7)
            //以下代码主要是把用户的信息和之前用户提交的认证信息重新组合成一个authentication实例返回，返回类是
            UsernamePasswordAuthenticationToken类的实例
             Object principalToReturn = user;
            if (forcePrincipalAsString) {
                principalToReturn = user.getUsername();
            }
            return createSuccessAuthentication(principalToReturn, authentication, user);
    ```
    从上面能看到 retrieveUser 是调用子类的。
    1：AbstractUserDetailsAuthenticationProvider 的 authenticate 在认证过程中又调用 DaoAuthenticationProvider 的 retrieveUser 方法获取登录认证所需的用户信息。
    2：DaoAuthenticationProvider.additionalAuthenticationChecks()获取到用户认证所需的信息之后，
        认证器会进行一些检查譬如 preAuthenticationChecks 进行账号状态之类的前置检查，
        然后调用 DaoAuthenticationProvider 的 additionalAuthenticationChecks 方法验证密码合法性。
    3：AbstractUserDetailsAuthenticationProvider.createSuccessAuthentication()
    登录认证成功之后， AbstractUserDetailsAuthenticationProvider 的 createSuccessAuthentication 方法被调用， 返回一个 UsernamePasswordAuthenticationToken 对象。


#### 5:DaoAuthenticationProvider
	DaoAuthenticationProvider实现了父类AbstractUserDetailsAuthenticationProvider的retrieveUser()和additionalAuthenticationChecks()方法
    1：retrieveUser 
        ```
        protected final UserDetails retrieveUser(String username,
                UsernamePasswordAuthenticationToken authentication)
                throws AuthenticationException {
            UserDetails loadedUser;

            try {
                //调用UserDetailsService接口的loadUserByUsername获取用户信息
                //通过实现UserDetailsService接口来扩展对用户密码的校验
                loadedUser = this.getUserDetailsService().loadUserByUsername(username);
            }
            
            ......

            //如果找不到该用户，则抛出异常
            if (loadedUser == null) {
                throw new InternalAuthenticationServiceException(
                    "UserDetailsService returned null, which is an interface contract violation");
            }
            return loadedUser;
        }
        ```
	DaoAuthenticationProvider调用UserDetailsService接口的loadUserByUsername获取用户信息


	2：additionalAuthenticationChecks()
		获取到用户认证所需的信息之后，认证器会进行一些检查譬如 preAuthenticationChecks 进行账号状态之类的前置检查，
		然后调用 DaoAuthenticationProvider 的 additionalAuthenticationChecks 方法验证密码合法性。
            ```
            @SuppressWarnings("deprecation")
            protected void additionalAuthenticationChecks(UserDetails userDetails,
                    UsernamePasswordAuthenticationToken authentication)
                    throws AuthenticationException {
                Object salt = null;

                if (this.saltSource != null) {
                    salt = this.saltSource.getSalt(userDetails);
                }

                //密码为空，则直接抛出异常
                if (authentication.getCredentials() == null) {
                    logger.debug("Authentication failed: no credentials provided");

                    throw new BadCredentialsException(messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.badCredentials",
                        "Bad credentials"));
                }

                //获取用户输入的密码
                String presentedPassword = authentication.getCredentials().toString();

                //将缓存中的密码(也可能是自定义查询的密码)与用户输入密码匹配
                //如果匹配不上，则抛出异常
                if (!passwordEncoder.isPasswordValid(userDetails.getPassword(),
                    presentedPassword, salt)) {
                    logger.debug("Authentication failed: password does not match stored value");

                    throw new BadCredentialsException(messages.getMessage(
                        "AbstractUserDetailsAuthenticationProvider.badCredentials",
                        "Bad credentials"));
                }
            }
            ```


#### 6:UserDetailsService
    UserDetailsManager.loadUserByUsername()

    DaoAuthenticationProvider 的 retrieveUser 方法 通过 UserDetailsService 来进一步获取登录认证所需的用户信息。
    UserDetailsManager 接口继承了 UserDetailsService 接口，框架默认提供了 InMemoryUserDetailsManager 和 JdbcUserDetailsManager 两种用户信息的获取方式，
    当然 InMemoryUserDetailsManager 主要用于非正式环境，正式环境大多都是采用  JdbcUserDetailsManager，从数据库获取用户信息，
    当然你也可以根据需要扩展其他的获取方式。
    
    UserDetailsService 其实一个接口，实际调用的还是我们自己的实现的类