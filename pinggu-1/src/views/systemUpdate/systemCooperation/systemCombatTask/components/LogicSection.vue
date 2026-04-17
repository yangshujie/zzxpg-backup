<template>
  <div class="logic-section">
    <div class="flow-chart-section">
      <h4 class="section-title">数据处理流程图</h4>
      <div id="flow-chart" class="flow-chart"></div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, watch, nextTick, ref } from 'vue'
import G6 from '@antv/g6'
import { ElMessageBox, ElMessage } from 'element-plus'

const props = defineProps({
  criterionFlow: {
    type: Object,
    default: () => ({
      conditionList: [
        { conditionName: '昼夜情况', conditionCode: 'x', conditionRule: [{ value: '1', text: '白天' }, { value: '0', text: '黑夜' }] },
        { conditionName: '云层等级', conditionCode: 'y', conditionRule: [{ value: '1', text: '等级1' }, { value: '2', text: '等级2' }, { value: '3', text: '等级3' }] },
        { conditionName: '波束范围', conditionCode: 'z', conditionRule: [{ value: '1', text: '在范围内' }, { value: '0', text: '不在范围' }] }
      ],
      formula: [
        { condition: 'x==1 && z==1 && y>3', result: '0.8', isDefault: false },
        { condition: 'x==1 && z==1 && y<=3', result: '0.5', isDefault: false },
        { condition: '', result: '0', isDefault: true }
      ]
    })
  }
})

let graph = null
let formulaNodeId = 'formula-node'

// 辅助函数：计算公式结果
const evaluateFormula = (variables) => {
  const formulas = props.criterionFlow.formula || []
  for (const rule of formulas) {
    if (!rule.isDefault && rule.condition) {
      try {
        const conditionStr = rule.condition
        const func = new Function(...Object.keys(variables), `return (${conditionStr})`)
        if (func(...Object.values(variables))) {
          return rule.result
        }
      } catch (e) {
        console.error('规则求值失败:', rule.condition, e)
      }
    }
  }
  const defaultRule = formulas.find(f => f.isDefault)
  return defaultRule ? defaultRule.result : '0'
}

// 更新所有输出节点的值
const updateAllOutputs = () => {
  if (!graph) return
  // 获取所有输入节点的当前值
  const inputNodes = graph.findAllByState('node', 'input-node').filter(node => node.getModel().type === 'input-node')
  const variables = {}
  inputNodes.forEach(node => {
    const model = node.getModel()
    variables[model.variable] = parseFloat(model.currentValue)
  })
  const result = evaluateFormula(variables)
  // 更新输出节点的显示
  const outputNodes = graph.findAllByState('node', 'output-node').filter(node => node.getModel().type === 'output-node')
  outputNodes.forEach(node => {
    const model = node.getModel()
    model.outputValue = result
    graph.updateItem(node, {
      outputValue: result,
      label: `${model.label}\n${result}`
    })
  })
}

// 添加输入节点
const addInputNode = (x, y) => {
  const inputCount = graph.findAllByState('node', 'input-node').filter(n => n.getModel().type === 'input-node').length
  const nodeId = `input-${Date.now()}`
  const variable = `var${inputCount + 1}`
  const newNode = {
    id: nodeId,
    type: 'input-node',
    label: `输入 ${variable}`,
    variable: variable,
    currentValue: 0,
    rangeType: 'continuous',
    min: 0,
    max: 10,
    x: x,
    y: y,
    size: [160, 60],
    style: {
      fill: '#F6FFED',
      stroke: '#52C41A',
      lineWidth: 2,
      radius: 8,
    },
    labelCfg: {
      style: { fill: '#52C41A', fontSize: 12 }
    }
  }
  graph.addItem('node', newNode)
  // 添加边：输入节点 -> 公式节点
  graph.addItem('edge', {
    id: `edge-${nodeId}-to-formula`,
    source: nodeId,
    target: formulaNodeId,
    style: { stroke: '#52C41A', lineWidth: 2, endArrow: true }
  })
  updateAllOutputs()
}

// 添加输出节点
const addOutputNode = (x, y) => {
  const nodeId = `output-${Date.now()}`
  const newNode = {
    id: nodeId,
    type: 'output-node',
    label: '输出结果',
    outputValue: '0',
    outputType: 'number',
    x: x,
    y: y,
    size: [160, 60],
    style: {
      fill: '#FFF2E8',
      stroke: '#FA8C16',
      lineWidth: 2,
      radius: 8,
    },
    labelCfg: {
      style: { fill: '#FA8C16', fontSize: 12 }
    }
  }
  graph.addItem('node', newNode)
  // 添加边：公式节点 -> 输出节点
  graph.addItem('edge', {
    id: `edge-formula-to-${nodeId}`,
    source: formulaNodeId,
    target: nodeId,
    style: { stroke: '#FA8C16', lineWidth: 2, endArrow: true }
  })
  updateAllOutputs()
}

// 删除节点
const deleteNode = (nodeId) => {
  // 获取所有关联边
  const edges = graph.getEdges()
  edges.forEach(edge => {
    const model = edge.getModel()
    if (model.source === nodeId || model.target === nodeId) {
      graph.removeItem(edge)
    }
  })
  graph.removeItem(nodeId)
  updateAllOutputs()
}

// 编辑输入节点属性
const editInputNode = async (node) => {
  const model = node.getModel()
  const { value } = await ElMessageBox.prompt('请输入变量名', '编辑输入节点', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: model.variable,
    inputPattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/,
    inputErrorMessage: '变量名只能包含字母、数字和下划线，且不能以数字开头'
  })
  if (value) {
    model.variable = value
    model.label = `输入 ${value}`
    graph.updateItem(node, { variable: value, label: model.label })
    updateAllOutputs()
  }
  // 可以继续编辑取值范围等，简化起见仅修改变量名
}

// 编辑输出节点属性
const editOutputNode = async (node) => {
  const model = node.getModel()
  const { value } = await ElMessageBox.prompt('请输入输出标签', '编辑输出节点', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: model.label
  })
  if (value) {
    model.label = value
    graph.updateItem(node, { label: value })
  }
}

// 修改输入节点的当前值（双击节点时弹出输入框）
const changeInputValue = async (node) => {
  const model = node.getModel()
  const { value } = await ElMessageBox.prompt('请输入当前值', '设置输入值', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: model.currentValue,
    inputType: 'number'
  })
  if (value !== undefined && value !== null) {
    model.currentValue = parseFloat(value)
    graph.updateItem(node, { currentValue: model.currentValue })
    updateAllOutputs()
  }
}

// 自定义公式节点（带左右加号按钮）
const registerFormulaNode = () => {
  G6.registerNode('formula-node', {
    draw(cfg, group) {
      const width = 200
      const height = 120
      const keyShape = group.addShape('rect', {
        attrs: {
          x: -width / 2,
          y: -height / 2,
          width: width,
          height: height,
          fill: cfg.style.fill || '#E6F7FF',
          stroke: cfg.style.stroke || '#1890FF',
          lineWidth: cfg.style.lineWidth || 2,
          radius: 8,
        },
        name: 'formula-box',
        draggable: true,
      })
      // 标题文本
      group.addShape('text', {
        attrs: {
          x: 0,
          y: -height / 2 + 20,
          text: cfg.label || '公式计算',
          fill: '#1890FF',
          fontSize: 14,
          textAlign: 'center',
          fontWeight: 'bold',
        },
        name: 'title-text',
      })
      // 显示规则简要
      const formulaList = cfg.formulaList || []
      let ruleText = ''
      if (formulaList.length) {
        const mainRule = formulaList.find(f => !f.isDefault) || formulaList[0]
        ruleText = mainRule.condition ? `${mainRule.condition} → ${mainRule.result}` : `默认 → ${mainRule.result}`
      } else {
        ruleText = '无规则'
      }
      group.addShape('text', {
        attrs: {
          x: 0,
          y: -height / 2 + 50,
          text: ruleText,
          fill: '#666',
          fontSize: 10,
          textAlign: 'center',
        },
        name: 'rule-text',
      })
      // 左侧加号按钮
      const leftBtn = group.addShape('circle', {
        attrs: {
          x: -width / 2 - 15,
          y: 0,
          r: 12,
          fill: '#52C41A',
          cursor: 'pointer',
        },
        name: 'add-left-btn',
      })
      group.addShape('text', {
        attrs: {
          x: -width / 2 - 15,
          y: 0,
          text: '+',
          fill: '#fff',
          fontSize: 16,
          textAlign: 'center',
          textBaseline: 'middle',
          cursor: 'pointer',
        },
        name: 'add-left-text',
      })
      // 右侧加号按钮
      const rightBtn = group.addShape('circle', {
        attrs: {
          x: width / 2 + 15,
          y: 0,
          r: 12,
          fill: '#FA8C16',
          cursor: 'pointer',
        },
        name: 'add-right-btn',
      })
      group.addShape('text', {
        attrs: {
          x: width / 2 + 15,
          y: 0,
          text: '+',
          fill: '#fff',
          fontSize: 16,
          textAlign: 'center',
          textBaseline: 'middle',
          cursor: 'pointer',
        },
        name: 'add-right-text',
      })
      return keyShape
    },
    afterDraw(cfg, group) {
      // 可以添加交互提示
    },
    update(cfg, node) {
      const group = node.getContainer()
      // 更新规则文本
      const formulaList = cfg.formulaList || []
      let ruleText = ''
      if (formulaList.length) {
        const mainRule = formulaList.find(f => !f.isDefault) || formulaList[0]
        ruleText = mainRule.condition ? `${mainRule.condition} → ${mainRule.result}` : `默认 → ${mainRule.result}`
      } else {
        ruleText = '无规则'
      }
      const ruleShape = group.find(e => e.get('name') === 'rule-text')
      if (ruleShape) ruleShape.attr('text', ruleText)
    }
  }, 'single-node')
}

// 自定义输入节点（带删除按钮）
const registerInputNode = () => {
  G6.registerNode('input-node', {
    draw(cfg, group) {
      const width = 160
      const height = 60
      const keyShape = group.addShape('rect', {
        attrs: {
          x: -width / 2,
          y: -height / 2,
          width: width,
          height: height,
          fill: cfg.style.fill || '#F6FFED',
          stroke: cfg.style.stroke || '#52C41A',
          lineWidth: cfg.style.lineWidth || 2,
          radius: 8,
        },
        name: 'input-box',
        draggable: true,
      })
      // 变量名和值
      group.addShape('text', {
        attrs: {
          x: 0,
          y: -height / 2 + 20,
          text: `${cfg.variable || 'var'} = ${cfg.currentValue || 0}`,
          fill: '#52C41A',
          fontSize: 12,
          textAlign: 'center',
          fontWeight: 'bold',
        },
        name: 'value-text',
      })
      group.addShape('text', {
        attrs: {
          x: 0,
          y: -height / 2 + 45,
          text: cfg.label || '输入节点',
          fill: '#666',
          fontSize: 10,
          textAlign: 'center',
        },
        name: 'label-text',
      })
      // 删除按钮
      const delBtn = group.addShape('circle', {
        attrs: {
          x: width / 2 - 12,
          y: -height / 2 + 12,
          r: 10,
          fill: '#f56c6c',
          cursor: 'pointer',
        },
        name: 'delete-btn',
      })
      group.addShape('text', {
        attrs: {
          x: width / 2 - 12,
          y: -height / 2 + 12,
          text: '×',
          fill: '#fff',
          fontSize: 14,
          textAlign: 'center',
          textBaseline: 'middle',
          cursor: 'pointer',
        },
        name: 'delete-text',
      })
      return keyShape
    },
    update(cfg, node) {
      const group = node.getContainer()
      const valueText = group.find(e => e.get('name') === 'value-text')
      if (valueText) {
        valueText.attr('text', `${cfg.variable} = ${cfg.currentValue}`)
      }
    }
  }, 'single-node')
}

// 自定义输出节点（带删除按钮）
const registerOutputNode = () => {
  G6.registerNode('output-node', {
    draw(cfg, group) {
      const width = 160
      const height = 60
      const keyShape = group.addShape('rect', {
        attrs: {
          x: -width / 2,
          y: -height / 2,
          width: width,
          height: height,
          fill: cfg.style.fill || '#FFF2E8',
          stroke: cfg.style.stroke || '#FA8C16',
          lineWidth: cfg.style.lineWidth || 2,
          radius: 8,
        },
        name: 'output-box',
        draggable: true,
      })
      group.addShape('text', {
        attrs: {
          x: 0,
          y: -height / 2 + 20,
          text: `结果: ${cfg.outputValue || 0}`,
          fill: '#FA8C16',
          fontSize: 12,
          textAlign: 'center',
          fontWeight: 'bold',
        },
        name: 'result-text',
      })
      group.addShape('text', {
        attrs: {
          x: 0,
          y: -height / 2 + 45,
          text: cfg.label || '输出节点',
          fill: '#666',
          fontSize: 10,
          textAlign: 'center',
        },
        name: 'label-text',
      })
      // 删除按钮
      const delBtn = group.addShape('circle', {
        attrs: {
          x: width / 2 - 12,
          y: -height / 2 + 12,
          r: 10,
          fill: '#f56c6c',
          cursor: 'pointer',
        },
        name: 'delete-btn',
      })
      group.addShape('text', {
        attrs: {
          x: width / 2 - 12,
          y: -height / 2 + 12,
          text: '×',
          fill: '#fff',
          fontSize: 14,
          textAlign: 'center',
          textBaseline: 'middle',
          cursor: 'pointer',
        },
        name: 'delete-text',
      })
      return keyShape
    },
    update(cfg, node) {
      const group = node.getContainer()
      const resultText = group.find(e => e.get('name') === 'result-text')
      if (resultText) {
        resultText.attr('text', `结果: ${cfg.outputValue}`)
      }
    }
  }, 'single-node')
}

// 注册完成条件/终止条件组合节点（简单占位）
const registerComboNodes = () => {
  G6.registerNode('complete-combo', {
    draw(cfg, group) {
      const width = 140
      const height = 80
      const keyShape = group.addShape('rect', {
        attrs: {
          x: -width / 2,
          y: -height / 2,
          width: width,
          height: height,
          fill: '#F6FFED',
          stroke: '#52C41A',
          lineWidth: 2,
          radius: 8,
          lineDash: [5, 5],
        },
        name: 'combo-box',
      })
      group.addShape('text', {
        attrs: {
          x: 0,
          y: 0,
          text: cfg.label || '完成条件',
          fill: '#52C41A',
          fontSize: 12,
          textAlign: 'center',
          fontWeight: 'bold',
        },
        name: 'combo-text',
      })
      return keyShape
    }
  }, 'single-node')

  G6.registerNode('terminate-combo', {
    draw(cfg, group) {
      const width = 140
      const height = 80
      const keyShape = group.addShape('rect', {
        attrs: {
          x: -width / 2,
          y: -height / 2,
          width: width,
          height: height,
          fill: '#FFF2E8',
          stroke: '#FA8C16',
          lineWidth: 2,
          radius: 8,
          lineDash: [5, 5],
        },
        name: 'combo-box',
      })
      group.addShape('text', {
        attrs: {
          x: 0,
          y: 0,
          text: cfg.label || '终止条件',
          fill: '#FA8C16',
          fontSize: 12,
          textAlign: 'center',
          fontWeight: 'bold',
        },
        name: 'combo-text',
      })
      return keyShape
    }
  }, 'single-node')
}

// 初始化流程图
const initFlowChart = () => {
  const container = document.getElementById('flow-chart')
  if (!container) return
  container.innerHTML = ''
  if (graph) graph.destroy()

  // 注册自定义节点
  registerFormulaNode()
  registerInputNode()
  registerOutputNode()
  registerComboNodes()

  // 定义初始节点数据
  const inputNodes = props.criterionFlow.conditionList.map((cond, idx) => {
    let currentValue = 0
    if (cond.conditionRule && cond.conditionRule.length) {
      currentValue = parseFloat(cond.conditionRule[0].value)
    }
    return {
      id: `input-${cond.conditionCode}`,
      type: 'input-node',
      label: cond.conditionName,
      variable: cond.conditionCode,
      currentValue: currentValue,
      x: 100 + idx * 180,
      y: 150,
      style: { fill: '#F6FFED', stroke: '#52C41A' }
    }
  })

  const formulaNode = {
    id: formulaNodeId,
    type: 'formula-node',
    label: '公式计算',
    formulaList: props.criterionFlow.formula,
    x: 450,
    y: 150,
    style: { fill: '#E6F7FF', stroke: '#1890FF' }
  }

  const outputNodes = [{
    id: 'output-1',
    type: 'output-node',
    label: '计算结果',
    outputValue: '0',
    outputType: 'number',
    x: 750,
    y: 150,
    style: { fill: '#FFF2E8', stroke: '#FA8C16' }
  }]

  const completeCombo = {
    id: 'complete-combo',
    type: 'complete-combo',
    label: '完成条件群组',
    x: 600,
    y: 350,
    style: { fill: '#F6FFED', stroke: '#52C41A' }
  }

  const terminateCombo = {
    id: 'terminate-combo',
    type: 'terminate-combo',
    label: '终止条件群组',
    x: 300,
    y: 350,
    style: { fill: '#FFF2E8', stroke: '#FA8C16' }
  }

  const nodes = [...inputNodes, formulaNode, ...outputNodes, completeCombo, terminateCombo]

  const edges = []
  inputNodes.forEach(inNode => {
    edges.push({
      id: `edge-${inNode.id}-to-formula`,
      source: inNode.id,
      target: formulaNodeId,
      style: { stroke: '#52C41A', lineWidth: 2, endArrow: true }
    })
  })
  outputNodes.forEach(outNode => {
    edges.push({
      id: `edge-formula-to-${outNode.id}`,
      source: formulaNodeId,
      target: outNode.id,
      style: { stroke: '#FA8C16', lineWidth: 2, endArrow: true }
    })
  })
  // 可选连线到完成/终止条件
  edges.push({
    id: 'edge-formula-to-complete',
    source: formulaNodeId,
    target: completeCombo.id,
    style: { stroke: '#52C41A', lineWidth: 2, endArrow: true, lineDash: [5, 5] }
  })
  edges.push({
    id: 'edge-formula-to-terminate',
    source: formulaNodeId,
    target: terminateCombo.id,
    style: { stroke: '#FA8C16', lineWidth: 2, endArrow: true, lineDash: [5, 5] }
  })

  graph = new G6.Graph({
    container: 'flow-chart',
    width: container.clientWidth,
    height: container.clientHeight,
    modes: {
      default: ['drag-canvas', 'zoom-canvas', 'drag-node', 'click-select']
    },
    defaultNode: {
      size: [160, 60],
      labelCfg: { style: { fill: '#333', fontSize: 12 } }
    },
    defaultEdge: {
      style: { endArrow: true, lineWidth: 2 }
    },
    layout: {
      type: 'preset' // 使用预设位置
    },
    fitView: true,
    fitViewPadding: 20
  })

  graph.data({ nodes, edges })
  graph.render()

  // 更新输出值
  updateAllOutputs()

  // 绑定节点交互事件
  graph.on('node:click', (evt) => {
    const node = evt.item
    const model = node.getModel()
    const targetShape = evt.target
    const shapeName = targetShape.get('name')

    if (shapeName === 'delete-btn' || shapeName === 'delete-text') {
      // 删除节点
      if (model.type === 'input-node' || model.type === 'output-node') {
        deleteNode(model.id)
      }
    } else if (shapeName === 'add-left-btn' || shapeName === 'add-left-text') {
      // 左侧加号：添加输入节点，位置在公式节点左侧偏移
      const formulaModel = graph.findById(formulaNodeId).getModel()
      const newNodeX = formulaModel.x - 200
      const newNodeY = formulaModel.y + (Math.random() - 0.5) * 100
      addInputNode(newNodeX, newNodeY)
    } else if (shapeName === 'add-right-btn' || shapeName === 'add-right-text') {
      // 右侧加号：添加输出节点
      const formulaModel = graph.findById(formulaNodeId).getModel()
      const newNodeX = formulaModel.x + 200
      const newNodeY = formulaModel.y + (Math.random() - 0.5) * 100
      addOutputNode(newNodeX, newNodeY)
    } else {
      // 普通点击：根据节点类型编辑
      if (model.type === 'input-node') {
        changeInputValue(node)
      } else if (model.type === 'output-node') {
        editOutputNode(node)
      } else if (model.type === 'formula-node') {
        // 可以编辑公式规则，这里简化
        ElMessage.info('双击公式节点可编辑规则（暂未实现）')
      }
    }
  })

  // 双击节点也可以编辑
  graph.on('node:dblclick', (evt) => {
    const node = evt.item
    const model = node.getModel()
    if (model.type === 'input-node') {
      editInputNode(node)
    } else if (model.type === 'output-node') {
      editOutputNode(node)
    }
  })

  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    if (graph) {
      const container = document.getElementById('flow-chart')
      graph.changeSize(container.clientWidth, container.clientHeight)
      graph.fitView(20)
    }
  })
}

onMounted(() => {
  nextTick(() => {
    initFlowChart()
  })
})

watch(() => props.criterionFlow, () => {
  nextTick(() => {
    initFlowChart()
  })
}, { deep: true })

onUnmounted(() => {
  if (graph) {
    graph.destroy()
    graph = null
  }
})
</script>

<style lang="scss" scoped>
.logic-section {
  width: 100%;
  margin-top: 20px;
}

.flow-chart-section {
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  padding: 15px;
  background-color: var(--el-bg-color);
  min-height: 400px;
  height: 500px;
}

.section-title {
  margin: 0 0 15px 0;
  font-size: 14px;
  color: var(--el-text-color-primary);
  text-align: center;
  font-weight: 600;
}

.flow-chart {
  width: 100%;
  height: calc(100% - 30px);
  min-height: 350px;
}

:deep(.g6-graph) {
  width: 100%;
  height: 100%;
}
</style>