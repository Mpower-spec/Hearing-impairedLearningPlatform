package com.hearing.platform.config;

import com.hearing.platform.security.AuthInterceptor; // 鉴权拦截器

import org.springframework.context.annotation.Configuration; // 配置类标记
import org.springframework.web.servlet.config.annotation.CorsRegistry; // CORS 注册表
import org.springframework.web.servlet.config.annotation.InterceptorRegistry; // 拦截器注册表
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry; // 静态资源注册表
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer; // MVC 定制接口

import java.nio.file.Path; // 文件路径工具

/**
 * Web MVC 配置。
 * 负责三件事：注册鉴权拦截器、映射上传文件静态目录、配置跨域。
 */
@Configuration // 声明为 Spring 配置类
public class WebConfig implements WebMvcConfigurer { // 实现 MVC 定制接口

    private final AuthInterceptor authInterceptor; // 鉴权拦截器
    private final AppProperties properties; // 应用配置（提供上传根目录）

    /**
     * 构造器注入所需依赖。
     * @param authInterceptor 鉴权拦截器
     * @param properties 应用配置
     */
    public WebConfig(AuthInterceptor authInterceptor, AppProperties properties) {
        this.authInterceptor = authInterceptor;
        this.properties = properties;
    }

    /**
     * 注册鉴权拦截器。
     * 拦截所有 /api/** 请求，但放行登录接口。
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**") // 需鉴权的路径
                .excludePathPatterns("/api/auth/login"); // 登录无需 token
    }

    /**
     * 映射上传文件的静态访问路径。
     * 将 /files/** 指向本地上传根目录，供前端直接下载/预览。
     * @param registry 静态资源注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把上传根目录转为绝对 file URI 作为资源位置
        String location = Path.of(properties.getUploadRoot()).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/"; // 补尾部斜杠，保证子路径正确拼接
        }
        registry.addResourceHandler("/files/**").addResourceLocations(location);
    }

    /**
     * 配置跨域规则。
     * 允许指定的本地/内网前端来源携带凭证访问。
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 对所有路径生效
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*", "http://26.255.29.43:*", "http://10.157.172.135:*") // 允许的前端来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方法
                .allowedHeaders("*") // 允许全部请求头
                .allowCredentials(true); // 允许携带 Cookie/凭证
    }
}
