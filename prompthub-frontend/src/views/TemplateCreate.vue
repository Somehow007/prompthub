<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  createTemplate,
  updateTemplate,
  getTemplateDetail,
  type TemplateCreateDTO,
  type TemplateUpdateDTO,
} from '@/api/template'
import { getTags, type TagVO } from '@/api/tag'
import TagSelector from '@/components/TagSelector.vue'

const route = useRoute()
const router = useRouter()

const isEdit = ref(false)
const editId = ref<number>(0)
const loading = ref(false)
const tags = ref<TagVO[]>([])

const form = reactive({
  title: '',
  description: '',
  promptContent: '',
  price: 0,
  coverUrl: '',
  changeNote: '',
  tagIds: [] as number[],
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 200, message: '标题长度 2-200 个字符', trigger: 'blur' },
  ],
  promptContent: [
    { required: true, message: '请输入 Prompt 内容', trigger: 'blur' },
  ],
}

async function loadTags() {
  try {
    tags.value = await getTags()
  } catch {
    // 忽略
  }
}

async function loadTemplate() {
  const id = Number(route.params.id)
  if (!id) return
  try {
    const tpl = await getTemplateDetail(id)
    editId.value = id
    isEdit.value = true
    form.title = tpl.title
    form.description = tpl.description || ''
    form.promptContent = tpl.promptContent
    form.price = tpl.price
    form.coverUrl = tpl.coverUrl || ''
    form.tagIds = tpl.tags?.map(t => t.id) || []
  } catch {
    ElMessage.error('模板加载失败')
  }
}

async function handleSubmit() {
  loading.value = true
  try {
    if (isEdit.value) {
      const dto: TemplateUpdateDTO = {
        title: form.title,
        description: form.description || undefined,
        promptContent: form.promptContent,
        price: form.price,
        coverUrl: form.coverUrl || undefined,
        changeNote: form.changeNote || undefined,
        tagIds: form.tagIds.length > 0 ? form.tagIds : undefined,
      }
      const result = await updateTemplate(editId.value, dto)
      ElMessage.success('编辑成功')
      router.push(`/template/${result.id}`)
    } else {
      const dto: TemplateCreateDTO = {
        title: form.title,
        description: form.description || undefined,
        promptContent: form.promptContent,
        price: form.price,
        coverUrl: form.coverUrl || undefined,
        tagIds: form.tagIds.length > 0 ? form.tagIds : undefined,
      }
      const result = await createTemplate(dto)
      ElMessage.success('创建成功')
      router.push(`/template/${result.id}`)
    }
  } catch {
    // 错误在拦截器中处理
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  router.back()
}

onMounted(() => {
  loadTags()
  loadTemplate()
})
</script>

<template>
  <div class="page">
    <header class="header">
      <div class="header-inner">
        <div class="header-left">
          <router-link to="/" class="logo">
            <div class="logo-mark">P</div>
            <span class="logo-text">PromptHub</span>
          </router-link>
          <span class="page-title">{{ isEdit ? '编辑模板' : '发布模板' }}</span>
        </div>
        <router-link v-if="isEdit" :to="`/template/${editId}`" class="back-link">← 返回模板详情</router-link>
        <router-link v-else to="/" class="back-link">← 返回广场</router-link>
      </div>
    </header>

    <main class="main">
      <div class="form-container">
        <!-- 基本信息 -->
        <section class="form-section">
          <h2 class="section-heading">基本信息</h2>

          <el-form :model="form" :rules="rules" label-position="top" class="tpl-form">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" placeholder="给模板起个好名字" size="large" />
            </el-form-item>

            <el-form-item label="适用场景描述" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="3"
                placeholder="描述这个模板适合什么场景使用（选填）"
              />
            </el-form-item>

            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="价格（¥）">
                  <el-input-number
                    v-model="form.price"
                    :min="0"
                    :max="9999.99"
                    :precision="2"
                    :step="1"
                    size="large"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="封面图URL">
                  <el-input v-model="form.coverUrl" placeholder="图片链接（选填）" size="large" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </section>

        <!-- 标签 -->
        <section class="form-section">
          <h2 class="section-heading">分类标签</h2>
          <TagSelector
            :tags="tags"
            :model-value="form.tagIds"
            @update:model-value="(v) => (form.tagIds = v)"
          />
        </section>

        <!-- Prompt 内容 -->
        <section class="form-section">
          <h2 class="section-heading">Prompt 内容</h2>
          <el-form :model="form" :rules="rules" label-position="top">
            <el-form-item prop="promptContent">
              <el-input
                v-model="form.promptContent"
                type="textarea"
                :rows="14"
                placeholder="在此编写你的 Prompt 模板..."
                class="prompt-editor"
              />
            </el-form-item>
          </el-form>
        </section>

        <!-- 变更说明（编辑时） -->
        <section v-if="isEdit" class="form-section">
          <h2 class="section-heading">变更说明（选填）</h2>
          <el-input
            v-model="form.changeNote"
            placeholder="描述此版本的改动内容..."
            size="large"
          />
        </section>

        <!-- 操作 -->
        <div class="form-actions">
          <el-button size="large" @click="handleCancel">取消</el-button>
          <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">
            {{ isEdit ? '保存新版本' : '发布模板' }}
          </el-button>
        </div>
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

/* 头部 */
.header {
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
}

.header-inner {
  width: 800px;
  max-width: 100%;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.back-link {
  font-size: 14px;
  color: #64748b;
  text-decoration: none;
  white-space: nowrap;
}

.back-link:hover {
  color: #4f46e5;
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

.page-title {
  font-size: 15px;
  color: #64748b;
}

/* 主体 */
.main {
  width: 800px;
  max-width: 100%;
  margin: 0 auto;
  padding: 32px 24px 60px;
}

.form-container {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px #e2e8f0;
}

.form-section {
  margin-bottom: 32px;
}

.section-heading {
  font-size: 17px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 18px;
}

/* 表单 */
.tpl-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #475569;
  padding-bottom: 4px;
}

.tpl-form :deep(.el-input__wrapper),
.prompt-editor :deep(.el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}

.tpl-form :deep(.el-input__wrapper:hover),
.prompt-editor :deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

.tpl-form :deep(.el-input__wrapper.is-focus),
.prompt-editor :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px #4f46e5 inset;
}

.prompt-editor :deep(.el-textarea__inner) {
  font-size: 14px;
  line-height: 1.7;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', monospace;
  background: #f8fafc;
}

/* 操作 */
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
}
</style>
