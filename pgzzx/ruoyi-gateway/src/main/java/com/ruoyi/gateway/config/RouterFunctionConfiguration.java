package com.ruoyi.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import com.ruoyi.gateway.handler.ValidateCodeHandler;

/**
 * 路由配置信息
 * 
 * @author ruoyi
 */
@Configuration
public class RouterFunctionConfiguration
{
    @Autowired
    private ValidateCodeHandler validateCodeHandler;

    @SuppressWarnings("rawtypes")  //告诉编译器忽略指定的警告，不用在编译完成后出现警告信息。
    @Bean
    public RouterFunction routerFunction()
    {
        return RouterFunctions.route( //RouterFunction为我们应用程序添加一个新的路由，这个路由需要绑定一个HandlerFunction，做为它的处理程序，里面可以添加业务代码，比如，你添加一个ImageCodeHandler，用来生产验证码。
                RequestPredicates.GET("/code").and(RequestPredicates.accept(MediaType.TEXT_PLAIN)),
                validateCodeHandler);
    }
}
