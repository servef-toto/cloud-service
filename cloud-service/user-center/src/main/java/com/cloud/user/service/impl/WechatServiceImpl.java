package com.cloud.user.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.cloud.model.user.*;
import com.cloud.model.user.constants.CredentialType;
import com.cloud.user.config.WechatConfig;
import com.cloud.user.dao.UserCredentialsDao;
import com.cloud.user.dao.WechatDao;
import com.cloud.user.service.AppUserService;
import com.cloud.user.service.WechatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WechatServiceImpl implements WechatService {

    @Autowired
    private WechatConfig wechatConfig;

    //第一步获取code
    private static final String WECHAT_AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=%s#wechat_redirect";
    private static final String STATE_WECHAT = "state_wechat";

    //第二部通过第一步得到的code，获取access_token
    private static final String WECHAT_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";

    //第三步通过access_token 和 openId 获取 用户信息
    private static final String WECHAT_USERINFO_URL = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN";


    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private WechatDao wechatDao;
    @Autowired
    private UserCredentialsDao userCredentialsDao;
    @Autowired
    private TaskExecutor taskExecutor;


    private WechatInfo getWechatInfo(String app) {
        WechatInfo wechatInfo = wechatConfig.getInfos().get(app);
        if (wechatInfo == null) {
            throw new IllegalArgumentException("未找到，" + app);
        }

        return wechatInfo;
    }

    @Override
    public String getWechatAuthorizeUrl(String app, HttpServletRequest request, String toUrl)
            throws UnsupportedEncodingException {
        log.info("引导到授权页:{},{}", app, toUrl);
        //获取配置的微信公众号的信息
        WechatInfo wechatInfo = getWechatInfo(app);

        // 网关域名(外网)加路由到用户系统的规则 https://xxx.xxx.xxx/api-u
        String domain = wechatConfig.getDomain();
        // 微信授权后调用该地址接口，参数为授权后得到的code、state
        StringBuilder redirectUri = new StringBuilder(domain + "/wechat/" + app + "/back");
        if (StringUtils.isNoneBlank(toUrl)) {
            toUrl = URLEncoder.encode(toUrl, "utf-8");
            redirectUri.append("?toUrl=").append(toUrl);
        }
        String redirect_uri = URLEncoder.encode(redirectUri.toString(), "utf-8");

        // 生成一个随机串，微信再跳回来的时候，会原封不动给我们带过来，到时候做一下校验
        String state = UUID.randomUUID().toString();
        request.getSession().setAttribute(STATE_WECHAT, state);

        String a = String.format(WECHAT_AUTHORIZE_URL, wechatInfo.getAppid(), redirect_uri, state);
        log.info("引导到授权页:{}", a);
        return a;
    }


    /**
     * getWechatUserInfo 获取微信用户信息
     * @param app
     * @param request
     * @param code
     * @param state
     * @return
     */
    @Transactional
    @Override
    public WechatUserInfo getWechatUserInfo(String app, HttpServletRequest request, String code, String state) {
        log.info("code:{}, state:{}", code, state);
        //校验state
        checkStateLegal(state, request);
        //通过code获取access_token
        WechatAccess wechatAccess = getWechatAccess(app, code);
        //通过open_id查询数据库是否有,open_id对微信用户来说是唯一的
        WechatUserInfo wechatUserInfo = wechatDao.findByOpenid(wechatAccess.getOpenid());
        if (wechatUserInfo == null) {
            //通过access_token获取用户的基本信息
            wechatUserInfo = saveWechatUserInfo(app, wechatAccess);
        } else {
            //修改用户数据
            updateWechatUserInfo(wechatAccess, wechatUserInfo);
        }
        //返回用户数据
        return wechatUserInfo;
    }

    /**
     * 保存微信用户信息
     * @param app
     * @param wechatAccess
     * @return
     */
    private WechatUserInfo saveWechatUserInfo(String app, WechatAccess wechatAccess) {
        //通过access_token获取用户的基本信息
        WechatUserInfo wechatUserInfo = getWechatUserInfo(wechatAccess);

        // 多公众号支持-unionid 只有在用户将公众号绑定到微信开放平台帐号后，才会出现该字段。详见：获取用户个人信息（UnionID机制），每个用户只有一个
        String unionid = wechatUserInfo.getUnionid();
        if (StringUtils.isNoneBlank(unionid)) {
            // 根据unionid查询，看是否有同源公众号已绑定用户-和用户结构有关系：用户表app-user  = 可以有多张凭证表（手机号码短信验证登录凭证、微信登录凭证、账号登录凭证）
            Set<WechatUserInfo> set = wechatDao.findByUniond(unionid);
            if (!CollectionUtils.isEmpty(set)) {
                WechatUserInfo userInfo = set.parallelStream().filter(w -> w.getUserId() != null).findFirst().orElse(null);
                if (userInfo != null) {
                    wechatUserInfo.setUserId(userInfo.getUserId());
                    log.info("具有相同的unionid,视为同一用户：{}", userInfo);

                    // 将新公众号的openid也存入登陆凭证表
                    userCredentialsDao.save(new UserCredential(wechatUserInfo.getOpenid(), CredentialType.WECHAT_OPENID.name(), userInfo.getUserId()));
                }
            }
        }

        wechatUserInfo.setApp(app);
        wechatUserInfo.setCreateTime(new Date());
        wechatUserInfo.setUpdateTime(wechatUserInfo.getCreateTime());

        wechatDao.save(wechatUserInfo);
        log.info("保存微信个人用户信息:{}", wechatUserInfo);

        return wechatUserInfo;
    }

    /**
     * 异步更新微信个人用户信息
     *
     * @param wechatAccess
     * @param wechatUserInfo
     */
    private void updateWechatUserInfo(WechatAccess wechatAccess, WechatUserInfo wechatUserInfo) {
        taskExecutor.execute(() -> {
            WechatUserInfo userInfo = getWechatUserInfo(wechatAccess);
            BeanUtils.copyProperties(userInfo, wechatUserInfo, new String[]{"id", "userId"});
            wechatUserInfo.setUpdateTime(new Date());
            wechatDao.update(wechatUserInfo);

            log.info("更新微信个人用户信息:{}", wechatUserInfo);
        });
    }

    /**
     * 校验state是否合法
     * states是之前第一步授权获取code的时候生成的，我们将states保存在session中
     * 微信授权后会原封不动的返回
     * 在这里校验通过后 将 session中的state删掉
     *
     * @param state
     * @param request
     */
    private void checkStateLegal(String state, HttpServletRequest request) {
        HttpSession httpSession = request.getSession();
        String sessionState = (String) httpSession.getAttribute(STATE_WECHAT);
        if (sessionState == null) {
            throw new IllegalArgumentException("缺失session state");
        }

        if (!state.equals(sessionState)) {
            throw new IllegalArgumentException("非法state");
        }

        // 校验通过，将session中的state移除
        httpSession.removeAttribute(STATE_WECHAT);
    }


    /**
     * 第二步:授权之后通过code获取用户access_token等登录信息
     * @param app
     * @param code
     * @return
     */
    private WechatAccess getWechatAccess(String app, String code) {
        WechatInfo wechatInfo = getWechatInfo(app);
        //请求assess_token地址
        String accessTokenUrl = String.format(WECHAT_ACCESS_TOKEN_URL, wechatInfo.getAppid(), wechatInfo.getSecret(),
                code);
        //使用restTemplate 发起 get 请求
        String string = restTemplate.getForObject(accessTokenUrl, String.class);
        WechatAccess wechatAccess = JSONObject.parseObject(string, WechatAccess.class);
        log.info("wechatAccess:{}", wechatAccess);

        return wechatAccess;
    }

    /**
     * 第三步：通过access_token获取用户的基本信息
     *
     * @param wechatAccess
     * @return
     */
    private WechatUserInfo getWechatUserInfo(WechatAccess wechatAccess) {
        String userInfoUrl = String.format(WECHAT_USERINFO_URL, wechatAccess.getAccess_token(),
                wechatAccess.getOpenid());

        String string = restTemplate.getForObject(userInfoUrl, String.class);

        try {
            string = new String(string.getBytes("ISO-8859-1"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        WechatUserInfo userInfo = JSONObject.parseObject(string, WechatUserInfo.class);
        log.info("userInfo:{}", userInfo);

        return userInfo;
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 获取到微信信息后，拼接参数，返回到tourl，进行下一步操作
     * @param toUrl
     * @param wechatUserInfo
     * @return
     */
    @Override
    public String getToUrl(String toUrl, WechatUserInfo wechatUserInfo) {
        StringBuilder builder = new StringBuilder(toUrl);
        if (!toUrl.contains("?")) {
            builder.append("?");
        }

        if (wechatUserInfo.getUserId() != null) {
            builder.append("&hasUser=1");
        }
        builder.append("&openid=").append(wechatUserInfo.getOpenid());

        String tempCode = cacheWechatUserInfo(wechatUserInfo);
        builder.append("&tempCode=").append(tempCode);

        builder.append("&nickname=").append(wechatUserInfo.getNickname());
        builder.append("&headimgurl=").append(wechatUserInfo.getHeadimgurl());

        return builder.toString();
    }

    private String cacheWechatUserInfo(WechatUserInfo wechatUserInfo) {
        String tempCode = UUID.randomUUID().toString();
        String key = prefixKey(tempCode);

        // 用tempCode和微信信息做个临时关系，后续的微信和账号绑定、微信登陆将会校验这个tempCode
        stringRedisTemplate.opsForValue().set(key, JSONObject.toJSONString(wechatUserInfo), 4, TimeUnit.HOURS);
        log.info("缓存微信信息:{},{}", tempCode, wechatUserInfo);

        return tempCode;
    }

    private String prefixKey(String key) {
        return "wechat:temp:" + key;
    }

    @Autowired
    private AppUserService appUserService;

    @Transactional
    @Override
    public void bindingUser(AppUser appUser, String tempCode, String openid) {
        WechatUserInfo wechatUserInfo = checkAndGetWechatUserInfo(tempCode, openid);

        UserCredential userCredential = new UserCredential(openid, CredentialType.WECHAT_OPENID.name(), appUser.getId());
        userCredentialsDao.save(userCredential);
        log.info("保存微信登陆凭证，{}", userCredential);

        if (StringUtils.isBlank(appUser.getHeadImgUrl())) {
            appUser.setHeadImgUrl(wechatUserInfo.getHeadimgurl());
            appUserService.updateAppUser(appUser);
        }

        wechatUserInfo.setUserId(appUser.getId());
        wechatDao.update(wechatUserInfo);
        log.info("{}，绑定微信成功，给微信设置用户id，{}", appUser, wechatUserInfo);
    }

    @Override
    public WechatUserInfo checkAndGetWechatUserInfo(String tempCode, String openid) {
        String key = prefixKey(tempCode);
        String string = stringRedisTemplate.opsForValue().get(key);
        if (string == null) {
            throw new IllegalArgumentException("无效的code");
        }

        WechatUserInfo wechatUserInfo = JSONObject.parseObject(string, WechatUserInfo.class);
        if (!wechatUserInfo.getOpenid().equals(openid)) {
            throw new IllegalArgumentException("无效的openid");
        }

        // 删除临时tempCode
        stringRedisTemplate.delete(tempCode);

        return wechatUserInfo;
    }

}
