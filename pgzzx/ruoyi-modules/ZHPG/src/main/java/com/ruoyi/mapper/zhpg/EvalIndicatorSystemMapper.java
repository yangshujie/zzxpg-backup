package com.ruoyi.mapper.zhpg;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.EvalIndicatorSystemSelectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评估指标体系Mapper接口
 */
@Mapper
public interface EvalIndicatorSystemMapper extends BaseMapper<EvalIndicatorSystem> {

    /**
     * 外部系统选择 - 查询所有指标体系（支持下拉框，支持关键字搜索）
     * @param keyword 关键字（模糊匹配：体系名称、装备类别、描述）
     * @return 指标体系列表
     */
    @Select("<script>" +
            "SELECT id AS indicatorSystemId, system_name AS indicatorSystemName, " +
            "       CASE WHEN refined_indicator_tree IS NOT NULL AND TRIM(refined_indicator_tree) != '' " +
            "            THEN refined_indicator_tree ELSE indicator_tree END AS treeData, description " +
            "FROM pgzc_indicator_system " +
            "WHERE del_flag = '0' " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (system_name LIKE CONCAT('%', #{keyword}, '%')" +
            " OR description LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            " ORDER BY id DESC" +
            "</script>")
    List<EvalIndicatorSystemSelectVO> selectIndicatorSystemListForSelect(@Param("keyword") String keyword);
}
