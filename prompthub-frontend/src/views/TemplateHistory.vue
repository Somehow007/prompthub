<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getVersions,
  getVersionDetail,
  compareVersions,
  rollback,
  getTemplateDetail,
  type VersionVO,
  type TemplateDetailVO,
} from '@/api/template'

const route = useRoute()
const router = useRouter()

const templateId = computed(() => Number(route.params.id))

const template = ref<TemplateDetailVO | null>(null)
const versions = ref<VersionVO[]>([])
const loading = ref(true)

// 版本对比
const compareA = ref<number>(0)
const compareB = ref<number>(0)
const diffView = ref<{ v1: VersionVO; v2: VersionVO } | null>(null)

async function loadData() {
  loading.value = true
  try {
    const [tpl, vers] = await Promise.all([
      getTemplateDetail(templateId.value),
      getVersions(templateId.value),
    ])
    template.value = tpl
    versions.value = vers
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function handleCompare() {
  if (!compareA.value || !compareB.value) {
    ElMessage.warning('请选择两个版本进行对比')
    return
  }
  try {
    const [v1, v2] = await compareVersions(compareA.value, compareB.value)
    diffView.value = { v1, v2 }
  } catch {
    // 忽略
  }
}

function closeDiff() {
  diffView.value = null
  compareA.value = 0
  compareB.value = 0
}

async function handleRollback(version: VersionVO) {
  try {
    await ElMessageBox.confirm(
      `确定要回滚到 v${version.versionNumber} 吗？这会生成一个新版本。`,
      '确认回滚',
      { type: 'warning' }
    )
    await rollback(templateId.value, version.id)
    ElMessage.success('回滚成功')
    await loadData()
  } catch {
    // 取消
  }
}

function isCurrentVersion(v: VersionVO): boolean {
  return template.value?.currentVersion === v.versionNumber
}

function formatDate(d: string): string {
  return new Date(d).toLocaleString('zh-CN')
}

onMounted(loadData)
</script>

<template>
  <div class="page">
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="logo">
          <div class="logo-mark">P</div>
          <span class="logo-text">PromptHub</span>
        </router-link>
        <router-link :to="`/template/${templateId}`" class="back-link">
          ← 返回模板详情
        </router-link>
      </div>
    </header>

    <main class="main">
      <h1 class="page-title">版本历史 · {{ template?.title }}</h1>
      <p class="page-sub">当前版本：v{{ template?.currentVersion }}</p>

      <!-- 版本对比工具 -->
      <div class="compare-tool">
        <div class="compare-selects">
          <el-select v-model="compareA" placeholder="选择版本 A" clearable size="default">
            <el-option
              v-for="v in versions"
              :key="v.id"
              :label="`v${v.versionNumber} - ${v.changeNote || '无说明'}`"
              :value="v.id"
            />
          </el-select>
          <span class="vs">VS</span>
          <el-select v-model="compareB" placeholder="选择版本 B" clearable size="default">
            <el-option
              v-for="v in versions"
              :key="v.id"
              :label="`v${v.versionNumber} - ${v.changeNote || '无说明'}`"
              :value="v.id"
            />
          </el-select>
          <el-button type="primary" @click="handleCompare">对比</el-button>
        </div>

        <!-- 对比结果 -->
        <div v-if="diffView" class="diff-result">
          <div class="diff-header">
            <span>v{{ diffView.v1.versionNumber }} vs v{{ diffView.v2.versionNumber }}</span>
            <button class="close-btn" @click="closeDiff">✕</button>
          </div>
          <div class="diff-cols">
            <div class="diff-col">
              <div class="diff-col-title">v{{ diffView.v1.versionNumber }}</div>
              <pre class="diff-content">{{ diffView.v1.promptContent }}</pre>
            </div>
            <div class="diff-col">
              <div class="diff-col-title">v{{ diffView.v2.versionNumber }}</div>
              <pre class="diff-content">{{ diffView.v2.promptContent }}</pre>
            </div>
          </div>
        </div>
      </div>

      <!-- 版本列表 -->
      <div v-if="versions.length > 0" class="version-list">
        <div
          v-for="v in versions"
          :key="v.id"
          :class="['version-item', { current: isCurrentVersion(v) }]"
        >
          <div class="version-header">
            <div class="version-info">
              <span class="version-number">v{{ v.versionNumber }}</span>
              <span v-if="isCurrentVersion(v)" class="current-tag">当前</span>
              <span class="version-date">{{ formatDate(v.createdAt) }}</span>
            </div>
            <div class="version-actions">
              <el-button
                v-if="!isCurrentVersion(v)"
                type="danger"
                plain
                size="small"
                @click="handleRollback(v)"
              >
                回滚到此版本
              </el-button>
            </div>
          </div>
          <div v-if="v.changeNote" class="version-note">📝 {{ v.changeNote }}</div>
          <div class="version-preview">
            <pre>{{ v.promptContent }}</pre>
          </div>
        </div>
      </div>

      <div v-else-if="!loading" class="empty">
        <p>暂无版本记录</p>
      </div>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', sans-serif;
}

.header {
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
}

.header-inner {
  width: 900px;
  max-width: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.logo-mark {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  background: #4f46e5;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.back-link {
  font-size: 14px;
  color: #64748b;
  text-decoration: none;
}

.back-link:hover {
  color: #4f46e5;
}

.main {
  width: 900px;
  max-width: 100%;
  margin: 0 auto;
  padding: 32px 24px 60px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.page-sub {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 32px;
}

/* 对比工具 */
.compare-tool {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
  margin-bottom: 24px;
}

.compare-selects {
  display: flex;
  align-items: center;
  gap: 12px;
}

.vs {
  font-size: 14px;
  font-weight: 700;
  color: #94a3b8;
}

/* 对比结果 */
.diff-result {
  margin-top: 20px;
  border-top: 1px solid #e2e8f0;
  padding-top: 20px;
}

.diff-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 14px;
}

.close-btn {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  border: none;
  background: #f1f5f9;
  color: #64748b;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.diff-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.diff-col-title {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
  margin-bottom: 8px;
}

.diff-content {
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  padding: 14px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  max-height: 400px;
  overflow-y: auto;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

/* 版本列表 */
.version-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.version-item {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
}

.version-item.current {
  border-color: #4f46e5;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 2px #eef2ff;
}

.version-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.version-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.version-number {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.current-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 500;
}

.version-date {
  font-size: 12px;
  color: #94a3b8;
}

.version-note {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 10px;
}

.version-preview {
  max-height: 160px;
  overflow-y: auto;
}

.version-preview pre {
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  color: #475569;
  background: #f8fafc;
  border-radius: 8px;
  padding: 14px;
  border: 1px solid #e2e8f0;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.empty {
  text-align: center;
  color: #94a3b8;
  padding-top: 60px;
}
</style>
