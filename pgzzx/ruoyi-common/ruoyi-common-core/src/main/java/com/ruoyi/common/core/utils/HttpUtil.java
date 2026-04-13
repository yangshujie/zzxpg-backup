package com.ruoyi.common.core.utils;

import com.alibaba.fastjson2.JSONObject;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author TGKY
 */
public class HttpUtil {
    public static JSONObject restApi(String url, HashMap<String, Object> paramMap, String method){
        JSONObject resultJson = new JSONObject();
        RestTemplate restTemplate=new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        headers.add("Accept-Charset", "application/json;charset=utf-8");
        if ("GET".equals(method.toUpperCase()) ) {
//            Map<String,Object> paramMap= new HashMap<>();
//            for (Map.Entry param : params.entrySet()) {
//                paramMap.put((String) param.getKey(),param.getValue());
//            }
//            paramMap.put("input", params);
            //该方法提供了三个参数，其中url为请求的地址，responseType为请求响应body的包装类型，urlVariables为url中的参数绑定，该方法的参考调用如下：http://USER-SERVICE/user?name={name)
            ResponseEntity<String> result = restTemplate.getForEntity(url,String.class,paramMap);
            resultJson=JSONObject.parseObject(result.getBody());

        } else if ("POST".equals(method.toUpperCase())) {
//            Map<String, Object> requestBody = new HashMap<>();
//            for (Map.Entry param : params.entrySet()) {
//                requestBody.put((String) param.getKey(),param.getValue());
//            }
//            requestBody.put("input", params);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(paramMap, headers);
            ResponseEntity<String> result = restTemplate.postForEntity(url, httpEntity, String.class);
            resultJson=JSONObject.parseObject(result.getBody());
        }
        return resultJson;
    }

    @Test
    public void test(){
        String url = "http://5.30.73.85:8080/serv_handle/process/v1/SCBasicInfoNew2temp?taskCode=BD3G01";
        HashMap<String, Object> param = new HashMap<>();
        param.put("taskCode", "BD3G01");
        System.out.println(restApi(url, param, "GET"));
    }
}
