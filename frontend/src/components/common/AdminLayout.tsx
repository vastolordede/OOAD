import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'

const navigationItems = [
  { label: 'Tổng quan', to: '/admin/dashboard' },
  { label: 'Quản lý tài khoản', to: '/admin/users' },
  { label: 'Quản lý danh mục', to: '/admin/categories' },
  { label: 'Quản lý thương hiệu', to: '/admin/brands' },
  { label: 'Quản lý sản phẩm', to: '/admin/products' },
  { label: 'Quản lý kho hàng', to: '/admin/inventory' },
  { label: 'Quản lý đơn hàng', to: '/admin/orders' },
  { label: 'Quản lý mã giảm giá', to: '/admin/vouchers' },
  { label: 'Quản lý đánh giá', to: '/admin/reviews' },
]

export function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <NavLink className="admin-brand" to="/admin/dashboard">
          Mỹ Phẩm Tơ Ràn
          <span>Hệ thống quản trị</span>
        </NavLink>
        <nav aria-label="Điều hướng quản trị" className="admin-nav">
          {navigationItems.map((item) => (
            <NavLink
              className={({ isActive }) =>
                isActive ? 'admin-nav-link is-active' : 'admin-nav-link'
              }
              key={item.to}
              to={item.to}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="admin-sidebar-footer">
          <span className="admin-user-name">{user?.fullName}</span>
          <button className="button button-secondary button-full" onClick={handleLogout} type="button">
            Đăng xuất
          </button>
        </div>
      </aside>
      <main className="admin-main">
        <header className="admin-topbar">
          <span>Quản trị hệ thống</span>
          <span className="admin-role">{user?.role === 'ADMIN' ? 'Quản trị viên' : 'Khách hàng'}</span>
        </header>
        <div className="admin-content">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
