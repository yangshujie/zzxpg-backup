package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.domain.zhpg.EvalIndicator;
import com.ruoyi.mapper.zhpg.EvalIndicatorMapper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EvalIndicatorServiceImplTest {

    @Test
    public void updateIndicatorPreservesExistingLeafCalcMethodWhenIncomingPartialUpdateOmitsIt() throws Exception {
        EvalIndicatorMapper mapper = mock(EvalIndicatorMapper.class);

        EvalIndicator existing = new EvalIndicator()
                .setId(665L)
                .setIdCode("ind_665")
                .setIndicatorName("干扰动作时间戳")
                .setIndicatorType("space_defense")
                .setParentId(0L)
                .setIsBottomNode(1)
                .setIsTemplate(0)
                .setCalcMethod("{\"data\":\"ind_665\",\"method\":{\"node\":[{\"type\":\"start\"}]}}");

        EvalIndicator incoming = new EvalIndicator()
                .setId(665L)
                .setIdCode("ind_665")
                .setIndicatorName("干扰动作时间戳")
                .setIndicatorType("space_defense")
                .setParentId(0L)
                .setIsBottomNode(1)
                .setIsTemplate(0);

        when(mapper.selectById(665L)).thenReturn(existing);
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.updateById(any(EvalIndicator.class))).thenReturn(1);

        EvalIndicatorServiceImpl service = new EvalIndicatorServiceImpl();
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);

        service.updateIndicator(incoming);

        ArgumentCaptor<EvalIndicator> captor = ArgumentCaptor.forClass(EvalIndicator.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(existing.getCalcMethod(), captor.getValue().getCalcMethod());
    }

    @Test
    public void insertIndicatorRejectsUnknownEquipmentTypeWithoutSystemDictCache() throws Exception {
        EvalIndicatorMapper mapper = mock(EvalIndicatorMapper.class);
        when(mapper.selectCount(any())).thenReturn(0L);

        EvalIndicatorServiceImpl service = new EvalIndicatorServiceImpl();
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);

        EvalIndicator incoming = new EvalIndicator()
                .setIndicatorName("未知装备指标")
                .setIndicatorType("unknown_equipment")
                .setParentId(0L)
                .setIsBottomNode(1)
                .setIsTemplate(0);

        try {
            service.insertIndicator(incoming);
            fail("Expected unknown equipment type to be rejected by ZHPG-owned validation");
        } catch (ServiceException ex) {
            assertEquals("指标类型不在 ZHPG 装备类型允许范围内", ex.getMessage());
        }
    }
}
