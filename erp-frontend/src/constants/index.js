// 订单状态映射
export const ORDER_STATUS_MAP = {
  SAVED: { label: '已保存', type: 'info' },
  PENDING_APPROVAL: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已审批', type: 'success' },
  SHIPPED: { label: '已发货', type: 'primary' },
  DELIVERED: { label: '已妥投', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  CANCELLED: { label: '已取消', type: 'info' },
  REFUNDED: { label: '已退款', type: 'warning' }
}

// 订单标签映射
export const ORDER_TAG_MAP = {
  '': { label: '无', type: 'info' },
  DELAYED: { label: '延迟发货', type: 'danger' },
  WASH_CARE: { label: '洗护订单', type: 'primary' }
}

// 订单审核操作映射
export const ORDER_ACTION_MAP = {
  SUBMIT: { label: '提交申请', type: 'primary' },
  APPROVE: { label: '审批通过', type: 'success' },
  REJECT: { label: '驳回', type: 'danger' },
  SHIP: { label: '发货', type: 'primary' },
  DELIVER: { label: '确认妥投', type: 'success' },
  REFUND: { label: '退款', type: 'warning' },
  CANCEL: { label: '取消订单', type: 'info' }
}

// 客户等级映射
export const CUSTOMER_LEVEL_MAP = {
  0: { label: '普通', type: 'info' },
  1: { label: '银卡', type: '' },
  2: { label: '金卡', type: 'warning' },
  3: { label: '钻石', type: 'danger' }
}

// 启用/禁用状态映射
export const ENABLE_STATUS_MAP = {
  0: { label: '禁用', type: 'danger' },
  1: { label: '启用', type: 'success' }
}

// 付款渠道类型映射
export const CHANNEL_TYPE_MAP = {
  ALIPAY: '支付宝',
  WECHAT: '微信支付',
  BANK_CARD: '银行卡',
  CASH: '现金',
  OTHER: '其他'
}

// 产品类型映射
export const PRODUCT_TYPE_MAP = {
  SINGLE: { label: '单品', type: '' },
  SET: { label: '套装', type: 'success' }
}

// 角色编码映射
export const ROLE_CODE_MAP = {
  ADMIN: { label: '管理员', type: 'danger' },
  SALES_PERSON: { label: '销售人员', type: 'primary' }
}
