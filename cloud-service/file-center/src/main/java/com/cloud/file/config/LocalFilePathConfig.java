package com.cloud.file.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 使系统加载jar包外的文件
 *
 * 上传文件存储路径肯定是在jar包外部的，这里不像传统war包是解压成文件夹的，因此这里要做个静态资源的映射处理
 *
 * @author admin008
 */
@Configuration
public class LocalFilePathConfig {

	/**
	 * 上传文件存储在本地的根路径
	 */
	@Value("${file.local.path}")
	private String localFilePath;

	/**
	 * url前缀
	 */
	@Value("${file.local.prefix}")
	public String localFilePrefix;

	/**
	 * registry.addResourceHandler(localFilePrefix + "/**")
	 * 						.addResourceLocations(ResourceUtils.FILE_URL_PREFIX + localFilePath + File.separator);
	 * 将url前缀和存储路径做了个映射
	 * @return
	 */
	@Bean
	public WebMvcConfigurer webMvcConfigurerAdapter() {
		return new WebMvcConfigurer() {
			/**
			 * 外部文件访问<br>
			 */
			@Override
			public void addResourceHandlers(ResourceHandlerRegistry registry) {
				registry.addResourceHandler(localFilePrefix + "/**")
						.addResourceLocations(ResourceUtils.FILE_URL_PREFIX + localFilePath + File.separator);
			}

		};
	}
}
