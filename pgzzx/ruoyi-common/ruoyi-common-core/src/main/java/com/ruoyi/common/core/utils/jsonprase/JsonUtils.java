package com.ruoyi.common.core.utils.jsonprase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ognl.Ognl;
import ognl.OgnlContext;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class JsonUtils {

    public static Map<String,Object> transferToMap(String json) {
//        Gson gson = new Gson();
//        Map<String, Object> map = gson.fromJson(json,
//                new TypeToken<Map<String, Object>>() {}.getType());
        //解决Integer自动转为Double的问题
        Gson gson = new GsonBuilder().registerTypeAdapter(new TypeToken<Map<String,Object>>(){}.getType(), new ObjectTypeAdapterRewrite()).create();
        Map<String, Object> map = gson.fromJson(json,
                new TypeToken<Map<String, Object>>() {}.getType());
        return map;
    }

    /**
     * 简化方法
     * @param json 原始的JSON数据
     * @param path OGNL规则表达式
     * @param clazz Value对应的目标类
     * @return clazz对应数据
     */
    public static <T> T getValue(String json, String path, Class<T> clazz) {
        try {
            Map map = transferToMap(json);
            OgnlContext context = new OgnlContext();
            context.setRoot(map);
            T value = (T) Ognl.getValue(path, context, context.getRoot());
            return value;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getValueFromMap(Map map, String path, Class<T> clazz) {
        try {
            OgnlContext context = new OgnlContext();
            context.setRoot(map);
            T value = (T) Ognl.getValue(path, context, context.getRoot());
            return value;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private String json1 = "{\n" +
            "\"showapi_res_error\": \"\",\n" +
            "\"showapi_res_id\": \"628cc9850de3769f06edbb49\",\n" +
            "\"showapi_res_code\": 0,\n" +
            "\"showapi_fee_num\": 1,\n" +
            "\"showapi_res_body\": {\"ret_code\":0,\"area\":\"南安\",\"areaid\":\"101230506\",\"areaCode\":\"350583\",\"hourList\": " +
            "[{\"weather_code\":\"07\",\"time\":\"202205242000\",\"area\":\"南安\",\"wind_direction\":\"东南风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨\",\"areaid\":\"101230506\",\"temperature\":\"20\"}, " +
            "{\"weather_code\":\"07\",\"time\":\"202205242100\",\"area\":\"南安\",\"wind_direction\":\"东风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨\",\"areaid\":\"101230506\",\"temperature\":\"20\"}, " +
            "{\"weather_code\":\"07\",\"time\":\"202205242200\",\"area\":\"南安\",\"wind_direction\":\"东风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨\",\"areaid\":\"101230506\",\"temperature\":\"20\"}, " +
            "{\"weather_code\":\"07\",\"time\":\"202205242300\",\"area\":\"南安\",\"wind_direction\":\"东风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨\",\"areaid\":\"101230506\",\"temperature\":\"20\"}, " +
            "{\"weather_code\":\"07\",\"time\":\"202205250000\",\"area\":\"南安" +
            "\",\"wind_direction\":\"南风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"21\"}, {\"weather_code\":\"07\",\"time\":\"202205250100\",\"area\":\"南安\",\"wind_direction\":\"西风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"21\"}, {\"weather_code\":\"07\",\"time\":\"202205250200\",\"area\":\"南安\",\"wind_direction\":\"西北风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"21\"}, {\"weather_code\":\"02\",\"time\":\"202205250300\",\"area\":\"南安\",\"wind_direction\":\"西北风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"阴" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"21\"}, {\"weather_code\":\"02\",\"time\":\"202205250400\",\"area\":\"南安" +
            "\",\"wind_direction\":\"西北风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"阴" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"21\"}, " +
            "{\"weather_code\":\"02\",\"time\":\"202205250500\",\"area\":\"南安\",\"wind_direction\":\"西风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"阴" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"21\"}, {\"weather_code\":\"07\",\"time\":\"202205250600\",\"area\":\"南安" +
            "\",\"wind_direction\":\"西北风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"21\"}, {\"weather_code\":\"07\",\"time\":\"202205250700\",\"area\":\"南安" +
            "\",\"wind_direction\":\"西北风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"21\"}, {\"weather_code\":\"07\",\"time\":\"202205250800\",\"area\":\"南安" +
            "\",\"wind_direction\":\"西北风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"21\"}, {\"weather_code\":\"07\",\"time\":\"202205250900\",\"area\":\"南安" +
            "\",\"wind_direction\":\"西南风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"22\"}, {\"weather_code\":\"07\",\"time\":\"202205251000\",\"area\":\"南安" +
            "\",\"wind_direction\":\"南风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"23\"}, " +
            "{\"weather_code\":\"07\",\"time\":\"202205251100\",\"area\":\"南安" +
            "\",\"wind_direction\":\"东风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"24\"}, {\"weather_code\":\"07\",\"time\":\"202205251200\",\"area\":\"南安" +
            "\",\"wind_direction\":\"东风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"24\"}, {\"weather_code\":\"07\",\"time\":\"202205251300\",\"area\":\"南安" +
            "\",\"wind_direction\":\"东风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"25\"}, {\"weather_code\":\"07\",\"time\":\"202205251400\",\"area\":\"南安" +
            "\",\"wind_direction\":\"东南风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"25\"}, {\"weather_code\":\"07\",\"time\":\"202205251500\",\"area\":\"南安" +
            "\",\"wind_direction\":\"东南风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"25\"},{\"weather_code\":\"07\",\"time\":\"202205251600\",\"area\":\"南安" +
            "\",\"wind_direction\":\"东南风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"24\"},{\"weather_code\":\"07\",\"time\":\"202205251700\",\"area\":\"南安" +
            "\",\"wind_direction\":\"东南风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨" +
            "\",\"areaid\":\"101230506\",\"temperature\":\"23\"},{\"weather_code\":\"07\",\"time\":\"202205251800\",\"area\":\"南安" +
            "\",\"wind_direction\":\"东南风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨\",\"areaid\":\"101230506\",\"temperature\":\"23\"}, {\"weather_code\":\"07\",\"time\":\"202205251900\",\"area\":\"南安\",\"wind_direction\":\"东南风\",\"wind_power\":\"0-3级 微风 <5.4m/s\",\"weather\":\"小雨\",\"areaid\":\"101230506\",\"temperature\":\"23\"}]}\n" +"}";

    /**
     * 超多层级JSON嵌套的快速提取
     */
    @Test
    public void case0(){
        String text = "{\n" +
                " \"a\": {\n" +
                " \"b\": {\n" +
                " \"c\": {\n" +
                " \"d\": {\n" +
                " \"e\": \"true\"\n" +
                " }\n" +
                " }\n" +
                " }\n" +
                " }\n" +
                "}";
        Map<String, Object> jsonMap = JsonUtils.transferToMap(text);
        System.out.println(jsonMap);
        String e = JsonUtils.getValue(text, "a.b.c.d.e", String.class);
        System.out.println(e);
    }

    //将JSON转为标准Map结构
    @Test
    public void case1(){
        Map<String, Object> jsonMap = JsonUtils.transferToMap(json1);
        System.out.println(jsonMap);
    }
    /**
     * OGNL直接提取数据，Value为子JSON对象的情况
     */
    @Test
    public void case2(){
        Map<String, Object> jsonMap = JsonUtils.transferToMap(json1);
        Map resBody = JsonUtils.getValue(json1, "showapi_res_body", Map.class);
        System.out.println(resBody);
    }

    /**
     * OGNL直接提取数据，Value为标准字符串的情况
     */
    @Test
    public void case3(){
        Map<String, Object> jsonMap = JsonUtils.transferToMap(json1);
        String value = JsonUtils.getValue(json1, "showapi_res_body.area", String.class);
        System.out.println(value);
    }

    /**
     * OGNL直接提取数据，Value为标准字符串的情况，Value为数组的情况
     */
    @Test
    public void case4(){
        Map<String, Object> jsonMap = JsonUtils.transferToMap(json1);
        List<Map> hourList = JsonUtils.getValue(json1, "showapi_res_body.hourList",List.class);
        System.out.println(hourList);
// 每一个集合对象都是List
        for(Map hour : hourList){
            System.out.println(hour);
        }
    }

    /**
     * 利用List语法获取第6个时点天气预报
     */
    @Test
    public void case5(){
        Map<String, Object> jsonMap = JsonUtils.transferToMap(json1);
//        List area = JsonUtils.getValue(json1,"showapi_res_body.hourList.{weather_code}", List.class);
        String area = JsonUtils.getValue(json1,"showapi_res_body.hourList[5].weather_code", String.class);
        System.out.println(area);
    }


    @Test
    public void case6(){
        String text = "{\n" +
                " \"a\": {\n" +
                " \"b\": {\n" +
                " \"c\": {\n" +
                " \"d\": {\n" +
                " \"e\": 8" +
                " }\n" +
                " }\n" +
                " }\n" +
                " }\n" +
                "}";
        Map<String, Object> jsonMap = JsonUtils.transferToMap(text);
        System.out.println(jsonMap);
        Integer e = JsonUtils.getValue(text, "a.b.c.d.e", Integer.class);
        System.out.println(e);
    }
}
