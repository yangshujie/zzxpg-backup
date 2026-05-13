package com.ruoyi.common.report;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PlaceholderService {

    private static final Pattern EACH_START = Pattern.compile("\\{\\{#each\\s+([a-zA-Z0-9_]+)\\s*}}");
    private static final Pattern EACH_END = Pattern.compile("\\{\\{/each\\s*}}");
    private static final Pattern IF_START = Pattern.compile("\\{\\{#if\\s+([a-zA-Z0-9_\\.]+)\\s*}}");
    private static final Pattern IF_ELSE = Pattern.compile("\\{\\{else\\s*}}");
    private static final Pattern IF_END = Pattern.compile("\\{\\{/if\\s*}}");
    private static final Pattern EXPRESSION = Pattern.compile("\\{\\{=\\s*([^{}]+)\\s*}}");
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_\\.]+)\\s*}}");
    private static final Pattern ANY_TOKEN = Pattern.compile("\\{\\{([^{}]+)}}");

    public void validateTemplateTokens(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("template content cannot be empty");
        }

        int openCount = count(htmlContent, "{{");
        int closeCount = count(htmlContent, "}}");
        if (openCount != closeCount) {
            throw new IllegalArgumentException("placeholder braces are not balanced");
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
                if (loopDepth < 0) throw new IllegalArgumentException("illegal /each token");
                continue;
            }
            if (token.matches("#if\\s+[a-zA-Z0-9_\\.]+")) {
                ifDepth++;
                continue;
            }
            if ("else".equals(token)) {
                if (ifDepth <= 0) throw new IllegalArgumentException("illegal else token");
                continue;
            }
            if ("/if".equals(token)) {
                ifDepth--;
                if (ifDepth < 0) throw new IllegalArgumentException("illegal /if token");
                continue;
            }
            if (token.matches("=.+")) continue;
            if (!token.matches("[a-zA-Z0-9_\\.]+")) {
                throw new IllegalArgumentException("unsupported placeholder token: {{" + token + "}}");
            }
        }

        if (loopDepth != 0) throw new IllegalArgumentException("#each and /each counts do not match");
        if (ifDepth != 0) throw new IllegalArgumentException("#if and /if counts do not match");
    }

    public String toFreemarkerTemplate(String htmlContent) {
        String eachConverted = EACH_START.matcher(htmlContent).replaceAll("<#list $1 as item>");
        eachConverted = EACH_END.matcher(eachConverted).replaceAll("</#list>");
        eachConverted = replaceIfBlocks(eachConverted);
        eachConverted = IF_ELSE.matcher(eachConverted).replaceAll("<#else>");
        eachConverted = IF_END.matcher(eachConverted).replaceAll("</#if>");
        eachConverted = EXPRESSION.matcher(eachConverted).replaceAll("\\${($1)!''}");
        return VARIABLE.matcher(eachConverted).replaceAll("\\${$1!''}");
    }

    public List<String> extractPlaceholders(String htmlContent) {
        Set<String> placeholderSet = new LinkedHashSet<>();
        String source = htmlContent == null ? "" : htmlContent;
        Matcher eachMatcher = EACH_START.matcher(source);
        while (eachMatcher.find()) {
            placeholderSet.add(eachMatcher.group(1));
        }
        Matcher matcher = VARIABLE.matcher(source);
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

    private String replaceIfBlocks(String source) {
        Matcher matcher = IF_START.matcher(source);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String expression = matcher.group(1);
            matcher.appendReplacement(result, Matcher.quoteReplacement("<#if (" + expression + ")?? && (" + expression + ")?has_content>"));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
