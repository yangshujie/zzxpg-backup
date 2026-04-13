package com.ruoyi.file.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ceshi")
public class TestController extends BaseController {

    @RequestMapping("/test")
    public String gettest() {

        System.out.printf("通过接口调用哦");
        return "ceshi--test";
    }


}
