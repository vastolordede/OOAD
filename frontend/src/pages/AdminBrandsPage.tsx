import {
  createAdminBrandApi,
  listAdminBrandsApi,
  updateAdminBrandApi,
  updateAdminBrandStatusApi,
} from '../api/admin-catalog.api'
import {
  AdminCatalogManagementPage,
  type AdminCatalogConfig,
} from '../components/common/AdminCatalogManagementPage'

const config: AdminCatalogConfig = {
  entityName: 'Brand',
  list: (page, size) => listAdminBrandsApi({ page, size }),
  create: (name) => createAdminBrandApi({ name }),
  update: (id, name) => updateAdminBrandApi(id, { name }),
  setStatus: updateAdminBrandStatusApi,
}

export function AdminBrandsPage() {
  return <AdminCatalogManagementPage config={config} />
}
