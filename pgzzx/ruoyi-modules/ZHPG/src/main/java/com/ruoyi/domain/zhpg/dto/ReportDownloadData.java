package com.ruoyi.domain.zhpg.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 报告下载数据
 */
@Getter
@AllArgsConstructor
public class ReportDownloadData {
    private final String fileName;
    private final String contentType;
    private final byte[] bytes;
}
