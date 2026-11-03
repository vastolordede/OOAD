import type { UserRole } from '../types/auth'
import type { OrderStatus } from '../types/commerce'
import type { PaymentMethod, PaymentStatus } from '../types/order'
import type { ReviewStatus } from '../types/review'
import type { CatalogStatus, DiscountType } from '../types/voucher'

export function roleLabel(role: UserRole): string {
  return role === 'ADMIN' ? 'Quản trị viên' : 'Khách hàng'
}

export function orderStatusLabel(status: OrderStatus): string {
  const labels: Record<OrderStatus, string> = {
    PENDING: 'Chờ xác nhận',
    CONFIRMED: 'Đã xác nhận',
    SHIPPING: 'Đang giao hàng',
    DELIVERED: 'Đã giao hàng',
    CANCELLED: 'Đã hủy',
  }
  return labels[status]
}

export function catalogStatusLabel(status: CatalogStatus): string {
  return status === 'ACTIVE' ? 'Đang hoạt động' : 'Ngừng hoạt động'
}

export function userStatusLabel(status: 'ACTIVE' | 'DISABLED'): string {
  return status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã khóa'
}

export function reviewStatusLabel(status: ReviewStatus): string {
  return status === 'VISIBLE' ? 'Đang hiển thị' : 'Đã ẩn'
}

export function paymentMethodLabel(method: PaymentMethod): string {
  return method === 'COD' ? 'Thanh toán khi nhận hàng' : 'Thanh toán trực tuyến (mô phỏng)'
}

export function paymentStatusLabel(status: PaymentStatus): string {
  const labels: Record<PaymentStatus, string> = {
    PENDING: 'Chờ thanh toán',
    PAID: 'Đã thanh toán',
    FAILED: 'Thanh toán thất bại',
    REFUNDED: 'Đã hoàn tiền',
  }
  return labels[status]
}

export function discountTypeLabel(type: DiscountType): string {
  return type === 'FIXED' ? 'Giảm số tiền cố định' : 'Giảm theo phần trăm'
}

export function formatDateTime(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('vi-VN')
}

export function formatDate(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('vi-VN')
}

const errorTranslations: Record<string, string> = {
  'Invalid email or password': 'Email hoặc mật khẩu không chính xác.',
  'Account is disabled': 'Tài khoản đã bị khóa.',
  'Data conflicts with an existing record': 'Dữ liệu bị trùng với thông tin đã tồn tại.',
  'Validation failed': 'Thông tin chưa hợp lệ.',
  'Constraint violation': 'Giá trị nhập vào chưa hợp lệ.',
  'An unexpected error occurred': 'Đã xảy ra lỗi không mong muốn.',
  'Authentication required': 'Vui lòng đăng nhập để tiếp tục.',
  'Email is already registered': 'Email này đã được đăng ký.',
  'Cart is empty': 'Giỏ hàng đang trống.',
  'Product is not available': 'Sản phẩm hiện không khả dụng.',
  'Product is already in your wishlist': 'Sản phẩm đã có trong danh sách yêu thích.',
  'Address not found': 'Không tìm thấy địa chỉ.',
  'Voucher not found': 'Không tìm thấy mã giảm giá.',
  'Voucher code already exists': 'Mã giảm giá đã tồn tại.',
  'Voucher is not active': 'Mã giảm giá hiện không hoạt động.',
  'Voucher is not yet valid': 'Mã giảm giá chưa đến thời gian áp dụng.',
  'Voucher has expired': 'Mã giảm giá đã hết hạn.',
  'Voucher usage limit reached': 'Mã giảm giá đã hết lượt sử dụng.',
  'You have already used this voucher': 'Bạn đã sử dụng mã giảm giá này.',
  'Product image upload is not configured': 'Chức năng tải hình ảnh chưa được cấu hình.',
  'Unable to connect to the server. Check the API URL and try again.':
    'Không thể kết nối đến máy chủ. Vui lòng kiểm tra địa chỉ API và thử lại.',
}

const fieldLabels: Record<string, string> = {
  email: 'Email',
  password: 'Mật khẩu',
  fullName: 'Họ và tên',
  phone: 'Số điện thoại',
  name: 'Tên',
  code: 'Mã',
  quantity: 'Số lượng',
  price: 'Giá',
  stock: 'Tồn kho',
  receiverName: 'Tên người nhận',
  addressLine: 'Địa chỉ',
}

export function translateErrorMessage(message: string): string {
  const exactTranslation = errorTranslations[message]
  if (exactTranslation) return exactTranslation

  const unavailableProduct = message.match(/^Product '(.+)' is no longer available$/)
  if (unavailableProduct) {
    return `Sản phẩm "${unavailableProduct[1]}" hiện không còn khả dụng.`
  }

  const resourceNotFound = message.match(/^([A-Za-z]+) not found(?:: .*)?$/)
  if (resourceNotFound) {
    const resourceNames: Record<string, string> = {
      Address: 'địa chỉ',
      Brand: 'thương hiệu',
      Category: 'danh mục',
      Order: 'đơn hàng',
      Product: 'sản phẩm',
      ProductVariant: 'phân loại sản phẩm',
      Review: 'đánh giá',
      User: 'tài khoản',
      Voucher: 'mã giảm giá',
      WishlistItem: 'sản phẩm yêu thích',
    }
    const resource = resourceNames[resourceNotFound[1]]
    if (resource) return `Không tìm thấy ${resource}.`
  }

  if (message === 'from must not be after to') {
    return 'Ngày bắt đầu không được sau ngày kết thúc.'
  }
  if (message === 'startDate must be before endDate') {
    return 'Thời điểm bắt đầu phải trước thời điểm kết thúc.'
  }
  if (message.startsWith('sortBy must be one of:')) {
    return 'Trường sắp xếp không hợp lệ.'
  }
  if (message === 'direction must be asc or desc') {
    return 'Thứ tự sắp xếp không hợp lệ.'
  }
  if (message.startsWith('Date range must not exceed')) {
    return 'Khoảng thời gian báo cáo vượt quá giới hạn cho phép.'
  }
  if (message.startsWith('limit must be between')) {
    return 'Số lượng kết quả yêu cầu vượt ngoài giới hạn cho phép.'
  }
  if (message === 'must not be blank') return 'Không được để trống.'
  if (message === 'must not be null') return 'Không được bỏ trống.'
  if (message === 'must be a well-formed email address') return 'Email không đúng định dạng.'

  const sizeError = message.match(/^size must be between (\d+) and (\d+)$/)
  if (sizeError) return `Độ dài phải từ ${sizeError[1]} đến ${sizeError[2]} ký tự.`
  const minimumError = message.match(/^must be greater than or equal to (\d+)$/)
  if (minimumError) return `Giá trị phải lớn hơn hoặc bằng ${minimumError[1]}.`
  const maximumError = message.match(/^must be less than or equal to (\d+)$/)
  if (maximumError) return `Giá trị phải nhỏ hơn hoặc bằng ${maximumError[1]}.`
  const minimumSizeError = message.match(/^must be at least (\d+) characters? long$/)
  if (minimumSizeError) return `Cần nhập ít nhất ${minimumSizeError[1]} ký tự.`

  return message
}

export function translateFieldName(field: string): string {
  return fieldLabels[field] ?? field
}
