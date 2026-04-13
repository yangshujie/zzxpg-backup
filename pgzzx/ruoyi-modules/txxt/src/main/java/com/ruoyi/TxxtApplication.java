package com.ruoyi;


import com.ruoyi.common.security.annotation.EnableRyFeignClients;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRyFeignClients
@EnableScheduling
@EnableCustomSwagger2
@SpringBootApplication
@MapperScan("com.ruoyi.mapper.**")
public class TxxtApplication {
    public static void main(String[] args) {
        SpringApplication application=new SpringApplication(TxxtApplication.class);
//        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  体系协同模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}
