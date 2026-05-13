package com.ruoyi.service.zhpg;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.domain.zhpg.EvalIndicator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 评估指标 Service 接口（指标 + 指标模板共用同一表，按 is_template 区分）
 */
public interface IEvalIndicatorService extends IService<EvalIndicator> {

    /** 分页查询；query.isTemplate 为必填语义（前端 Tab 决定） */
    Page<EvalIndicator> selectIndicatorPage(Page page, EvalIndicator query);

    /** 不分页查询 */
    List<EvalIndicator> selectIndicatorList(EvalIndicator query);

    /** 新增指标 / 模板（是否模板由 indicator.isTemplate 决定） */
    int insertIndicator(EvalIndicator indicator);

    /** 修改指标 / 模板 */
    int updateIndicator(EvalIndicator indicator);

    /** 删除（按 ids，存在子节点时拒绝） */
    int deleteIndicatorByIds(Long[] ids);

    /** 父节点候选（限定 isTemplate 范围与同 indicatorType 或「无」） */
    List<EvalIndicator> selectParentCandidates(String indicatorType, Boolean isTemplate, String keyword,
                                               Long excludeId, Integer limit);

    /** 拉取以 rootId 为根的子树（用于「从指标库导入」到指标体系） */
    IndicatorTreeNode buildIndicatorTree(Long rootId);

    /**
     * 从模板（is_template=1）实例化为指标（is_template=0），允许指定挂载父级
     * @param templateId 模板指标 ID
     * @param parentId 新指标的父级 ID（0/null 表示根）
     * @param operator 操作人
     * @return 新建后的指标记录
     */
    EvalIndicator instantiateFromTemplate(Long templateId, Long parentId, String operator);

    /**
     * 指标树节点 VO，仅用于 /tree 接口返回，不持久化
     */
    class IndicatorTreeNode {
        private Long id;
        private String idCode;
        private Long parentId;
        private String indicatorName;
        private String indicatorType;
        private String unit;
        private BigDecimal valueMin;
        private BigDecimal valueMax;
        private String valueCategory;
        private String calcMethod;
        private Long algorithmId;
        private Integer level;
        private Integer orderNum;
        private String description;
        private Integer isApplied;
        private Boolean isBottomNode;
        private final List<IndicatorTreeNode> children = new ArrayList<>();

        public static IndicatorTreeNode from(EvalIndicator ind) {
            IndicatorTreeNode n = new IndicatorTreeNode();
            n.id = ind.getId();
            n.idCode = ind.getIdCode();
            n.parentId = ind.getParentId();
            n.indicatorName = ind.getIndicatorName();
            n.indicatorType = ind.getIndicatorType();
            n.unit = ind.getUnit();
            n.valueMin = ind.getValueMin();
            n.valueMax = ind.getValueMax();
            n.valueCategory = ind.getValueCategory();
            n.calcMethod = ind.getCalcMethod();
            n.algorithmId = ind.getAlgorithmId();
            n.level = ind.getLevel();
            n.orderNum = ind.getOrderNum();
            n.description = ind.getDescription();
            n.isApplied = ind.getIsApplied();
            n.isBottomNode = ind.getIsBottomNode();
            return n;
        }

        public Long getId() { return id; }
        public String getIdCode() { return idCode; }
        public Long getParentId() { return parentId; }
        public String getIndicatorName() { return indicatorName; }
        public String getIndicatorType() { return indicatorType; }
        public String getUnit() { return unit; }
        public BigDecimal getValueMin() { return valueMin; }
        public BigDecimal getValueMax() { return valueMax; }
        public String getValueCategory() { return valueCategory; }
        public String getCalcMethod() { return calcMethod; }
        public Long getAlgorithmId() { return algorithmId; }
        public Integer getLevel() { return level; }
        public Integer getOrderNum() { return orderNum; }
        public String getDescription() { return description; }
        public Integer getIsApplied() { return isApplied; }
        public Boolean getIsBottomNode() { return isBottomNode; }
        public List<IndicatorTreeNode> getChildren() { return children; }
    }
}
