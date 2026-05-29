import request from './request'
import { downloadBlob } from './download'

export function getPaymentSummary() {
  return request.get('/payment-dashboard/summary')
}

export function getByApprovalTime(startDate, endDate) {
  return request.get('/payment-dashboard/by-approval-time', { params: { startDate, endDate } })
}

export function exportByApprovalTime(startDate, endDate) {
  return downloadBlob('/payment-dashboard/export-by-approval-time', { startDate, endDate })
}
