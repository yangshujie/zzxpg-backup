package com.xxl.job.admin;

import com.ruoyi.common.security.annotation.EnableRyFeignClients;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import com.xxl.job.admin.core.cron.CronExpression;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.ParseException;

/**
 * @author xuxueli 2018-10-28 00:38:13
 */
@EnableRyFeignClients
@EnableCustomSwagger2
@SpringBootApplication
public class XxlJobAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxlJobAdminApplication.class, args);
    }

}
