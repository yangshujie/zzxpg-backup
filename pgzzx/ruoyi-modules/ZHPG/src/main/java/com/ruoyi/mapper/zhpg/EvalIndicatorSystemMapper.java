package com.ruoyi.mapper.zhpg;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.EvalIndicatorSystemSelectVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EvalIndicatorSystemMapper extends BaseMapper<EvalIndicatorSystem> {

    @Select("<script>" +
            "SELECT id AS indicatorSystemId, " +
            "       system_name AS indicatorSystemName, " +
            "       requirement_id AS requirementId, " +
            "       CASE WHEN indicator_tree_weight IS NOT NULL AND TRIM(indicator_tree_weight) != '' " +
            "            THEN indicator_tree_weight ELSE indicator_tree END AS treeData, " +
            "       description " +
            "FROM pgzc_indicator_system " +
            "WHERE COALESCE(is_template, 0) = 0 " +
            "  AND requirement_id IS NOT NULL " +
            "<if test='requirementId != null'>" +
            "  AND requirement_id = #{requirementId} " +
            "</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (system_name LIKE CONCAT('%', #{keyword}, '%') " +
            "   OR description LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY id DESC" +
            "</script>")
    List<EvalIndicatorSystemSelectVO> selectIndicatorSystemListForSelect(@Param("keyword") String keyword,
                                                                         @Param("requirementId") Long requirementId);
}
