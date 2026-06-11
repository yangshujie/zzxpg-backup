package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.EvalIndicatorSystemSelectVO;
import com.ruoyi.mapper.zhpg.EvalIndicatorSystemMapper;
import com.ruoyi.service.zhpg.IEvalIndicatorService;
import com.ruoyi.zhpg.util.ZhpgIndicatorLibrarySyncHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    public void receiveRequirementPayloadCreatesCurrentTreeWithoutWeightSnapshot() throws Exception {
        EvalIndicatorSystemMapper mapper = mock(EvalIndicatorSystemMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            EvalIndicatorSystem system = invocation.getArgument(0);
            system.setId(100L);
            return 1;
        }).when(mapper).insert(any(EvalIndicatorSystem.class));

        EvalIndicatorSystemServiceImpl service = newService(mapper);

        JSONObject payload = payload(43L, "通信对抗试验评估指标体系");
        service.receiveRefinedFromRequirementPayload(payload, "admin");

        ArgumentCaptor<EvalIndicatorSystem> updated = ArgumentCaptor.forClass(EvalIndicatorSystem.class);
        org.mockito.Mockito.verify(mapper).updateById(updated.capture());
        EvalIndicatorSystem saved = updated.getValue();
        assertTrue(saved.getIndicatorTree().contains("通信对抗试验评估指标体系"));
        assertNull(saved.getIndicatorTreeWeight());
    }

    @Test
    public void receiveRequirementPayloadUpdatesCurrentTreeAndClearsStaleWeightSnapshot() throws Exception {
        EvalIndicatorSystem existing = new EvalIndicatorSystem();
        existing.setId(100L);
        existing.setSystemName("通信对抗试验评估指标体系（需求43）");
        existing.setRequirementId(43L);
        existing.setIndicatorTree("{\"treeData\":{\"label\":\"旧结构\"}}");
        existing.setIndicatorTreeWeight("{\"treeData\":{\"label\":\"旧权重快照\"}}");

        EvalIndicatorSystemMapper mapper = mock(EvalIndicatorSystemMapper.class);
        when(mapper.selectOne(any())).thenReturn(existing);
        when(mapper.selectById(eq(100L))).thenReturn(existing);

        EvalIndicatorSystemServiceImpl service = newService(mapper);

        JSONObject payload = payload(43L, "新细化结构");
        service.receiveRefinedFromRequirementPayload(payload, "admin");

        ArgumentCaptor<EvalIndicatorSystem> updated = ArgumentCaptor.forClass(EvalIndicatorSystem.class);
        org.mockito.Mockito.verify(mapper).updateById(updated.capture());
        EvalIndicatorSystem saved = updated.getValue();
        assertTrue(saved.getIndicatorTree().contains("新细化结构"));
        assertNull(saved.getIndicatorTreeWeight());
    }

    @Test
    public void updateSystemClearsWeightSnapshotWhenCurrentTreeIsSavedWithoutSnapshot() throws Exception {
        EvalIndicatorSystem existing = new EvalIndicatorSystem();
        existing.setId(100L);
        existing.setSystemName("通信对抗试验评估指标体系（需求43）");
        existing.setRequirementId(43L);
        existing.setIndicatorTree("{\"treeData\":{\"label\":\"旧结构\"}}");
        existing.setIndicatorTreeWeight("{\"treeData\":{\"label\":\"旧权重快照\"}}");

        EvalIndicatorSystemMapper mapper = mock(EvalIndicatorSystemMapper.class);
        when(mapper.selectById(eq(100L))).thenReturn(existing);

        EvalIndicatorSystemServiceImpl service = newService(mapper);

        EvalIndicatorSystem request = new EvalIndicatorSystem();
        request.setId(100L);
        request.setSystemName(existing.getSystemName());
        request.setRequirementId(43L);
        request.setIndicatorTree("{\"treeData\":{\"label\":\"编辑后的当前结构\"}}");

        service.updateSystem(request);

        ArgumentCaptor<EvalIndicatorSystem> updated = ArgumentCaptor.forClass(EvalIndicatorSystem.class);
        verify(mapper).updateById(updated.capture());
        assertTrue(updated.getValue().getIndicatorTree().contains("编辑后的当前结构"));
        verify(mapper).update(eq(null), any());
    }

    private static EvalIndicatorSystemServiceImpl newService(EvalIndicatorSystemMapper mapper) throws Exception {
        EvalIndicatorSystemServiceImpl service = new EvalIndicatorSystemServiceImpl();
        setField(ServiceImpl.class, service, "baseMapper", mapper);
        setField(EvalIndicatorSystemServiceImpl.class, service, "indicatorService", mock(IEvalIndicatorService.class));
        ZhpgIndicatorLibrarySyncHelper helper = mock(ZhpgIndicatorLibrarySyncHelper.class);
        when(helper.syncTreeToLibrary(any(), any(), any(), any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        setField(EvalIndicatorSystemServiceImpl.class, service, "librarySyncHelper", helper);
        setField(EvalIndicatorSystemServiceImpl.class, service, "evaluationResultLineageClient", mock(EvaluationResultLineageClient.class));
        return service;
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static JSONObject payload(Long requirementId, String rootLabel) {
        JSONObject root = new JSONObject();
        root.put("id", "root_" + requirementId);
        root.put("label", rootLabel);
        root.put("workMode", "主分协同");
        JSONObject payload = new JSONObject();
        payload.put("requirementId", requirementId);
        payload.put("treeData", root);
        return payload;
    }
}
