import request from './request'

// ── VO ──

export interface OrderVO {
  id: number
  orderNo: string
  userId: number
  templateId: number
  templateTitle: string
  amount: number
  status: number
  payTime: string
  createdAt: string
}

// ── API ──

/** 购买模板 */
export function purchaseTemplate(templateId: number): Promise<OrderVO> {
  return request.post('/orders', null, { params: { templateId } })
}

/** 我的订单列表 */
export function getOrders(): Promise<OrderVO[]> {
  return request.get('/orders')
}

/** 获取已购买的模板ID列表 */
export function getPurchasedIds(): Promise<number[]> {
  return request.get('/orders/purchased-ids')
}
