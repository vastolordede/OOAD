import { useLocation } from 'react-router-dom'

const sectionNames: Record<string, string> = {
  dashboard: 'Dashboard',
  orders: 'Order management',
  vouchers: 'Voucher management',
  reviews: 'Review moderation',
}

export function AdminSectionPage() {
  const { pathname } = useLocation()
  const section = pathname.split('/').filter(Boolean).at(-1) ?? 'dashboard'
  const title = sectionNames[section] ?? 'Admin'

  return (
    <section className="page-section">
      <p className="eyebrow">Admin</p>
      <h1>{title}</h1>
      <p className="page-intro">
        This section is ready for its Spring Boot API-backed workflow.
      </p>
    </section>
  )
}
