import {
  createAdminCategoryApi,
  listAdminCategoriesApi,
  updateAdminCategoryApi,
  updateAdminCategoryStatusApi,
} from '../api/admin-catalog.api'
import {
  AdminCatalogManagementPage,
  type AdminCatalogConfig,
} from '../components/common/AdminCatalogManagementPage'

const config: AdminCatalogConfig = {
  entityName: 'Category',
  list: (page, size) => listAdminCategoriesApi({ page, size }),
  create: (name) => createAdminCategoryApi({ name }),
  update: (id, name) => updateAdminCategoryApi(id, { name }),
  setStatus: updateAdminCategoryStatusApi,
}

export function AdminCategoriesPage() {
  return <AdminCatalogManagementPage config={config} />
}
