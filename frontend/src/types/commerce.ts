import type { PaymentMethod, PaymentStatus } from './order'

export interface CartItemResponse {
  id: number
  variantId: number
  productId: number
  productName: string
  sku: string
  unitPrice: number
  quantity: number
  lineTotal: number
  stock: number
  available: boolean
  imageUrl: string | null
}

export interface CartResponse {
  id: number
  items: CartItemResponse[]
  totalQuantity: number
  subtotal: number
  hasUnavailableItems: boolean
}

export interface AddressResponse {
  id: number
  receiverName: string
  phone: string
  addressLine: string
  ward: string | null
  district: string | null
  city: string
  defaultAddress: boolean
  createdAt: string
  updatedAt: string
}

export interface AddressRequest {
  receiverName: string
  phone: string
  addressLine: string
  ward: string | null
  district: string | null
  city: string
  defaultAddress: boolean
}

export interface CheckoutPreviewRequest {
  addressId: number
  voucherCode: string | null
}

export interface CheckoutItemResponse {
  variantId: number
  productId: number
  productName: string
  sku: string
  unitPrice: number
  quantity: number
  lineTotal: number
  imageUrl: string | null
}

export interface CheckoutPreviewResponse {
  items: CheckoutItemResponse[]
  shippingAddress: AddressResponse
  subtotal: number
  shippingFee: number
  discountAmount: number
  totalAmount: number
  voucherCode: string | null
}

export interface CreateOrderRequest {
  addressId: number
  voucherCode: string | null
  paymentMethod: PaymentMethod
  note: string | null
}

export interface PaymentResponse {
  method: PaymentMethod
  status: PaymentStatus
  amount: number
  transactionRef: string | null
  paidAt: string | null
}

export interface OrderItemResponse {
  id: number
  variantId: number
  productName: string
  sku: string
  unitPrice: number
  quantity: number
  lineTotal: number
}

export interface OrderResponse {
  id: number
  status: OrderStatus
  receiverName: string
  receiverPhone: string
  shippingAddressLine: string
  shippingWard: string | null
  shippingDistrict: string | null
  shippingCity: string
  items: OrderItemResponse[]
  subtotal: number
  shippingFee: number
  discountAmount: number
  totalAmount: number
  voucherCode: string | null
  note: string | null
  payment: PaymentResponse | null
  createdAt: string
  deliveredAt: string | null
  cancelledAt: string | null
}

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'SHIPPING'
  | 'DELIVERED'
  | 'CANCELLED'
