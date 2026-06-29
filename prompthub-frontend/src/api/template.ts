import request from './request'
import type { TagVO } from './tag'

// ── DTO ──

export interface TemplateCreateDTO {
  title: string
  description?: string
  promptContent: string
  price?: number
  coverUrl?: string
  tagIds?: number[]
}

export interface TemplateUpdateDTO {
  title: string
  description?: string
  promptContent: string
  price?: number
  coverUrl?: string
  changeNote?: string
  tagIds?: number[]
}

export interface TemplateQueryDTO {
  page?: number
  size?: number
  tagId?: number
  keyword?: string
  minPrice?: number
  maxPrice?: number
  sortBy?: string
  status?: number
  creatorId?: number
}

// ── VO ──

export interface TemplateVO {
  id: number
  creatorId: number
  creatorName: string
  creatorAvatar: string | null
  title: string
  description: string
  coverUrl: string | null
  price: number
  status: number
  currentVersion: number
  useCount: number
  favoriteCount: number
  reviewCount: number
  avgRating: number
  tagIds: number[]
  tagNames: string[]
  createdAt: string
  updatedAt: string
}

export interface TemplateDetailVO {
  id: number
  creatorId: number
  creatorName: string
  creatorAvatar: string | null
  title: string
  description: string
  coverUrl: string | null
  price: number
  status: number
  currentVersion: number
  promptContent: string
  changeNote: string
  useCount: number
  favoriteCount: number
  reviewCount: number
  avgRating: number
  tags: TagVO[]
  hasPurchased: boolean
  createdAt: string
  updatedAt: string
}

export interface VersionVO {
  id: number
  templateId: number
  versionNumber: number
  promptContent: string
  changeNote: string
  createdAt: string
}

export interface Page<T> {
  current: number
  size: number
  total: number
  records: T[]
}

// ── API ──

/** 创建模板 */
export function createTemplate(data: TemplateCreateDTO): Promise<TemplateDetailVO> {
  return request.post('/templates', data)
}

/** 模板列表（分页+筛选+排序） */
export function getTemplates(query: TemplateQueryDTO): Promise<Page<TemplateVO>> {
  return request.get('/templates', { params: query })
}

/** 模板详情 */
export function getTemplateDetail(id: number): Promise<TemplateDetailVO> {
  return request.get(`/templates/${id}`)
}

/** 编辑模板（生成新版本） */
export function updateTemplate(id: number, data: TemplateUpdateDTO): Promise<TemplateDetailVO> {
  return request.put(`/templates/${id}`, data)
}

/** 上下架 */
export function updateTemplateStatus(id: number, status: number): Promise<void> {
  return request.put(`/templates/${id}/status`, null, { params: { status } })
}

/** 版本历史列表 */
export function getVersions(templateId: number): Promise<VersionVO[]> {
  return request.get(`/templates/${templateId}/versions`)
}

/** 版本详情 */
export function getVersionDetail(versionId: number): Promise<VersionVO> {
  return request.get(`/templates/versions/${versionId}`)
}

/** 版本对比 */
export function compareVersions(v1: number, v2: number): Promise<VersionVO[]> {
  return request.get('/templates/versions/compare', { params: { v1, v2 } })
}

/** 回滚到指定版本 */
export function rollback(templateId: number, versionId: number): Promise<TemplateDetailVO> {
  return request.post(`/templates/${templateId}/rollback/${versionId}`)
}

/** 关键字搜索 */
export function searchTemplates(keyword: string, page?: number, size?: number): Promise<Page<TemplateVO>> {
  return request.get('/templates/search', { params: { keyword, page: page || 1, size: size || 12 } })
}

/** 记录模板使用 */
export function recordUse(templateId: number, inputParams?: string): Promise<void> {
  return request.post(`/templates/${templateId}/use`, null, { params: { inputParams } })
}
