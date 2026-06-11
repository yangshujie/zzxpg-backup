package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.domain.zhpg.AlgorithmInfo;
import com.ruoyi.service.zhpg.IAlgorithmInfoService;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SubjectiveWeightServiceImplTest {

    @Test
    public void resolveSubjectiveAlgorithmPrefersAlgorithmIdWithoutWritingSubtype() throws Exception {
        IAlgorithmInfoService algorithmInfoService = mock(IAlgorithmInfoService.class);
        AlgorithmInfo algorithm = new AlgorithmInfo();
        algorithm.setId(13L);
        algorithm.setAlgorithmName("相似度法");
        when(algorithmInfoService.selectAlgorithmDetail(13L)).thenReturn(algorithm);

        SubjectiveWeightServiceImpl service = new SubjectiveWeightServiceImpl();
        Field field = SubjectiveWeightServiceImpl.class.getDeclaredField("algorithmInfoService");
        field.setAccessible(true);
        field.set(service, algorithmInfoService);

        JSONObject parent = new JSONObject();
        parent.put("weightAssignAlgorithm", 13);
        parent.put("subtype", "层次分析");

        Method method = SubjectiveWeightServiceImpl.class.getDeclaredMethod("resolveSubjectiveAlgorithm", JSONObject.class);
        method.setAccessible(true);

        assertEquals("相似度法", method.invoke(service, parent));
        assertEquals("层次分析", parent.getString("subtype"));
    }
}
