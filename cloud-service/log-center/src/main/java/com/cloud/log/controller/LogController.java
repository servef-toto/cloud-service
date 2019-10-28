package com.cloud.log.controller;

import com.cloud.log.service.LogService;
import com.cloud.model.common.Page;
import com.cloud.model.log.Log;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(value = "日志管理控制")
@RestController
public class LogController {

	@Autowired
	private LogService logService;

	@ApiOperation(value = "保存日志",notes = "保存日志")
	@PostMapping("/logs-anon/internal")
	public void save(@RequestBody Log log) {
		logService.save(log);
	}

	/**
	 * 日志模块<br>
	 * 2018.07.29作废
	 *
	 * @Deprecated:若某类或某方法加上该注解之后，表示此方法或类不再建议使用，调用时也会出现删除线，但并不代表不能用，只是说，不推荐使用，因为还有更好的方法可以调用。
	 */
	@ApiOperation(value = "日志模块-不再建议使用",notes = "权限：“log:query”，日志模块，不再建议使用")
	@Deprecated
	@PreAuthorize("hasAuthority('log:query')")
	@GetMapping("/logs-modules")
	public Map<String, String> logModule() {
		return com.cloud.model.log.constants.LogModule.MODULES;
	}

	/**
	 * 日志查询
	 * 
	 * @param params
	 * @return
	 */
	@ApiOperation(value = "日志查询",notes = "权限：“log:query”，日志查询")
	@PreAuthorize("hasAuthority('log:query')")
	@GetMapping("/logs")
	public Page<Log> findLogs(@RequestParam Map<String, Object> params) {
		return logService.findLogs(params);
	}

}
