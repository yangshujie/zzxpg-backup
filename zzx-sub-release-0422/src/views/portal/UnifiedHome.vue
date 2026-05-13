<template>
  <div class="unified-home">
    <div class="middle-content">
      <div class="section-title">
        <el-icon>
          <Notification />
        </el-icon>
        公告与动态
      </div>
      <el-carousel :interval="5000" height="520px" class="announcement-carousel" arrow="always">
        <el-carousel-item v-for="(group, index) in announcementGroups" :key="index">
          <div class="announcement-grid">
            <div v-for="item in group" :key="item.id" class="announcement-card" @click="viewAnnouncement(item)">
              <div class="card-tag" :class="item.typeClass">{{ item.type }}</div>
              <h4 class="card-title">{{ item.title }}</h4>
              <p class="card-desc">{{ item.desc }}</p>
              <div class="card-footer">
                <span class="time">{{ item.time }}</span>
                <el-button link type="primary" size="small">详情 <el-icon>
                    <ArrowRight />
                  </el-icon></el-button>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>

      <div class="dashboard-stats">
        <div v-for="(stat, index) in stats" :key="index" class="stat-card">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Notification, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const viewAnnouncement = (item) => {
  ElMessage.info(`查看公告详情: ${item.title}`)
}

// Announcements
const announcements = ref([
  { id: 1, type: '通知公告', typeClass: 'note', title: '关于2024年Q1评估任务的启动说明', desc: '请各单位按照既定流程在3月20日前完成数据采集工作。', time: '2024-03-15' },
  { id: 2, type: '系统更新', typeClass: 'update', title: '系统 v2.5 版本正式上线', desc: '新增了 AI 骨架匹配引擎和自动化预处理算子，效率提升30%。', time: '2024-03-14' },
  { id: 3, type: '重要提醒', typeClass: 'warn', title: '数据存储空间预警 (节点-A4)', desc: '节点-A4 存储已接近 90% 阈值，请及时清理冗余原始数据。', time: '2024-03-13' },
  { id: 4, type: '评估活动', typeClass: 'activity', title: '某型航天器全效能评估演练', desc: '定于本周五下午14:00开展实时链路丢包敏感性评估演练。', time: '2024-03-12' },
  { id: 5, type: '系统更新', typeClass: 'update', title: '表单设计器组件库扩充', desc: '新增5种高阶图形化采集组件，支持复杂参数的动态绑定。', time: '2024-03-11' },
  { id: 6, type: '通知公告', typeClass: 'note', title: '年度资源库清理计划', desc: '将对超过2年的历史评估报告进行离线归档，请知悉。', time: '2024-03-10' },
  { id: 7, type: '重要提醒', typeClass: 'warn', title: 'API 接口调用频次限制', desc: '外网数据源调用已达到日上限，建议切换至备用离线库。', time: '2024-03-09' },
  { id: 8, type: '评估活动', typeClass: 'activity', title: '专家组在线会签通知', desc: '请相关专家于16日登录系统进行指标权重二次确认。', time: '2024-03-08' },
])

const announcementGroups = computed(() => {
  const groups = []
  for (let i = 0; i < announcements.value.length; i += 4) {
    groups.push(announcements.value.slice(i, i + 4))
  }
  return groups
})

const stats = [
  { label: '在研评估课题', value: '12' },
  { label: '本月采集数据量', value: '45.8 GB' },
  { label: '系统在线节点', value: '1,502' },
  { label: 'AI 指标置信度', value: '98.5%' },
]
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.unified-home {
  height: 100%;
  width: 100%;
}

.middle-content {
  padding: 20px;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
}

.section-title {
  font-size: 18px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  text-shadow: 0 0 10px rgba(0, 242, 255, 0.3);
}

.announcement-carousel {
  flex: 1;
  min-height: 500px;
}

.announcement-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: 20px;
  height: 100%;
  padding: 10px;
  box-sizing: border-box;
}

.announcement-card {
  background: rgba(16, 32, 53, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  cursor: pointer;

  &:hover {
    transform: translateY(-5px);
    background: rgba(0, 242, 255, 0.05);
    border-color: #00f2ff;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 15px rgba(0, 242, 255, 0.1);
  }

  .card-tag {
    align-self: flex-start;
    font-size: 10px;
    padding: 2px 8px;
    border-radius: 4px;
    margin-bottom: 12px;
    font-weight: bold;
    text-transform: uppercase;

    &.note {
      background: rgba(0, 242, 255, 0.2);
      color: #00f2ff;
    }

    &.update {
      background: rgba(63, 185, 80, 0.2);
      color: #3fb950;
    }

    &.warn {
      background: rgba(248, 81, 73, 0.2);
      color: #f85149;
    }

    &.activity {
      background: rgba(227, 179, 65, 0.2);
      color: #e3b341;
    }
  }

  .card-title {
    margin: 0 0 10px 0;
    font-size: 16px;
    color: #fff;
    line-height: 1.4;
  }

  .card-desc {
    margin: 0;
    font-size: 13px;
    color: #8fa3b8;
    line-height: 1.6;
    flex: 1;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .card-footer {
    padding-top: 15px;
    margin-top: auto;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    display: flex;
    justify-content: space-between;
    align-items: center;

    .time {
      font-size: 12px;
      color: #5d6d7e;
    }
  }
}

.dashboard-stats {
  display: flex;
  gap: 20px;
  margin-top: 24px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 12px;
  border: 1px dashed rgba(0, 242, 255, 0.1);

  .stat-card {
    flex: 1;
    text-align: center;

    .stat-value {
      font-size: 24px;
      font-weight: bold;
      color: #00f2ff;
      margin-bottom: 4px;
      font-family: 'Roboto Mono', monospace;
    }

    .stat-label {
      font-size: 12px;
      color: #6b8096;
    }
  }
}
</style>
