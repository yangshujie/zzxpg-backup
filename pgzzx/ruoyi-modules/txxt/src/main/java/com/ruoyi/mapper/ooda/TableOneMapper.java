package com.ruoyi.mapper.ooda;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.domain.ooda.TableOne;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TableOneMapper extends BaseMapper<TableOne>
{
    @Insert("insert into table_one(id,name) values ('${arg0}','${arg1}')")
    Integer insertTableOne(String id,String name);

    @Delete("delete from table_one where id = '${arg0}'")
    Integer deleteTableOne(String id);

    @Update("update table_one set name = '${arg1}' where id = '${arg0}'")
    Integer updateTableOne(String id,String name);

    @Select("select * from table_one where id = '${arg0}'")
    TableOne selectTableOne(String id);
}
