import { useEffect, useState, type FormEvent } from 'react'
import { createAddressApi, deleteAddressApi, listAddressesApi, setDefaultAddressApi, updateAddressApi } from '../api/address.api'
import { EmptyState, ErrorState, LoadingState } from '../components/common/FeedbackStates'
import { useToast } from '../hooks/useToast'
import type { AddressRequest, AddressResponse } from '../types/commerce'
import { getErrorMessage } from '../utils/getErrorMessage'

const emptyAddress: AddressRequest = {
  receiverName: '',
  phone: '',
  addressLine: '',
  ward: '',
  district: '',
  city: '',
  defaultAddress: false,
}

function toAddressPayload(address: AddressRequest): AddressRequest {
  return {
    ...address,
    receiverName: address.receiverName.trim(),
    phone: address.phone.trim(),
    addressLine: address.addressLine.trim(),
    ward: address.ward?.trim() || null,
    district: address.district?.trim() || null,
    city: address.city.trim(),
  }
}

function formatAddress(address: AddressResponse): string {
  return [address.addressLine, address.ward, address.district, address.city]
    .filter((part): part is string => Boolean(part))
    .join(', ')
}

export function AddressesPage() {
  const toast = useToast()
  const [addresses, setAddresses] = useState<AddressResponse[]>([])
  const [form, setForm] = useState<AddressRequest>(emptyAddress)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [workingId, setWorkingId] = useState<number | null>(null)
  const [reloadVersion, setReloadVersion] = useState(0)

  useEffect(() => {
    let isCurrent = true
    listAddressesApi()
      .then((loadedAddresses) => {
        if (isCurrent) setAddresses(loadedAddresses)
      })
      .catch((error: unknown) => {
        if (isCurrent) setLoadError(getErrorMessage(error))
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false)
      })
    return () => {
      isCurrent = false
    }
  }, [reloadVersion])

  function handleRetry() {
    setIsLoading(true)
    setLoadError(null)
    setReloadVersion((version) => version + 1)
  }

  function resetForm() {
    setForm(emptyAddress)
    setEditingId(null)
  }

  function beginEdit(address: AddressResponse) {
    setEditingId(address.id)
    setForm({
      receiverName: address.receiverName,
      phone: address.phone,
      addressLine: address.addressLine,
      ward: address.ward,
      district: address.district,
      city: address.city,
      defaultAddress: address.defaultAddress,
    })
    document.getElementById('addressForm')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSaving(true)
    try {
      const payload = toAddressPayload(form)
      const saved = editingId === null
        ? await createAddressApi(payload)
        : await updateAddressApi(editingId, payload)
      setAddresses((current) => {
        const updated = editingId === null
          ? [saved, ...current]
          : current.map((address) => address.id === saved.id ? saved : address)
        return saved.defaultAddress
          ? updated.map((address) => ({ ...address, defaultAddress: address.id === saved.id }))
          : updated
      })
      resetForm()
      toast.success(editingId === null ? 'Đã thêm địa chỉ.' : 'Đã cập nhật địa chỉ.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  async function handleSetDefault(id: number) {
    setWorkingId(id)
    try {
      const updatedDefault = await setDefaultAddressApi(id)
      setAddresses((current) =>
        current.map((address) => ({
          ...address,
          defaultAddress: address.id === updatedDefault.id,
        })),
      )
      toast.success('Đã cập nhật địa chỉ mặc định.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setWorkingId(null)
    }
  }

  async function handleDelete(address: AddressResponse) {
    if (!window.confirm(`Bạn có chắc muốn xóa địa chỉ của ${address.receiverName}?`)) return

    setWorkingId(address.id)
    try {
      await deleteAddressApi(address.id)
      setAddresses((current) => current.filter((item) => item.id !== address.id))
      if (editingId === address.id) resetForm()
      toast.success('Đã xóa địa chỉ.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setWorkingId(null)
    }
  }

  return (
    <main className="customer-page">
      <header className="customer-page-header">
        <div>
          <p className="eyebrow">Tài khoản</p>
          <h1>Địa chỉ nhận hàng</h1>
          <p className="page-intro">Quản lý các địa chỉ dùng để giao đơn hàng của bạn.</p>
        </div>
      </header>
      {isLoading && <LoadingState message="Đang tải địa chỉ..." />}
      {loadError && <ErrorState message={loadError} onRetry={handleRetry} />}
      {!isLoading && !loadError && addresses.length === 0 && (
        <EmptyState title="Bạn chưa có địa chỉ nhận hàng" description="Thêm địa chỉ để thanh toán nhanh chóng hơn." />
      )}
      {!isLoading && !loadError && addresses.length > 0 && (
        <section aria-label="Địa chỉ đã lưu" className="address-management-list">
          {addresses.map((address) => (
            <article className="customer-panel managed-address" key={address.id}>
              <div>
                <h2>{address.receiverName} {address.defaultAddress && <span className="default-tag">Mặc định</span>}</h2>
                <p>{address.phone}</p>
                <p>{formatAddress(address)}</p>
              </div>
              <div className="address-management-actions">
                <button className="button button-secondary" onClick={() => beginEdit(address)} type="button">Chỉnh sửa</button>
                {!address.defaultAddress && (
                  <button className="button button-secondary" disabled={workingId === address.id} onClick={() => void handleSetDefault(address.id)} type="button">Đặt làm mặc định</button>
                )}
                <button className="button button-secondary button-danger" disabled={workingId === address.id} onClick={() => void handleDelete(address)} type="button">Xóa</button>
              </div>
            </article>
          ))}
        </section>
      )}
      <section className="customer-panel address-management-form" id="addressForm">
        <h2>{editingId === null ? 'Thêm địa chỉ' : 'Chỉnh sửa địa chỉ'}</h2>
        <form className="address-form-grid" onSubmit={handleSubmit}>
          <label>
            Tên người nhận
            <input autoComplete="name" onChange={(event) => setForm((current) => ({ ...current, receiverName: event.target.value }))} required value={form.receiverName} />
          </label>
          <label>
            Số điện thoại
            <input autoComplete="tel" onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))} required type="tel" value={form.phone} />
          </label>
          <label className="address-form-wide">
            Địa chỉ
            <input autoComplete="address-line1" onChange={(event) => setForm((current) => ({ ...current, addressLine: event.target.value }))} required value={form.addressLine} />
          </label>
          <label>
            Phường / xã
            <input onChange={(event) => setForm((current) => ({ ...current, ward: event.target.value }))} value={form.ward ?? ''} />
          </label>
          <label>
            Quận / huyện
            <input onChange={(event) => setForm((current) => ({ ...current, district: event.target.value }))} value={form.district ?? ''} />
          </label>
          <label>
            Tỉnh / thành phố
            <input autoComplete="address-level1" onChange={(event) => setForm((current) => ({ ...current, city: event.target.value }))} required value={form.city} />
          </label>
          <label className="checkbox-option address-form-wide">
            <input checked={form.defaultAddress} onChange={(event) => setForm((current) => ({ ...current, defaultAddress: event.target.checked }))} type="checkbox" />
            Đặt làm địa chỉ mặc định
          </label>
          <div className="address-form-actions address-form-wide">
            <button className="button button-primary" disabled={isSaving} type="submit">
              {isSaving ? 'Đang lưu...' : editingId === null ? 'Thêm địa chỉ' : 'Lưu thay đổi'}
            </button>
            {editingId !== null && (
              <button className="button button-secondary" onClick={resetForm} type="button">Hủy chỉnh sửa</button>
            )}
          </div>
        </form>
      </section>
    </main>
  )
}
