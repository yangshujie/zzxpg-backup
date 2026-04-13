package com.ruoyi.service.impl.ooda;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.domain.ooda.TableOne;
import com.ruoyi.mapper.ooda.TableOneMapper;
import com.ruoyi.service.ooda.TableOneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TableOneImpl extends ServiceImpl<TableOneMapper, TableOne> implements TableOneService
{
    /**
     * 注入TableOneMapper
     */
    @Autowired
    private TableOneMapper tableOneMapper;

    @Override
    public Integer insertTableOne(String id, String name)
    {
        return tableOneMapper.insertTableOne(id, name);
    }

    @Override
    public Integer deleteTableOne(String id)
    {
        return tableOneMapper.deleteTableOne(id);
    }

    @Override
    public Integer updateTableOne(String id, String name)
    {
        return tableOneMapper.updateTableOne(id, name);
    }

    @Override
    public TableOne selectTableOne(String id)
    {
        return tableOneMapper.selectTableOne(id);
    }

    @Override
    public List<String> selectNameList(TableOne tableOne)
    {
        LambdaQueryWrapper<TableOne> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(tableOne.getName() != null, TableOne::getName, tableOne.getName());

        List<TableOne> tableOneList = tableOneMapper.selectList(queryWrapper);
        List<String> nameList = new ArrayList<>();

        for (TableOne result : tableOneList)
        {
            String name = result.getName();
            if (!nameList.contains(name))
            {
                nameList.add(name);
            }
        }

        return nameList;
    }
}
