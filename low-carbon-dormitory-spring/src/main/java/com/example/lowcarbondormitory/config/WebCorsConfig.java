package com.example.lowcarbondormitory.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.example.lowcarbondormitory.service.admin.AdminRewardImageStorageService;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    private final AuthTokenInterceptor authTokenInterceptor;
    private final AdminRewardImageStorageService adminRewardImageStorageService;

    public WebCorsConfig(
            AuthTokenInterceptor authTokenInterceptor,
            AdminRewardImageStorageService adminRewardImageStorageService
    ) {
        this.authTokenInterceptor = authTokenInterceptor;
        this.adminRewardImageStorageService = adminRewardImageStorageService;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authTokenInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(adminRewardImageStorageService.getResourcePattern())
                .addResourceLocations(adminRewardImageStorageService.getResourceLocation());
    }
}
