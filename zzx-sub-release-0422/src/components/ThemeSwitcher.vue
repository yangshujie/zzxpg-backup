<template>
  <el-dropdown trigger="click" @command="handleThemeChange" class="theme-switcher">
    <div class="theme-trigger">
      <el-icon :size="20">
        <component :is="currentThemeIcon" />
      </el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="theme in themeList"
          :key="theme.key"
          :command="theme.key"
          :disabled="currentTheme === theme.key"
        >
          <span class="theme-item">
            <el-icon class="theme-icon">
              <component :is="getThemeIcon(theme.key)" />
            </el-icon>
            {{ theme.name }}
            <el-icon v-if="currentTheme === theme.key" class="check-icon">
              <Check />
            </el-icon>
          </span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue';
import { useThemeStore } from '@/store/theme';
import { Monitor, Sunny, Check } from '@element-plus/icons-vue';

const themeStore = useThemeStore();

const currentTheme = computed(() => themeStore.currentThemeName);
const themeList = computed(() => themeStore.getThemeList());

const currentThemeIcon = computed(() => {
  return currentTheme.value === 'dark-theme' ? 'Monitor' : 'Sunny';
});

const getThemeIcon = (themeKey) => {
  return themeKey === 'dark-theme' ? 'Monitor' : 'Sunny';
};

const handleThemeChange = (themeKey) => {
  themeStore.setTheme(themeKey);
};
</script>

<style scoped lang="scss">
.theme-switcher {
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.theme-trigger {
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  
  &:hover {
    background-color: var(--border-color);
  }
  
  .el-icon {
    color: var(--text-color-primary);
  }
}

.theme-item {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .theme-icon {
    color: var(--primary-color);
  }
  
  .check-icon {
    margin-left: auto;
    color: var(--success-color);
  }
}
</style>
