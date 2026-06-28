import request from './request'

export interface TagVO {
  id: number
  name: string
  parentId: number | null
  level: number
  sortOrder: number
  children: TagVO[]
}

/** 获取标签树 */
export function getTags(): Promise<TagVO[]> {
  return request.get('/tags')
}
