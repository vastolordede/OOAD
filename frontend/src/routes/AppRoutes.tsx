import { Navigate, Route, Routes } from 'react-router-dom'
import { AdminLayout } from '../components/common/AdminLayout'
import { MainLayout } from '../components/common/MainLayout'
import { AccessDeniedPage } from '../pages/AccessDeniedPage'
import { AddressesPage } from '../pages/AddressesPage'
import { AdminOrdersPage } from '../pages/AdminOrdersPage'
import { AdminOrderDetailPage } from '../pages/AdminOrderDetailPage'
import { AdminVoucherFormPage } from '../pages/AdminVoucherFormPage'
import { AdminVoucherListPage } from '../pages/AdminVoucherListPage'
import { AdminDashboardPage } from '../pages/AdminDashboardPage'
import { AdminUsersPage } from '../pages/AdminUsersPage'
import { AdminCategoriesPage } from '../pages/AdminCategoriesPage'
import { AdminBrandsPage } from '../pages/AdminBrandsPage'
import { AdminProductListPage } from '../pages/AdminProductListPage'
import { AdminProductFormPage } from '../pages/AdminProductFormPage'
import { AdminInventoryPage } from '../pages/AdminInventoryPage'
import { AdminReviewsPage } from '../pages/AdminReviewsPage'
import { CheckoutPage } from '../pages/CheckoutPage'
import { CartPage } from '../pages/CartPage'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage'
import { MyOrdersPage } from '../pages/MyOrdersPage'
import { MyReviewsPage } from '../pages/MyReviewsPage'
import { OrderDetailPage } from '../pages/OrderDetailPage'
import { PaymentPage } from '../pages/PaymentPage'
import { ProfilePage } from '../pages/ProfilePage'
import { ProductDetailPage } from '../pages/ProductDetailPage'
import { ProductCatalogPage } from '../pages/ProductCatalogPage'
import { RegisterPage } from '../pages/RegisterPage'
import { ResetPasswordPage } from '../pages/ResetPasswordPage'
import { WishlistPage } from '../pages/WishlistPage'
import { ProtectedRoute } from './ProtectedRoute'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<MainLayout />}>
        <Route element={<HomePage />} path="/" />
        <Route element={<LoginPage />} path="/login" />
        <Route element={<RegisterPage />} path="/register" />
        <Route element={<ForgotPasswordPage />} path="/forgot-password" />
        <Route element={<ResetPasswordPage />} path="/reset-password" />
        <Route element={<AccessDeniedPage />} path="/access-denied" />
        <Route element={<ProductCatalogPage />} path="/products" />
        <Route element={<ProductDetailPage />} path="/products/:productId" />
        <Route element={<ProtectedRoute requiredRole="CUSTOMER" />}>
          <Route element={<CartPage />} path="/cart" />
          <Route element={<CheckoutPage />} path="/checkout" />
          <Route element={<MyOrdersPage />} path="/my-orders" />
          <Route element={<OrderDetailPage />} path="/my-orders/:orderId" />
          <Route element={<PaymentPage />} path="/my-orders/:orderId/payment" />
          <Route element={<MyReviewsPage />} path="/my-reviews" />
          <Route element={<WishlistPage />} path="/wishlist" />
          <Route element={<ProfilePage />} path="/profile" />
          <Route element={<AddressesPage />} path="/addresses" />
        </Route>
      </Route>
      <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
        <Route element={<AdminLayout />} path="/admin">
          <Route element={<Navigate replace to="dashboard" />} index />
          <Route element={<AdminDashboardPage />} path="dashboard" />
          <Route element={<AdminUsersPage />} path="users" />
          <Route element={<AdminCategoriesPage />} path="categories" />
          <Route element={<AdminBrandsPage />} path="brands" />
          <Route element={<AdminProductListPage />} path="products" />
          <Route element={<AdminProductFormPage />} path="products/new" />
          <Route element={<AdminProductFormPage />} path="products/:productId/edit" />
          <Route element={<AdminInventoryPage />} path="inventory" />
          <Route element={<AdminOrdersPage />} path="orders" />
          <Route element={<AdminOrderDetailPage />} path="orders/:orderId" />
          <Route element={<AdminVoucherListPage />} path="vouchers" />
          <Route element={<AdminVoucherFormPage />} path="vouchers/new" />
          <Route element={<AdminVoucherFormPage />} path="vouchers/:voucherId/edit" />
          <Route element={<AdminReviewsPage />} path="reviews" />
        </Route>
      </Route>
      <Route element={<HomePage />} path="*" />
    </Routes>
  )
}
