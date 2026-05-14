// 订单状态映射
export const ORDER_STATUS_MAP = {
  DRAFT: { label: '草稿', type: 'info' },
  PENDING_APPROVAL: { label: '待审批', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' }
}

// 订单审核操作映射
export const ORDER_ACTION_MAP = {
  SUBMIT: { label: '提交申请', type: 'primary' },
  APPROVE: { label: '审批通过', type: 'success' },
  REJECT: { label: '驳回', type: 'danger' },
  CANCEL: { label: '取消订单', type: 'info' },
  COMPLETE: { label: '完成订单', type: 'success' }
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

// 微信绑定状态映射
export const BINDING_STATUS_MAP = {
  0: { label: '已解绑', type: 'danger' },
  1: { label: '已绑定', type: 'success' }
}

// 付款渠道类型映射
export const CHANNEL_TYPE_MAP = {
  ALIPAY: '支付宝',
  WECHAT: '微信支付',
  BANK_CARD: '银行卡',
  CASH: '现金',
  OTHER: '其他'
}

// 角色编码映射
export const ROLE_CODE_MAP = {
  ADMIN: { label: '管理员', type: 'danger' },
  SALES_MANAGER: { label: '销售经理', type: 'warning' },
  SALES_PERSON: { label: '销售人员', type: 'primary' }
}
