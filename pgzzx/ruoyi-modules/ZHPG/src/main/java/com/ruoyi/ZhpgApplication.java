package com.ruoyi;

import com.ruoyi.common.security.annotation.EnableRyFeignClients;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@EnableRyFeignClients
@EnableScheduling
@EnableCustomSwagger2
@SpringBootApplication
@MapperScan("com.ruoyi.mapper.**")
public class ZhpgApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ZhpgApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        ConfigurableApplicationContext context = application.run(args);
        printStartedBanner(context);
    }

    private static void printStartedBanner(ConfigurableApplicationContext context) {
        String appName = context.getEnvironment().getProperty("spring.application.name", "zhpg");
        try (InputStream inputStream = ZhpgApplication.class.getClassLoader().getResourceAsStream("banner.txt")) {
            if (inputStream == null) {
                System.out.println("=== 试验任务综合评估系统模块启动成功 ===");
                return;
            }
            String banner = readToString(inputStream)
                    .replace("${spring-boot.version}", SpringBootVersion.getVersion())
                    .replace("${spring.application.name}", appName);
            System.out.println(banner);
        } catch (IOException e) {
            System.out.println("=== 试验任务综合评估系统模块启动成功 ===");
        }
    }

    private static String readToString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }
}
