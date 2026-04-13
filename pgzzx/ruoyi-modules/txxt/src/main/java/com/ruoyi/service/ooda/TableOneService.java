package com.ruoyi.service.ooda;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.ooda.TableOne;

import java.util.List;

public interface TableOneService extends IService<TableOne>
{
    /**
     * 插入一条数据
     * @param id
     * @param name
     * @return Integer
     */
    Integer insertTableOne(String id, String name);

    /**
     * 删除一条数据
     * @param id
     * @return Integer
     */
    Integer deleteTableOne(String id);

    /**
     * 更新一条数据
     * @param id
     * @param name
     * @return Integer
     */
    Integer updateTableOne(String id, String name);

    /**
     * 查询一条数据
     * @param id
     * @return TableOne
     */
    TableOne selectTableOne(String id);

    List<String> selectNameList(TableOne tableOne);
}
