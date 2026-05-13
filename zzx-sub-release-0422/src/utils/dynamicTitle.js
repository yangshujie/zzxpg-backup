import defaultSettings from '@/settings'
import useSettingsStore from '@/store/modules/settings'

/**
 * 动态修改标题
 */
export function useDynamicTitle() {
  const settingsStore = useSettingsStore()
  const title = defaultSettings.title || '系统管理平台'
  if (settingsStore.dynamicTitle) {
    document.title = (settingsStore.title ? settingsStore.title + ' - ' : '') + title
  } else {
    document.title = title
  }
}