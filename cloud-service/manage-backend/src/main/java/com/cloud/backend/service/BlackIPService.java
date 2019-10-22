package com.cloud.backend.service;

import com.cloud.backend.model.BlackIP;
import com.cloud.model.common.Page;

import java.util.Map;

public interface BlackIPService {

	void save(BlackIP blackIP);

	void delete(String ip);

	Page<BlackIP> findBlackIPs(Map<String, Object> params);

}
