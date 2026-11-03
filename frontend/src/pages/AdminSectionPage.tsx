import { useLocation } from 'react-router-dom'

const sectionNames: Record<string, string> = {
  dashboard: 'Tổng quan',
  orders: 'Quản lý đơn hàng',
  vouchers: 'Quản lý mã giảm giá',
  reviews: 'Quản lý đánh giá',
}

export function AdminSectionPage() {
  const { pathname } = useLocation()
  const section = pathname.split('/').filter(Boolean).at(-1) ?? 'dashboard'
  const title = sectionNames[section] ?? 'Quản trị'

  return (
    <section className="page-section">
      <p className="eyebrow">Quản trị</p>
      <h1>{title}</h1>
      <p className="page-intro">
        Khu vực này đã sẵn sàng để kết nối với API của hệ thống.
      </p>
    </section>
  )
}
