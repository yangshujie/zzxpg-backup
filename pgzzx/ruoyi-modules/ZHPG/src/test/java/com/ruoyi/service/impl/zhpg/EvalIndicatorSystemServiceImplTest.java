package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.domain.zhpg.dto.EvalIndicatorSystemSelectVO;
import com.ruoyi.mapper.zhpg.EvalIndicatorSystemMapper;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EvalIndicatorSystemServiceImplTest {

    @Test
    public void selectForSelectFiltersOutSystemsWithoutRequirementId() throws Exception {
        EvalIndicatorSystemMapper mapper = mock(EvalIndicatorSystemMapper.class);

        EvalIndicatorSystemSelectVO keep = new EvalIndicatorSystemSelectVO();
        keep.setIndicatorSystemId(14L);
        keep.setIndicatorSystemName("通信对抗试验评估指标体系");
        keep.setRequirementId(40L);
        keep.setTreeData("{\"treeData\":{\"label\":\"root-keep\"}}");

        EvalIndicatorSystemSelectVO drop = new EvalIndicatorSystemSelectVO();
        drop.setIndicatorSystemId(2L);
        drop.setIndicatorSystemName("111");
        drop.setRequirementId(null);
        drop.setTreeData("{\"treeData\":{\"label\":\"root-drop\"}}");

        when(mapper.selectIndicatorSystemListForSelect(null, 40L)).thenReturn(Arrays.asList(keep, drop));

        EvalIndicatorSystemServiceImpl service = new EvalIndicatorSystemServiceImpl();
        Field field = ServiceImpl.class.getDeclaredField("baseMapper");
        field.setAccessible(true);
        field.set(service, mapper);

        List<EvalIndicatorSystemSelectVO> result = service.selectIndicatorSystemListForSelect(null, 40L);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(14L), result.get(0).getIndicatorSystemId());
        assertEquals(Long.valueOf(40L), result.get(0).getRequirementId());
        assertTrue(result.get(0).getTreeData() instanceof JSONObject);
        assertEquals("root-keep", ((JSONObject) result.get(0).getTreeData()).getString("label"));
    }
}
