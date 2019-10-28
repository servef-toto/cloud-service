package com.cloud.user.controller;

import com.cloud.common.utils.AppUserUtil;
import com.cloud.model.user.AppUser;
import com.cloud.model.user.WechatUserInfo;
import com.cloud.user.service.WechatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

/**
 * 详细的步骤如下：
 *
 * 　　1．用户关注微信公众账号。
 * 　　2．微信公众账号提供用户请求授权页面URL。
 * 　　3．用户点击授权页面URL，将向服务器发起请求
 * 　　4．服务器询问用户是否同意授权给微信公众账号(scope为snsapi_base时无此步骤)
 * 　　5．用户同意(scope为snsapi_base时无此步骤)
 * 　　6．服务器将CODE通过回调传给微信公众账号
 * 　　7．微信公众账号获得CODE
 * 　　8．微信公众账号通过CODE向服务器请求Access Token
 * 　　9．服务器返回Access Token和OpenID给微信公众账号
 * 　　10．微信公众账号通过Access Token向服务器请求用户信息(scope为snsapi_base时无此步骤)
 * 　　11．服务器将用户信息回送给微信公众账号(scope为snsapi_base时无此步骤)
 *
 *
 *前提：获取到APPID和AppSecret，也可以获取临时的
 *
 *
 *第一步：请求CODE
 * 第三方使用网站应用授权登录前请注意已获取相应网页授权作用域（scope=snsapi_login），则可以通过在PC端打开以下链接：
 * https://open.weixin.qq.com/connect/oauth2/authorize?appid=APPID&redirect_uri=REDIRECT_URI&response_type=code&scope=SCOPE&state=STATE#wechat_redirect
 * 注：若提示“该链接无法访问”，请检查参数是否填写错误，如redirect_uri的域名与审核时填写的授权域名不一致或scope不为snsapi_login。
 * 参数说明
 *  appid 公众号的唯一标识（这个就是我们前面申请的）
 *  redirect_uri 授权后重定向的回调链接地址
 *  response_type 返回类型，请填写code
 *  scope 应用授权作用域，snsapi_base （不弹出授权页面，直接跳转，只能获取用户openid），snsapi_userinfo （弹出授权页面，可通过openid拿到昵称、性别、所在地。并且，
 *                                    即使在未关注的情况下，只要用户授权，也能获取其信息）
 *  state 重定向后会带上state参数，开发者可以填写a-zA-Z0-9的参数值，最多128字节，该值会被微信原样返回，我们可以将其进行比对，防止别人的攻击。
 *  #wechat_redirect 直接在微信打开链接，可以不填此参数。做页面302重定向时候，必须带此参数
 * 返回说明
 *  用户允许授权后，将会重定向到redirect_uri的网址上，并且带上code和state参数
 *  若用户禁止授权，则重定向后不会带上code参数，仅会带上state参数
 *
 *
 *第二步：通过code获取access_token
 * https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code
 * 参数说明
 *  appid 公众号的唯一标识
 *  secret 公众号的appsecret
 *  code 填写第一步获取的code参数
 *  grant_type 填写为authorization_code
 * 返回说明
 *  access_token 网页授权接口调用凭证,注意：此access_token与基础支持的access_token不同
 *  expires_in access_token接口调用凭证超时时间，单位（秒）
 *  refresh_token 用户刷新access_token
 *  openid 用户唯一标识
 *  scope 用户授权的作用域，使用逗号（,）分隔
 *
 *
 *第二.一步：刷新access_token有效期
 * access_token是调用授权关系接口的调用凭证，由于access_token有效期（目前为2个小时）较短，
 * 当access_token超时后，可以使用refresh_token进行刷新，access_token刷新结果有两种：
 *      1：如果access_token已经超时，则会获取一个新的access_token
 *      2：如果access_token没有超时，access_token不会改变，但是会刷新这个access_token的超时时间
 * refresh_token拥有较长的有效期（30天），当refresh_token失效的后，需要用户重新授权。
 * https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=APPID&grant_type=refresh_token&refresh_token=REFRESH_TOKEN
 * 参数说明
 *   appid 公众号的唯一标识
 *   grant_type refresh_token
 *   refresh_token 填写access_token去刷新获取新的access_token
 * 返回说明
 *  access_token 网页授权接口调用凭证
 *  expires_in access_token接口调用凭证超时时间，单位（秒）
 *  refresh_token 用户刷新access_token
 *  openid 用户唯一标识
 *  scope 用户授权的作用域，使用逗号（,）分隔
 *
 *
 *第三步：通过access_token，openid调用接口获取用户信息
 * https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID
 * 参数说明
 *  access_token 网页授权接口调用凭证,注意：此access_token与基础支持的access_token不同
 *  openid 用户的唯一标识
 * 返回说明
 *  openid 用户的唯一标识
 *  nickname 用户昵称
 *  sex 用户的性别，值为1时是男性，值为2时是女性，值为0时是未知
 *  province 用户个人资料填写的省份
 *  city 普通用户个人资料填写的城市
 *  country 国家，如中国为CN
 *  headimgurl 用户头像，最后一个数值代表正方形头像大小（有0、46、64、96、132数值可选，0代表640*640正方形头像），用户没有头像时该项为空。若用户更换头像，原有头像URL将失效。
 *  privilege 用户特权信息，json 数组，如微信沃卡用户为（chinaunicom）
 *  unionid 只有在用户将公众号绑定到微信开放平台帐号后，才会出现该字段。详见：获取用户个人信息（UnionID机制）
 *
 */
@Slf4j
@RestController
@RequestMapping("/wechat")
public class WechatController {

    @Autowired
    private WechatService wechatService;

    /**
     * 引导到授权，第一步获取授权code
     * 说明一下：在application配置文件中配置了：
     * wechat:
     *   domain: http://api.gateway.com:8080/api-u
     *   infos:
     *     app1:
     *       appid: wx22cc2e471b5201b8
     *       secret: 56453462fa28e4fdf66eebece28ce391
     * 因此这里从前端传过来的app参数，是用来从配置文件中获取appid、secret
     *
     * @param app 配置文件中配置的那个微信公众号的配置信息
     * @param request
     * @param toUrl   授权后，跳转的页面url，注意url要转义
     * @return
     */
    @GetMapping("/{app}")
    public RedirectView toWechatAuthorize(@PathVariable String app, HttpServletRequest request,
                                          @RequestParam String toUrl) throws UnsupportedEncodingException {
        //引导到微信授权页https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=%s#wechat_redirect
        String url = wechatService.getWechatAuthorizeUrl(app, request, toUrl);

        return new RedirectView(url);
    }

    /**
     * 授权后，微信跳转到此接口,会带回"code", "state"参数
     * @return
     */
    @GetMapping(value = "/{app}/back", params = {"code", "state"})
    public RedirectView wechatBack(HttpServletRequest request, @PathVariable String app, String code, String state,
                                   @RequestParam String toUrl) {
        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("code不能为空");
        }
        if (StringUtils.isBlank(state)) {
            throw new IllegalArgumentException("state不能为空");
        }
        WechatUserInfo wechatUserInfo = wechatService.getWechatUserInfo(app, request, code, state);
        toUrl = wechatService.getToUrl(toUrl, wechatUserInfo);
        return new RedirectView(toUrl);
    }

    /**
     * 微信绑定用户
     *
     * @param tempCode
     * @param openid
     */
    @PostMapping(value = "/binding-user", params = {"tempCode", "openid"})
    public void bindingUser(String tempCode, String openid) {
        AppUser appUser = AppUserUtil.getLoginAppUser();
        if (appUser == null) {
            throw new IllegalArgumentException("非法请求");
        }

        log.info("绑定微信和用户：{},{},{}", appUser, openid, tempCode);
        wechatService.bindingUser(appUser, tempCode, openid);
    }

    /**
     * 微信登陆校验
     *
     * @param tempCode
     * @param openid
     */
    @GetMapping(value = "/login-check", params = {"tempCode", "openid"})
    public void wechatLoginCheck(String tempCode, String openid) {
        wechatService.checkAndGetWechatUserInfo(tempCode, openid);
    }
}
