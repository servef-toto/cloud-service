package com.cloud.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 微信网页授权的调用
 */
@Controller
@RequestMapping(value="/open/weixin")
public class WeiXinController {

    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final String TOKEN = "luuservice";

    private static final String WXUSERINFO = "WXUSERINFO";

    @RequestMapping(value="/getWeiXinMethod",method=RequestMethod.GET)
    @ResponseBody
    public void getWeiXinMethod(HttpServletRequest request, HttpServletResponse response) throws IOException {
        boolean validate = validate(request);
        if (validate) {
            response.getWriter().write(request.getParameter("echostr"));
            response.getWriter().close();
        }

    }

    /**
     * 开发者通过检验signature对请求进行校验。若确认此次GET请求来自微信服务器，请原样返回echostr参数内容，则接入生效，成为开发者成功，否则接入失败。加密/校验流程如下：
     *
     * 1）将token、timestamp、nonce三个参数进行字典序排序
     * 2）将三个参数字符串拼接成一个字符串进行sha1加密
     * 3）开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
     * @param req
     * @return
     * @throws IOException
     */
    private boolean validate(HttpServletRequest req) throws IOException {
        //微信加密签名 signature结合了开发者填写的token参数和请求中的timestamp参数、nonce参数。
        String signature = req.getParameter("signature");
        //时间戳
        String timestamp = req.getParameter("timestamp");
        //随机数
        String nonce = req.getParameter("nonce");

        List<String> list = new ArrayList<String>();
        //设置token 值为haida
        list.add(TOKEN);
        list.add(timestamp);
        list.add(nonce);
        //将token、timestamp、nonce三个参数进行字典序排序
        Collections.sort(list);
        //将三个参数字符串拼接成一个字符串进行sha1加密
        String s = "";
        for (int i = 0; i < list.size(); i++) {
            s += (String) list.get(i);
        }
        //进行sha1加密，开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
        if (encode("SHA1", s).equalsIgnoreCase(signature)) {
            return true;
        } else {
            return false;
        }
    }

    public static String encode(String algorithm, String str) {
        if (str == null) {
            return null;
        }
        try {
            //Java自带的加密类
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            //转为byte
            messageDigest.update(str.getBytes());
            return getFormattedText(messageDigest.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getFormattedText(byte[] bytes) {
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(len * 2);
        // 把密文转换成十六进制的字符串形式
        for (int j = 0; j < len; j++) {
            buf.append(HEX_DIGITS[(bytes[j] >> 4) & 0x0f]);
            buf.append(HEX_DIGITS[bytes[j] & 0x0f]);
        }
        return buf.toString();
    }

    private static String create_nonce_str() {
        return UUID.randomUUID().toString();
    }

    private static String create_timestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }

    /**
     * 获取当前系统时间 用来判断access_token是否过期
     * @return
     */
    public static String getTime(){
        Date dt=new Date();
        SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(dt);
    }
}
