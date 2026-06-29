<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { User, Message, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
})
const loading = ref(false)

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度 3-50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度 6-100 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

async function handleRegister() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      password: form.password,
      email: form.email || undefined,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
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
          <h1 class="hero-title">加入创作者社区</h1>
          <p class="hero-desc">创建账号，开始分享你的 AI Prompt 模板，获得使用反馈与收益</p>
          <div class="features">
            <div class="feature">
              <div class="feature-icon">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                  <path d="M16.667 5L7.5 14.167 3.333 10" stroke="#4f46e5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <span>创建与管理 Prompt 模板</span>
            </div>
            <div class="feature">
              <div class="feature-icon">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                  <path d="M16.667 5L7.5 14.167 3.333 10" stroke="#4f46e5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <span>版本控制与历史追溯</span>
            </div>
            <div class="feature">
              <div class="feature-icon">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                  <path d="M16.667 5L7.5 14.167 3.333 10" stroke="#4f46e5" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </div>
              <span>获得使用反馈与创作者收入</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-side">
        <div class="form-box">
          <router-link to="/" class="back-home">← 返回首页</router-link>
          <h2 class="form-title">注册</h2>
          <p class="form-sub">创建你的账号，开启 AI Prompt 创作之旅</p>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            class="reg-form"
            @keyup.enter="handleRegister"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="用户名"
                :prefix-icon="User"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="email">
              <el-input
                v-model="form.email"
                placeholder="邮箱（选填）"
                :prefix-icon="Message"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码（6-100 个字符）"
                :prefix-icon="Lock"
                size="large"
                show-password
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="确认密码"
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
                @click="handleRegister"
              >
                注 册
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            已有账号？
            <router-link to="/login" class="link">立即登录</router-link>
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
  min-height: 640px;
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
  margin-bottom: 40px;
}

/* 功能列表 */
.features {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #475569;
}

.feature-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #eef2ff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* ==================== 右侧表单区 ==================== */
.form-side {
  width: 460px;
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
  margin-bottom: 32px;
}

/* ===== 表单 ===== */
.reg-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.reg-form :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  padding: 1px 12px;
  transition: box-shadow 0.2s;
}

.reg-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

.reg-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #4f46e5 inset;
}

.reg-form :deep(.el-input__inner) {
  font-size: 15px;
  color: #1e293b;
}

.reg-form :deep(.el-input__inner::placeholder) {
  color: #94a3b8;
}

.reg-form :deep(.el-input__prefix .el-icon) {
  color: #94a3b8;
}

.btn-submit {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  letter-spacing: 0.04em;
  margin-top: 8px;
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
