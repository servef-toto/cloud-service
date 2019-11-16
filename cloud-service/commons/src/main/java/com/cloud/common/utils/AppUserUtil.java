package com.cloud.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.cloud.model.user.LoginAppUser;
import com.cloud.model.user.SysRole;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.provider.OAuth2Authentication;


public class AppUserUtil {
    /**
     * 获取登陆的 LoginAppUser
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static LoginAppUser getLoginAppUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null){
            LoginAppUser appUser = new LoginAppUser();
            appUser.setUsername("superadmin");
            appUser.setPassword("superadmin");

            Set<SysRole> roleSet = new HashSet<>();
            SysRole s = new SysRole();
            s.setId(Long.valueOf(1));
            s.setCode("SUPER_ADMIN");
            s.setName("超级管理员");
            roleSet.add(s);
            appUser.setSysRoles(roleSet);
            return appUser;
        }
        if (authentication instanceof OAuth2Authentication) {
            OAuth2Authentication oAuth2Auth = (OAuth2Authentication) authentication;
            authentication = oAuth2Auth.getUserAuthentication();

            if (authentication instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken authenticationToken = (UsernamePasswordAuthenticationToken) authentication;
                Object principal = authentication.getPrincipal();
                if (principal instanceof LoginAppUser) {
                    return (LoginAppUser) principal;
                }

                Map map = (Map) authenticationToken.getDetails();
                map = (Map) map.get("principal");

                return JSONObject.parseObject(JSONObject.toJSONString(map), LoginAppUser.class);
            }
        }

        return null;
    }
}
