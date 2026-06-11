package com.ruoyi.zhpgcalc;

import com.ruoyi.common.security.annotation.EnableRyFeignClients;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 综合计算流程-XXL-Job执行器启动类
 * <p>
 * 对应 CalcFlowTemplate 中 scheduleConfig 阶段的调度策略,
 * 在 XXL-Job Admin 中注册 appname = zhpg-calc-executor,
 * 供流程模板通过 executorHandler 发起计算任务。
 */
@EnableRyFeignClients
@EnableScheduling
@EnableCustomSwagger2
@SpringBootApplication(scanBasePackages = {"com.ruoyi.zhpgcalc", "com.ruoyi.system.api.factory"})
public class ZhpgCalcExecutorApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ZhpgCalcExecutorApplication.class);
        application.setBannerMode(Banner.Mode.OFF);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        ConfigurableApplicationContext context = application.run(args);
        printStartedBanner(context);
    }

    private static void printStartedBanner(ConfigurableApplicationContext context) {
        String appName = context.getEnvironment().getProperty("spring.application.name", "zhpg-calc-executor");
        try (InputStream inputStream = ZhpgCalcExecutorApplication.class.getClassLoader().getResourceAsStream("banner.txt")) {
            if (inputStream == null) {
                System.out.println("=== 综合计算流程执行器启动成功 ===");
                return;
            }
            String banner = readToString(inputStream)
                    .replace("${spring-boot.version}", SpringBootVersion.getVersion())
                    .replace("${spring.application.name}", appName);
            System.out.println(banner);
        } catch (IOException e) {
            System.out.println("=== 综合计算流程执行器启动成功 ===");
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
