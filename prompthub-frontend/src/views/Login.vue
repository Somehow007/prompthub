<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const form = reactive({
  username: '',
  password: '',
})
const loading = ref(false)
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur', min: 6 }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form)
    router.push('/')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="container">
      <!-- 左侧插图区 -->
      <div class="hero">
        <div class="hero-inner">
          <div class="logo">
            <div class="logo-mark">P</div>
            <span class="logo-text">PromptHub</span>
          </div>
          <h1 class="hero-title">智享 AI 提示词平台</h1>
          <p class="hero-desc">发现高质量 Prompt 模板，连接 AI 创造力与开发者智慧</p>
          <div class="hero-illustration">
            <div class="illu-card illu-card--1">
              <div class="illu-tag">写作</div>
              <div class="illu-line illu-line--short"></div>
              <div class="illu-line illu-line--long"></div>
              <div class="illu-line illu-line--medium"></div>
            </div>
            <div class="illu-card illu-card--2">
              <div class="illu-tag illu-tag--blue">编程</div>
              <div class="illu-line illu-line--long"></div>
              <div class="illu-line illu-line--short"></div>
            </div>
            <div class="illu-card illu-card--3">
              <div class="illu-tag illu-tag--purple">翻译</div>
              <div class="illu-line illu-line--medium"></div>
              <div class="illu-line illu-line--long"></div>
              <div class="illu-line illu-line--short"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-side">
        <div class="form-box">
          <router-link to="/" class="back-home">← 返回首页</router-link>
          <h2 class="form-title">登录</h2>
          <p class="form-sub">欢迎回来，请登录你的账号</p>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                class="btn-submit"
                @click="handleLogin"
              >
                登 录
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            还没有账号？
            <router-link to="/register" class="link">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ==================== 布局 ==================== */
.page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', sans-serif;
}

.container {
  width: 960px;
  min-height: 600px;
  display: flex;
  background: #fff;
  border-radius: 16px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.04),
    0 8px 40px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* ==================== 左侧品牌区 ==================== */
.hero {
  flex: 1;
  background: linear-gradient(160deg, #f0f4ff 0%, #f8f9ff 40%, #eef0fb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 48px;
}

.hero-inner {
  max-width: 360px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 32px;
}

.logo-mark {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #4f46e5;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  letter-spacing: -0.01em;
}

.hero-title {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
  margin-bottom: 12px;
  letter-spacing: -0.02em;
}

.hero-desc {
  font-size: 15px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 48px;
}

/* 抽象插图：卡片列表 */
.hero-illustration {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.illu-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.illu-card--1 {
  transform: translateX(0);
}
.illu-card--2 {
  transform: translateX(16px);
}
.illu-card--3 {
  transform: translateX(32px);
}

.illu-tag {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 4px;
  background: #fef3c7;
  color: #d97706;
  width: fit-content;
}

.illu-tag--blue {
  background: #dbeafe;
  color: #2563eb;
}

.illu-tag--purple {
  background: #ede9fe;
  color: #7c3aed;
}

.illu-line {
  height: 6px;
  border-radius: 3px;
  background: #e2e8f0;
}

.illu-line--short {
  width: 60%;
}
.illu-line--medium {
  width: 80%;
}
.illu-line--long {
  width: 100%;
}

/* ==================== 右侧表单区 ==================== */
.form-side {
  width: 440px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 48px;
}

.form-box {
  width: 100%;
}

.back-home {
  display: inline-block;
  font-size: 13px;
  color: #94a3b8;
  text-decoration: none;
  margin-bottom: 20px;
  transition: color 0.15s;
}

.back-home:hover {
  color: #4f46e5;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 8px;
  letter-spacing: -0.02em;
}

.form-sub {
  font-size: 14px;
  color: #94a3b8;
  margin-bottom: 36px;
}

/* ===== 表单 ===== */
.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  padding: 1px 12px;
  transition: box-shadow 0.2s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #4f46e5 inset;
}

.login-form :deep(.el-input__inner) {
  font-size: 15px;
  color: #1e293b;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.login-form :deep(.el-input__prefix .el-icon) {
  color: #94a3b8;
}

.btn-submit {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  letter-spacing: 0.04em;
  margin-top: 4px;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: #94a3b8;
  margin-top: 4px;
}

.link {
  color: #4f46e5;
  font-weight: 500;
}

.link:hover {
  color: #4338ca;
}

/* ==================== 响应式 ==================== */
@media (max-width: 760px) {
  .container {
    width: 100%;
    border-radius: 0;
    min-height: 100vh;
  }

  .hero {
    display: none;
  }

  .form-side {
    width: 100%;
    padding: 40px 28px;
  }
}
</style>
