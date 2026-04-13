package com.ruoyi.zhpgcalc.report;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 占位符处理服务
 * 将自定义占位符语法转换为Freemarker模板语法
 */
@Slf4j
public class PlaceholderService {

    private static final Pattern EACH_START = Pattern.compile("\\{\\{#each\\s+([a-zA-Z0-9_]+)\\s*}}");
    private static final Pattern EACH_END = Pattern.compile("\\{\\{/each\\s*}}");
    private static final Pattern IF_START = Pattern.compile("\\{\\{#if\\s+([a-zA-Z0-9_\\.]+)\\s*}}");
    private static final Pattern IF_ELSE = Pattern.compile("\\{\\{else\\s*}}");
    private static final Pattern IF_END = Pattern.compile("\\{\\{/if\\s*}}");
    private static final Pattern EXPRESSION = Pattern.compile("\\{\\{=\\s*([^{}]+)\\s*}}");
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_\\.]+)\\s*}}");
    private static final Pattern ANY_TOKEN = Pattern.compile("\\{\\{([^{}]+)}}");

    /**
     * 验证模板占位符是否合法
     */
    public void validateTemplateTokens(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("模板内容不能为空");
        }

        int openCount = count(htmlContent, "{{");
        int closeCount = count(htmlContent, "}}");
        if (openCount != closeCount) {
            throw new IllegalArgumentException("占位符括号未正确闭合: 开括号=" + openCount + ", 闭括号=" + closeCount);
        }

        Matcher tokenMatcher = ANY_TOKEN.matcher(htmlContent);
        int loopDepth = 0;
        int ifDepth = 0;
        while (tokenMatcher.find()) {
            String token = tokenMatcher.group(1).trim();
            if (token.matches("#each\\s+[a-zA-Z0-9_]+")) {
                loopDepth++;
                continue;
            }
            if ("/each".equals(token)) {
                loopDepth--;
                if (loopDepth < 0) throw new IllegalArgumentException("存在非法 /each 标签");
                continue;
            }
            if (token.matches("#if\\s+[a-zA-Z0-9_\\.]+")) {
                ifDepth++;
                continue;
            }
            if ("else".equals(token)) {
                if (ifDepth <= 0) throw new IllegalArgumentException("存在非法 else 标签");
                continue;
            }
            if ("/if".equals(token)) {
                ifDepth--;
                if (ifDepth < 0) throw new IllegalArgumentException("存在非法 /if 标签");
                continue;
            }
            if (token.matches("=.+")) continue;
            if (!token.matches("[a-zA-Z0-9_\\.]+")) {
                throw new IllegalArgumentException("存在不支持的占位符: {{" + token + "}}");
            }
        }

        if (loopDepth != 0) throw new IllegalArgumentException("#each 与 /each 数量不匹配");
        if (ifDepth != 0) throw new IllegalArgumentException("#if 与 /if 数量不匹配");
    }

    /**
     * 转换为Freemarker模板
     */
    public String toFreemarkerTemplate(String htmlContent) {
        String eachConverted = EACH_START.matcher(htmlContent).replaceAll("<#list $1 as item>");
        eachConverted = EACH_END.matcher(eachConverted).replaceAll("</#list>");
        eachConverted = IF_START.matcher(eachConverted).replaceAll("<#if $1>");
        eachConverted = IF_ELSE.matcher(eachConverted).replaceAll("<#else>");
        eachConverted = IF_END.matcher(eachConverted).replaceAll("</#if>");
        eachConverted = EXPRESSION.matcher(eachConverted).replaceAll("\\${($1)!''}");
        return VARIABLE.matcher(eachConverted).replaceAll("\\${$1!''}");
    }

    /**
     * 提取占位符列表
     */
    public List<String> extractPlaceholders(String htmlContent) {
        Set<String> placeholderSet = new LinkedHashSet<>();
        Matcher matcher = VARIABLE.matcher(htmlContent);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!key.startsWith("item.")) {
                placeholderSet.add(key);
            }
        }
        return new ArrayList<>(placeholderSet);
    }

    private int count(String source, String token) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }
}
