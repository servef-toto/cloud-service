package com.cloud.notification.service.impl;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.cloud.notification.model.Sms;
import com.cloud.notification.model.VerificationCode;
import com.cloud.notification.service.SmsService;
import com.cloud.notification.service.VerificationCodeService;
import com.cloud.notification.utils.Util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

	/**
	 * 短信验证码有效期（单位：分钟）
	 */
	@Value("${sms.expire-minute:15}")
	private Integer expireMinute;
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	@Autowired
	private SmsService smsService;

	@Transactional
	@Override
	public VerificationCode generateCode(String phone) {
		String uuid = UUID.randomUUID().toString();
		//生成6为编码
		String code = Util.randomCode(6);

		Map<String, String> map = new HashMap<>(2);
		map.put("code", code);
		map.put("phone", phone);

		//发送验证码，缓存手机号和验证码{key=uuid,value={code,phone}},缓存时长expireMinute，TimeUnit.MINUTES分钟
		//stringRedisTemplate.opsForValue();　　//操作字符串
		stringRedisTemplate.opsForValue().set(smsRedisKey(uuid), JSONObject.toJSONString(map), expireMinute,
				TimeUnit.MINUTES);
		log.info("缓存验证码：{}", map);

		//保存sms数据和发送短信
		saveSmsAndSendCode(phone, code);

		//返回uuid给前端，前端会利用uuid再来后端校验
		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setKey(uuid);
		return verificationCode;
	}

	/**
	 * 保存短信记录，并发送短信
	 * 
	 * @param phone
	 * @param code
	 */
	private void saveSmsAndSendCode(String phone, String code) {
		checkTodaySendCount(phone);

		Sms sms = new Sms();
		sms.setPhone(phone);

		Map<String, String> params = new HashMap<>();
		params.put("code", code);

		//保存短信记录
		smsService.save(sms, params);
		//发送短信
		smsService.sendSmsMsg(sms);

		// 当天发送验证码次数+1
		String countKey = countKey(phone);
		//通过increment(K key, long delta)方法以增量方式存储long值，如果刚新建的值，则初始是1，后面开始自增1
		stringRedisTemplate.opsForValue().increment(countKey, 1L);
		//设置缓存时长为1天
		stringRedisTemplate.expire(countKey, 1, TimeUnit.DAYS);
	}

	@Value("${sms.day-count:30}")
	private Integer dayCount;

	/**
	 * 获取当天发送验证码次数
	 * 
	 * @param phone
	 * @return
	 */
	private void checkTodaySendCount(String phone) {
		String value = stringRedisTemplate.opsForValue().get(countKey(phone));
		if (value != null) {
			Integer count = Integer.parseInt(value);
			if (count > dayCount) {
				throw new IllegalArgumentException("已超过当天最大次数");
			}
		}

	}

	private String countKey(String phone) {
		return "sms:count:" + LocalDate.now().toString() + ":" + phone;
	}

	/**
	 * redis中key加上前缀
	 *
	 * @param str
	 * @return
	 */
	private String smsRedisKey(String str) {
		return "sms:" + str;
	}

	@Override
	public String matcheCodeAndGetPhone(String key, String code, Boolean delete, Integer second) {
		key = smsRedisKey(key);

		String value = stringRedisTemplate.opsForValue().get(key);
		if (value != null) {
			JSONObject json = JSONObject.parseObject(value);
			if (code != null && code.equals(json.getString("code"))) {
				log.info("验证码校验成功：{}", value);

				if (delete == null || delete) {
					stringRedisTemplate.delete(key);
				}

				if (delete == Boolean.FALSE && second != null && second > 0) {
					stringRedisTemplate.expire(key, second, TimeUnit.SECONDS);
				}

				return json.getString("phone");
			}

		}

		return null;
	}
}
