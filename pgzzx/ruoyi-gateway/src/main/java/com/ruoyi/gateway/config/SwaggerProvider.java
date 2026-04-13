package com.ruoyi.gateway.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * 聚合系统接口
 * 
 * @author ruoyi
 */
@Component
public class SwaggerProvider implements SwaggerResourcesProvider, WebFluxConfigurer
{
    /**
     * Swagger2默认的url后缀
     */
    public static final String SWAGGER2URL = "/v2/api-docs";
    private final GatewayProperties gatewayProperties;
    /**
     * 网关应用名称
     */
    @Value("${spring.application.name}")
    private String applicationName;

    public SwaggerProvider(GatewayProperties gatewayProperties)
    {
        this.gatewayProperties = gatewayProperties;
    }
    /**
     * 聚合其他服务接口,,这个地方修改。。。。。
     *
     * @return
     */
    @Override
    public List<SwaggerResource> get()
    {
        List<SwaggerResource> resources = new ArrayList<>();
        Set<String> existsUrl = new LinkedHashSet<>();

        for (RouteDefinition routeDefinition : gatewayProperties.getRoutes())
        {
            if (routeDefinition.getUri() == null || routeDefinition.getUri().getHost() == null)
            {
                continue;
            }
            String host = routeDefinition.getUri().getHost();
            if (applicationName.equals(host))
            {
                continue;
            }
            String routeId = routeDefinition.getId();

            String pathPattern = "";
            for (PredicateDefinition predicate : routeDefinition.getPredicates())
            {
                if (!"Path".equalsIgnoreCase(predicate.getName()) || predicate.getArgs() == null || predicate.getArgs().isEmpty())
                {
                    continue;
                }
                // Path 断言 args 形如 {_genkey_0=/system/**}
                pathPattern = predicate.getArgs().values().iterator().next();
                break;
            }
            if (pathPattern == null || pathPattern.isEmpty())
            {
                continue;
            }

            String apiDocsPath = pathPattern.replace("/**", SWAGGER2URL);
            if (!apiDocsPath.startsWith("/"))
            {
                apiDocsPath = "/" + apiDocsPath;
            }
            if (existsUrl.add(apiDocsPath))
            {
                String displayName = (routeId == null || routeId.isEmpty()) ? host : routeId;
                resources.add(swaggerResource(displayName, apiDocsPath));
            }
        }
        return resources;
    }
    private SwaggerResource swaggerResource(String name, String location){
        SwaggerResource swaggerResource = new SwaggerResource();
        swaggerResource.setName(name);
        swaggerResource.setLocation(location);
        swaggerResource.setSwaggerVersion("2.0");
        return swaggerResource;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
        /** swagger-ui 地址 */
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/");
    }
}


