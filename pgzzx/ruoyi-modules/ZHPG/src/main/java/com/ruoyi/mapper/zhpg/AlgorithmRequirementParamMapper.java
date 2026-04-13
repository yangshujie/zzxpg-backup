package com.ruoyi.mapper.zhpg;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.domain.zhpg.AlgorithmRequirementParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import java.util.List;

/**
 * 算法需求参数Mapper接口
 */
@Mapper
public interface AlgorithmRequirementParamMapper extends BaseMapper<AlgorithmRequirementParam> {

    /**
     * 根据需求ID查询参数列表
     */
    @Select("SELECT * FROM pgzc_algorithm_requirement_param WHERE requirement_id = #{requirementId} ORDER BY sort_order")
    List<AlgorithmRequirementParam> selectParamsByRequirementId(@Param("requirementId") Long requirementId);

    /**
     * 批量删除需求参数
     */
    @Delete("<script>DELETE FROM pgzc_algorithm_requirement_param WHERE requirement_id IN <foreach collection='requirementIds' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deleteParamsByRequirementIds(@Param("requirementIds") List<Long> requirementIds);
}
