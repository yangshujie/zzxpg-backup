package com.ruoyi.zhpgcalc;

import com.ruoyi.common.security.annotation.EnableRyFeignClients;
import com.ruoyi.common.swagger.annotation.EnableCustomSwagger2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.scheduling.annotation.EnableScheduling;

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
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  综合计算执行器模块启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}
