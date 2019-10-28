package com.cloud.notification.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * swagger文档
 * swagger 通过注解接口生成文档，包括接口名，请求方法，参数，返回信息等
 *
 * @Api: 修饰整个类，用于controller类上
 *
 * @ApiOperation: 描述一个接口，用户controller方法上
 *
 * @ApiParam: 单个参数描述
 *
 * @ApiModel: 用来对象接收参数, 即返回对象
 *
 * @ApiModelProperty: 对象接收参数时，描述对象的字段
 *
 * @ApiResponse: Http响应其中的描述，在ApiResonse中
 *
 * @ApiResponses: Http响应所有的描述，用在
 *
 * @ApiIgnore: 忽略这个API
 *
 * @ApiError: 发生错误的返回信息
 *
 * @ApiImplicitParam: 一个请求参数
 *
 * @ApiImplicitParam: 多个请求参数
 *
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Value("${my.swagger.groupName}")
	private String groupName;
	@Value("${my.swagger.title}")
	private String title;
	@Value("${my.swagger.version}")
	private String version;
	@Value("${my.swagger.contact.name}")
	private String name;
	@Value("${my.swagger.contact.url}")
	private String url;
	@Value("${my.swagger.contact.email}")
	private String email;

	@Bean
	public Docket docket() {
		return new Docket(DocumentationType.SWAGGER_2).groupName(StringUtils.isEmpty(groupName)?"短信中心swagger接口文档":groupName)
				.apiInfo(new ApiInfoBuilder().title(StringUtils.isEmpty(title)?"短信中心swagger接口文档":title)
						.contact(new Contact(StringUtils.isEmpty(name)?"":name
								, StringUtils.isEmpty(url)?"":url,
								StringUtils.isEmpty(email)?"":email))
						.version(StringUtils.isEmpty(version)?"1.0":version).build())
				.select().paths(PathSelectors.any()).build();
	}
}
