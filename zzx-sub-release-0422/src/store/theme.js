import { defineStore } from 'pinia';
import { ref, watch } from 'vue';

// Theme Configurations
export const themes = {
  'dark-theme': {
    name: '赛博朋克',
    variables: {
      '--bg-color': '#060B13',
      '--bg-color-rgb': '6, 11, 19',
      
      '--bg-color-overlay': 'rgba(6, 11, 19, 0.9)',
      '--bg-color-overlay-rgb': '6, 11, 19',
      
      '--sider-bg-color': '#0B1A30',
      '--sider-bg-color-rgb': '11, 26, 48',
      
      '--header-bg-color': '#0B1A30',
      '--header-bg-color-rgb': '11, 26, 48',
      
      '--card-bg-color': 'rgba(18, 44, 76, 0.6)',
      '--card-bg-color-rgb': '18, 44, 76',

      '--text-color-primary': '#e0eafc',
      '--text-color-primary-rgb': '224, 234, 252',
      
      '--text-color-secondary': '#8fa3b8',
      '--text-color-secondary-rgb': '143, 163, 184',
      
      '--text-color-placeholder': '#4a5d73',
      '--text-color-placeholder-rgb': '74, 93, 115',

      '--primary-color': '#00b3db',
      '--primary-color-rgb': '0, 179, 219',
      
      '--success-color': '#00ff9d',
      '--success-color-rgb': '0, 255, 157',
      
      '--warning-color': '#ffbd2e',
      '--warning-color-rgb': '255, 189, 46',
      
      '--danger-color': '#ff3c5b',
      '--danger-color-rgb': '255, 60, 91',
      
      '--info-color': '#1e90ff',
      '--info-color-rgb': '30, 144, 255',

      '--border-color': '#1c3b5a',
      '--border-color-rgb': '28, 59, 90',
      
      '--border-color-hover': '#00f2ff',
      '--border-color-hover-rgb': '0, 242, 255',

      // 非颜色变量保持不变
      '--box-shadow-base': '0 4px 12px rgba(0, 0, 0, 0.5)',
      '--box-shadow-neon': '0 0 10px rgba(0, 242, 255, 0.3)',
      '--box-shadow-neon-inset': 'inset 0 0 8px rgba(0, 242, 255, 0.1)',
      '--backdrop-filter': 'blur(10px)',
      '--itw-bg': '#081223',
      '--itw-pane-bg': '#0B1A30',
      '--itw-graph-bg': '#08101C',
      '--itw-border': 'rgba(0, 242, 255, 0.15)',
      '--itw-border-accent': 'rgba(0, 242, 255, 0.2)',
      '--itw-accent': '#00f2ff',
      '--itw-text': '#e0eafc',
      '--itw-text-secondary': '#8fa3b8',
      '--itw-text-muted': '#64748b',
      '--itw-node-label': '#e0eafc',
      '--itw-shadow': '0 16px 42px rgba(0, 0, 0, 0.4)',
      '--itw-grid-line': 'rgba(0, 242, 255, 0.05)',
      '--itw-glow1': 'rgba(0, 242, 255, 0.05)',
      '--itw-glow2': 'rgba(0, 242, 255, 0.03)',
      '--itw-tree-hover': 'rgba(0, 242, 255, 0.08)',
      '--itw-tree-active': 'rgba(0, 242, 255, 0.15)',
      '--itw-splitter': 'rgba(255, 255, 255, 0.05)',
      '--itw-splitter-hover': 'rgba(0, 242, 255, 0.2)',
      '--itw-btn-bg': 'rgba(0, 242, 255, 0.1)',
      '--itw-btn-border': 'rgba(0, 242, 255, 0.2)',
      '--itw-btn-text': 'var(--itw-text)',
      '--itw-btn-bg-hover': 'rgba(0, 242, 255, 0.15)',
      '--itw-btn-border-hover': 'rgba(0, 242, 255, 0.3)',
      '--itw-btn-secondary-bg': 'rgba(255, 255, 255, 0.03)',
      '--itw-btn-secondary-border': 'rgba(255, 255, 255, 0.1)',
      '--itw-btn-secondary-text': 'var(--itw-text)',
      '--itw-btn-secondary-bg-hover': 'rgba(255, 255, 255, 0.08)',
      '--itw-btn-secondary-border-hover': 'rgba(0, 242, 255, 0.2)',
      '--itw-danger-bg': 'rgba(255, 77, 79, 0.1)',
      '--itw-danger-border': 'rgba(255, 77, 79, 0.2)',
      '--itw-danger-text': '#ff7875',
      '--itw-danger-bg-hover': 'rgba(255, 77, 79, 0.15)',
      '--itw-danger-border-hover': 'rgba(255, 77, 79, 0.3)',
      '--itw-input-bg': 'rgba(12, 29, 56, 0.4)',
      '--itw-input-bg-focus': 'rgba(12, 29, 56, 0.6)',
      '--itw-input-border': 'rgba(0, 242, 255, 0.15)',
      '--itw-input-border-focus': 'rgba(0, 242, 255, 0.4)',
      '--itw-input-text': '#e0eafc',
      '--itw-divider-bg': '#0B1A30',
      '--itw-divider-border': 'rgba(255, 255, 255, 0.05)',
      '--itw-tag-bg': 'rgba(0, 242, 255, 0.08)',
      '--itw-tag-border': 'rgba(0, 242, 255, 0.15)',
      '--itw-tag-text': '#00f2ff',
      '--itw-tag-info-bg': 'rgba(255, 255, 255, 0.05)',
      '--itw-tag-info-border': 'rgba(255, 255, 255, 0.1)',
      '--itw-tag-info-text': '#8fa3b8',
      '--pre-chart-panel-bg': 'rgba(7, 22, 38, 0.96)',
      '--pre-chart-card-bg': 'rgba(10, 31, 52, 0.9)',
      '--pre-chart-plot-bg': '#071726',
      '--pre-chart-border': 'rgba(77, 139, 180, 0.42)',
      '--pre-chart-grid': 'rgba(101, 159, 196, 0.22)',
      '--pre-chart-axis': 'rgba(181, 214, 235, 0.74)',
      '--pre-chart-label': 'rgba(219, 239, 250, 0.9)',
      '--pre-chart-title': 'rgba(241, 250, 255, 0.96)',
      '--pre-chart-subtitle': 'rgba(196, 222, 238, 0.86)',
      '--pre-chart-highlight': 'linear-gradient(180deg, rgba(29, 70, 104, 0.2), rgba(5, 18, 31, 0) 46%)',
      '--pre-chart-before': '#f6b44b',
      '--pre-chart-after': '#22d391',
      '--pre-chart-before-area': '0.1',
      '--pre-chart-after-area': '0.14',
      '--pre-chart-node-fill': '#071726',
    }
  },
  'light-clean': {
    name: '清爽白',
    variables: {
      '--bg-color': '#ffffff',
      '--bg-color-rgb': '255, 255, 255',
      
      '--bg-color-overlay': 'rgba(255, 255, 255, 0.9)',
      '--bg-color-overlay-rgb': '255, 255, 255',
      
      '--sider-bg-color': '#f5f7fa',
      '--sider-bg-color-rgb': '245, 247, 250',
      
      '--header-bg-color': '#ffffff',
      '--header-bg-color-rgb': '255, 255, 255',
      
      '--card-bg-color': '#ffffff',
      '--card-bg-color-rgb': '255, 255, 255',

      '--text-color-primary': '#303133',
      '--text-color-primary-rgb': '48, 49, 51',
      
      '--text-color-secondary': '#606266',
      '--text-color-secondary-rgb': '96, 98, 102',
      
      '--text-color-placeholder': '#a8abb2',
      '--text-color-placeholder-rgb': '168, 171, 178',

      '--primary-color': '#007bff',
      '--primary-color-rgb': '0, 123, 255',
      
      '--success-color': '#67c23a',
      '--success-color-rgb': '103, 194, 58',
      
      '--warning-color': '#e6a23c',
      '--warning-color-rgb': '230, 162, 60',
      
      '--danger-color': '#f56c6c',
      '--danger-color-rgb': '245, 108, 108',
      
      '--info-color': '#409eff',
      '--info-color-rgb': '64, 158, 255',

      '--border-color': '#dcdfe6',
      '--border-color-rgb': '220, 223, 230',
      
      '--border-color-hover': '#007bff',
      '--border-color-hover-rgb': '0, 123, 255',

      // 非颜色变量保持不变
      '--box-shadow-base': '0 2px 12px 0 rgba(0, 0, 0, 0.1)',
      '--box-shadow-neon': 'none',
      '--box-shadow-neon-inset': 'none',
      '--backdrop-filter': 'blur(10px)',
  '--itw-bg': '#f8fafc',
  '--itw-pane-bg': '#ffffff',
  '--itw-graph-bg': '#f8fafc',
  '--itw-border': '#d8e2ee',
  '--itw-border-accent': '#cbd5e1',
  '--itw-accent': '#2563eb',
  '--itw-text': '#0f172a',
  '--itw-text-secondary': '#334155',
  '--itw-text-muted': '#64748b',
  '--itw-node-label': '#0f172a',
  '--itw-shadow': '0 14px 36px rgba(15, 23, 42, 0.12)',
  '--itw-grid-line': 'rgba(100, 116, 139, 0.12)',
  '--itw-glow1': 'rgba(37, 99, 235, 0.08)',
  '--itw-glow2': 'rgba(5, 150, 105, 0.06)',
  '--itw-tree-hover': 'rgba(37, 99, 235, 0.08)',
  '--itw-tree-active': 'rgba(37, 99, 235, 0.14)',
  '--itw-splitter': 'rgba(148, 163, 184, 0.35)',
  '--itw-splitter-hover': 'rgba(37, 99, 235, 0.35)',
  '--itw-btn-bg': 'rgba(37, 99, 235, 0.1)',
  '--itw-btn-border': 'rgba(37, 99, 235, 0.24)',
  '--itw-btn-text': '#1d4ed8',
  '--itw-btn-bg-hover': 'rgba(37, 99, 235, 0.16)',
  '--itw-btn-border-hover': 'rgba(37, 99, 235, 0.38)',
  '--itw-btn-secondary-bg': '#ffffff',
  '--itw-btn-secondary-border': '#cbd5e1',
  '--itw-btn-secondary-text': '#334155',
  '--itw-btn-secondary-bg-hover': '#f1f5f9',
  '--itw-btn-secondary-border-hover': '#94a3b8',
  '--itw-danger-bg': 'rgba(220, 38, 38, 0.08)',
  '--itw-danger-border': 'rgba(220, 38, 38, 0.24)',
  '--itw-danger-text': '#dc2626',
  '--itw-danger-bg-hover': 'rgba(220, 38, 38, 0.14)',
  '--itw-danger-border-hover': 'rgba(220, 38, 38, 0.38)',
  '--itw-input-bg': '#ffffff',
  '--itw-input-bg-focus': '#ffffff',
  '--itw-input-border': '#d8e2ee',
  '--itw-input-border-focus': 'rgba(37, 99, 235, 0.42)',
  '--itw-input-text': '#0f172a',
  '--itw-divider-bg': '#ffffff',
  '--itw-divider-border': '#d8e2ee',
  '--itw-tag-bg': 'rgba(37, 99, 235, 0.08)',
  '--itw-tag-border': 'rgba(37, 99, 235, 0.22)',
  '--itw-tag-text': '#1d4ed8',
  '--itw-tag-info-bg': '#f1f5f9',
  '--itw-tag-info-border': '#cbd5e1',
  '--itw-tag-info-text': '#475569',
      '--pre-chart-panel-bg': 'rgba(255, 255, 255, 0.98)',
      '--pre-chart-card-bg': 'rgba(255, 255, 255, 0.96)',
      '--pre-chart-plot-bg': '#f8fafc',
      '--pre-chart-border': 'rgba(148, 163, 184, 0.42)',
      '--pre-chart-grid': 'rgba(148, 163, 184, 0.22)',
      '--pre-chart-axis': 'rgba(71, 85, 105, 0.72)',
      '--pre-chart-label': 'rgba(51, 65, 85, 0.86)',
      '--pre-chart-title': '#303133',
      '--pre-chart-subtitle': '#606266',
      '--pre-chart-highlight': 'linear-gradient(180deg, rgba(255, 255, 255, 0.34), transparent 42%)',
      '--pre-chart-before': '#d97706',
      '--pre-chart-after': '#059669',
      '--pre-chart-before-area': '0.07',
      '--pre-chart-after-area': '0.09',
      '--pre-chart-node-fill': '#f8fafc',
    }
  }
};

export const useThemeStore = defineStore('theme', () => {
  const currentThemeName = ref('dark-theme');

  // Apply theme CSS variables
  const applyTheme = (themeName) => {
    const theme = themes[themeName];
    if (!theme) {
      console.warn(`Theme "${themeName}" not found`);
      return;
    }

    const root = document.documentElement;
    Object.entries(theme.variables).forEach(([key, value]) => {
      root.style.setProperty(key, value);
    });
    root.setAttribute('data-theme', themeName);
    Object.keys(themes).forEach((key) => root.classList.remove(key));
    root.classList.add(themeName)
    currentThemeName.value = themeName;
    console.log("apply theme ",currentThemeName.value)
    localStorage.setItem('app-theme', themeName);
  };

  // Set theme by name
  const setTheme = (themeName) => {
    if (themes[themeName]) {
      applyTheme(themeName);
    }
  };

  // Toggle between dark and light
  const toggleTheme = () => {
    const newTheme = currentThemeName.value === 'dark-theme' ? 'light-clean' : 'dark-theme';
    setTheme(newTheme);
  };

  // Initialize theme from localStorage or default
  const initTheme = () => {
    const savedTheme = localStorage.getItem('app-theme');
    if (savedTheme && themes[savedTheme]) {
      console.log("init theme 1", savedTheme)
      applyTheme(savedTheme);
    } else {
      console.log("init theme 2", savedTheme)
      applyTheme('dark-theme'); // Default theme
    }
  };

  // Get available themes list
  const getThemeList = () => {
    return Object.keys(themes).map(key => ({
      key,
      name: themes[key].name
    }));
  };

  return {
    currentThemeName,
    setTheme,
    toggleTheme,
    initTheme,
    getThemeList
  };
});
