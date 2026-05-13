package com.ruoyi.zhpg.util;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.domain.zhpg.EvalIndicator;
import com.ruoyi.service.zhpg.IEvalIndicatorService;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZhpgIndicatorLibrarySyncHelperTest {

    @Test
    public void syncTreeToLibraryFailsFastWhenPersistedCalcMethodIsTruncated() {
        String fullCalcMethod = "{\"data\":\"leaf\",\"method\":{\"node\":[{\"type\":\"start\",\"taskStage\":\"干扰执行阶段\"},{\"type\":\"result\"}],\"lineList\":[]}}";
        String tree = "{\"treeData\":{\"id\":\"node_tmp\",\"label\":\"时间占空比\",\"indicatorType\":\"space_defense\",\"computeRule\":"
                + fullCalcMethod + ",\"children\":[]}}";

        IEvalIndicatorService service = mock(IEvalIndicatorService.class);
        doAnswer(invocation -> {
            EvalIndicator indicator = invocation.getArgument(0);
            indicator.setId(100L);
            indicator.setIdCode("ind_100");
            return 1;
        }).when(service).insertIndicator(any(EvalIndicator.class));

        EvalIndicator truncated = new EvalIndicator()
                .setId(100L)
                .setCalcMethod(fullCalcMethod.substring(0, fullCalcMethod.indexOf("taskStage") + 20));
        when(service.getById(100L)).thenReturn(truncated);

        try {
            new ZhpgIndicatorLibrarySyncHelper()
                    .syncTreeToLibrary(tree, 0, 43L, "通信对抗试验评估指标体系", service, "admin");
            fail("Expected calc_method truncation to fail fast");
        } catch (ServiceException ex) {
            assertTrue(ex.getMessage().contains("calc_method"));
            assertTrue(ex.getMessage().contains("TEXT"));
        }
    }
}
