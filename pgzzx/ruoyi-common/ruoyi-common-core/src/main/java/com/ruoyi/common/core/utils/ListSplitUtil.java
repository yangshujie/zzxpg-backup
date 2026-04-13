package com.ruoyi.common.core.utils;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.poi.ss.formula.functions.T;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListSplitUtil {
    /**
     * 拆分
     *
     * @param allList allList
     * @param limitCount limitCount
     * @return List
     */
    public static List<List> splitList(List allList, int limitCount){
        if (CollectionUtils.isEmpty(allList)) {
            return new ArrayList<>();
        }
        List<List> appSplitList = new ArrayList<>();
        long total = allList.size();
        long remain = total % limitCount;
        long times = total / limitCount;
        long realTimes = remain == 0 ? times : times + 1;
        for (long i = 0; i < realTimes; i++) {
            List<T> batchList = (List<T>) allList.stream().skip(i * limitCount).limit(limitCount).collect(Collectors.toList());
            appSplitList.add(batchList);
        }

        return appSplitList;
    }

}
