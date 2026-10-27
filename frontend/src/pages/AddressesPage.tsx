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
      toast.success(editingId === null ? 'Address added.' : 'Address updated.')
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
      toast.success('Default address updated.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setWorkingId(null)
    }
  }

  async function handleDelete(address: AddressResponse) {
    if (!window.confirm(`Delete the address for ${address.receiverName}?`)) return

    setWorkingId(address.id)
    try {
      await deleteAddressApi(address.id)
      setAddresses((current) => current.filter((item) => item.id !== address.id))
      if (editingId === address.id) resetForm()
      toast.success('Address deleted.')
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
          <p className="eyebrow">Account</p>
          <h1>My addresses</h1>
          <p className="page-intro">Manage the addresses used for your deliveries.</p>
        </div>
      </header>
      {isLoading && <LoadingState message="Loading your addresses…" />}
      {loadError && <ErrorState message={loadError} onRetry={handleRetry} />}
      {!isLoading && !loadError && addresses.length === 0 && (
        <EmptyState title="No addresses yet" description="Add an address to make checkout easier." />
      )}
      {!isLoading && !loadError && addresses.length > 0 && (
        <section aria-label="Saved addresses" className="address-management-list">
          {addresses.map((address) => (
            <article className="customer-panel managed-address" key={address.id}>
              <div>
                <h2>{address.receiverName} {address.defaultAddress && <span className="default-tag">Default</span>}</h2>
                <p>{address.phone}</p>
                <p>{formatAddress(address)}</p>
              </div>
              <div className="address-management-actions">
                <button className="button button-secondary" onClick={() => beginEdit(address)} type="button">Edit</button>
                {!address.defaultAddress && (
                  <button className="button button-secondary" disabled={workingId === address.id} onClick={() => void handleSetDefault(address.id)} type="button">Set Default</button>
                )}
                <button className="button button-secondary button-danger" disabled={workingId === address.id} onClick={() => void handleDelete(address)} type="button">Delete</button>
              </div>
            </article>
          ))}
        </section>
      )}
      <section className="customer-panel address-management-form" id="addressForm">
        <h2>{editingId === null ? 'Add an address' : 'Edit address'}</h2>
        <form className="address-form-grid" onSubmit={handleSubmit}>
          <label>
            Receiver name
            <input autoComplete="name" onChange={(event) => setForm((current) => ({ ...current, receiverName: event.target.value }))} required value={form.receiverName} />
          </label>
          <label>
            Phone
            <input autoComplete="tel" onChange={(event) => setForm((current) => ({ ...current, phone: event.target.value }))} required type="tel" value={form.phone} />
          </label>
          <label className="address-form-wide">
            Address line
            <input autoComplete="address-line1" onChange={(event) => setForm((current) => ({ ...current, addressLine: event.target.value }))} required value={form.addressLine} />
          </label>
          <label>
            Ward
            <input onChange={(event) => setForm((current) => ({ ...current, ward: event.target.value }))} value={form.ward ?? ''} />
          </label>
          <label>
            District
            <input onChange={(event) => setForm((current) => ({ ...current, district: event.target.value }))} value={form.district ?? ''} />
          </label>
          <label>
            City
            <input autoComplete="address-level1" onChange={(event) => setForm((current) => ({ ...current, city: event.target.value }))} required value={form.city} />
          </label>
          <label className="checkbox-option address-form-wide">
            <input checked={form.defaultAddress} onChange={(event) => setForm((current) => ({ ...current, defaultAddress: event.target.checked }))} type="checkbox" />
            Set as default address
          </label>
          <div className="address-form-actions address-form-wide">
            <button className="button button-primary" disabled={isSaving} type="submit">
              {isSaving ? 'Saving…' : editingId === null ? 'Add address' : 'Save changes'}
            </button>
            {editingId !== null && (
              <button className="button button-secondary" onClick={resetForm} type="button">Cancel edit</button>
            )}
          </div>
        </form>
      </section>
    </main>
  )
}
