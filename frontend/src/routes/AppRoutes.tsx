import { Navigate, Route, Routes } from 'react-router-dom'
import { AdminLayout } from '../components/common/AdminLayout'
import { AccessDeniedPage } from '../pages/AccessDeniedPage'
import { AdminSectionPage } from '../pages/AdminSectionPage'
import { AdminVoucherFormPage } from '../pages/AdminVoucherFormPage'
import { AdminVoucherListPage } from '../pages/AdminVoucherListPage'
import { CheckoutPage } from '../pages/CheckoutPage'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { MyOrdersPage } from '../pages/MyOrdersPage'
import { OrderDetailPage } from '../pages/OrderDetailPage'
import { ProtectedRoute } from './ProtectedRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<HomePage />} path="/" />
      <Route element={<LoginPage />} path="/login" />
      <Route element={<AccessDeniedPage />} path="/access-denied" />
      <Route element={<ProtectedRoute requiredRole="CUSTOMER" />}>
        <Route element={<CheckoutPage />} path="/checkout" />
        <Route element={<MyOrdersPage />} path="/my-orders" />
        <Route element={<OrderDetailPage />} path="/my-orders/:orderId" />
      </Route>
      <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
        <Route element={<AdminLayout />} path="/admin">
          <Route element={<Navigate replace to="dashboard" />} index />
          <Route element={<AdminSectionPage />} path="dashboard" />
          <Route element={<AdminSectionPage />} path="orders" />
          <Route element={<AdminVoucherListPage />} path="vouchers" />
          <Route element={<AdminVoucherFormPage />} path="vouchers/new" />
          <Route element={<AdminVoucherFormPage />} path="vouchers/:voucherId/edit" />
          <Route element={<AdminSectionPage />} path="reviews" />
        </Route>
      </Route>
      <Route element={<HomePage />} path="*" />
    </Routes>
  )
}
