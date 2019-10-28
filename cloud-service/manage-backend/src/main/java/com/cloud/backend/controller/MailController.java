package com.cloud.backend.controller;

import com.cloud.backend.service.MailService;
import com.cloud.model.common.Page;
import com.cloud.model.log.LogAnnotation;
import com.cloud.model.mail.Mail;
import feign.FeignException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Api(value = "邮件管理控制")
@RestController
@RequestMapping("/mails")
@RefreshScope
public class MailController {

    /**
     * 动态配置测试
     */
    @Value("${my.swagger.version}")
    private String version;
    @GetMapping("version")
    public String main() {
        return version;
    }

    /**
     * session 共享测试
     * @param request
     * @return
     */
    @RequestMapping(value="/getSessionId")
    @ResponseBody
    public String getSessionId(HttpServletRequest request){
        Object o = request.getSession().getAttribute("springboot");
        if(o == null){
            o = "spring boot 牛逼了!!!有端口"+request.getLocalPort()+"生成";
            request.getSession().setAttribute("springboot", o);
        }
        return "端口=" + request.getLocalPort() +  " sessionId=" + request.getSession().getId() +"<br/>"+o;
    }



    @Autowired
    private MailService mailService;

    @ApiOperation(value = "查找邮件",notes = "权限：“mail:query”，查找邮件")
    @PreAuthorize("hasAuthority('mail:query')")
    @GetMapping("/{id}")
    public Mail findById(@PathVariable Long id) {
        return mailService.findById(id);
    }

    @PreAuthorize("hasAuthority('mail:query')")
    @GetMapping
    public Page<Mail> findMails(@RequestParam Map<String, Object> params) {
        return mailService.findMails(params);
    }

    /**
     * 保存邮件
     *
     * @param mail
     * @param send 是否发送邮件
     * @return
     */
    @ApiOperation(value = "保存发送邮件",notes = "权限：“mail:save”，保存发送邮件")
    @LogAnnotation(module = "保存邮件")
    @PreAuthorize("hasAuthority('mail:save')")
    @PostMapping
    public Mail save(@RequestBody Mail mail, Boolean send) {
        mailService.saveMail(mail);
        if (Boolean.TRUE == send) {
            mailService.sendMail(mail);
        }

        return mail;
    }

    /**
     * 修改邮件
     *
     * @param mail
     * @param send 是否发送
     * @return
     */
    @ApiOperation(value = "修改邮件",notes = "权限：“mail:update”，修改邮件")
    @LogAnnotation(module = "修改邮件")
    @PreAuthorize("hasAuthority('mail:update')")
    @PutMapping
    public Mail update(@RequestBody Mail mail, Boolean send) {
        mailService.updateMail(mail);
        if (Boolean.TRUE == send) {
            mailService.sendMail(mail);
        }

        return mail;
    }


}
