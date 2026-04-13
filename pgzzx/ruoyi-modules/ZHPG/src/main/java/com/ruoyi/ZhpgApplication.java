package com.ruoyi;

import com.ruoyi.common.security.annotation.EnableRyFeignClients;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRyFeignClients
@EnableScheduling
@EnableCustomSwagger2
@SpringBootApplication
@MapperScan("com.ruoyi.mapper.**")
public class ZhpgApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ZhpgApplication.class);
        application.run(args);
        System.out.println("=== 试验任务综合评估系统模块启动成功 ===");
    }
}
