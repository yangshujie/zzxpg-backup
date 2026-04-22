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
            "       CASE WHEN refined_indicator_tree IS NOT NULL AND TRIM(refined_indicator_tree) != '' " +
            "            THEN refined_indicator_tree ELSE indicator_tree END AS treeData, " +
            "       description " +
            "FROM pgzc_indicator_system " +
            "WHERE del_flag = '0' " +
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
