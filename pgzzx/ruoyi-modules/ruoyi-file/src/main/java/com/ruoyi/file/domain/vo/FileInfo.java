package com.ruoyi.file.domain.vo;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName FileList
 * @Description TODO
 * @author: ltq
 * @date 2022/10/10 14:24
 * @Version 1.0
 */
@Data
public class FileInfo {

    private String filename;

    private Long size;

    private String time;

    private String label;

}
