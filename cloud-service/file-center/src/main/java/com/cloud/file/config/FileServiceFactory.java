package com.cloud.file.config;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.cloud.file.model.FileType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.cloud.file.service.FileService;

/**
 * FileService工厂
 * 将各个实现类放入map，根据上传文件时指定的存储位置，进行路由匹配合适的文件存储service
 * LOCAL：存储本地
 * ALIYUN：存储阿里云
 *
 * @author admin008
 */
@Configuration
public class FileServiceFactory {

	/**
	 * 用来匹配选择service
	 */
	private Map<FileType, FileService> map = new HashMap<>();

	/**
	 * local存储
	 */
	@Autowired
	private FileService localFileServiceImpl;
	/**
	 * aliyun存储
	 */
	@Autowired
	private FileService aliyunFileServiceImpl;

	/**
	 * PostConstruct 注释用于在依赖关系注入完成之后需要执行的方法上，以执行任何初始化。
	 * 1.@PostConstruct说明
	 *      被@PostConstruct修饰的方法会在服务器加载Servlet的时候运行，并且只会被服务器调用一次，
	 *      类似于Serclet的inti()方法。被@PostConstruct修饰的方法会在构造函数之后，init()方法之前运行。
	 *
	 * 2.@PreConstruct说明
	 *      被@PreConstruct修饰的方法会在服务器卸载Servlet的时候运行，并且只会被服务器调用一次，
	 *      类似于Servlet的destroy()方法。被@PreConstruct修饰的方法会在destroy()方法之后运行，在Servlet被彻底卸载之前。（详见下面的程序实践）
	 */
	@PostConstruct
	public void init() {
		map.put(FileType.LOCAL, localFileServiceImpl);
		map.put(FileType.ALIYUN, aliyunFileServiceImpl);
	}

	/**
	 * 根据文件源获取具体的实现类
	 *
	 * @param fileType
	 * @return
	 */
	public FileService getFileService(String fileType) {
		if (StringUtils.isBlank(fileType)) {// 默认用本地存储
			return localFileServiceImpl;
		}

		FileService fileService = map.get(FileType.valueOf(fileType));
		if (fileService == null) {
			throw new IllegalArgumentException("请检查FileServiceFactory类的init方法，看是否有" + fileType + "对应的实现类");
		}

		return fileService;
	}
}
