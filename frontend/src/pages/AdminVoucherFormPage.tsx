import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { createVoucherApi, getVoucherApi, updateVoucherApi } from '../api/vouchers.api'
import type { VoucherRequest, VoucherResponse } from '../types/voucher'
import { useToast } from '../hooks/useToast'
import { getErrorMessage } from '../utils/getErrorMessage'

interface VoucherForm {
  code: string
  description: string
  discountType: 'FIXED' | 'PERCENTAGE'
  discountValue: string
  minOrderValue: string
  maxDiscount: string
  usageLimit: string
  startDate: string
  endDate: string
}

const emptyForm: VoucherForm = {
  code: '',
  description: '',
  discountType: 'FIXED',
  discountValue: '',
  minOrderValue: '0',
  maxDiscount: '',
  usageLimit: '',
  startDate: '',
  endDate: '',
}

interface LoadedVoucher {
  key: string
  voucher?: VoucherResponse
  error?: string
}

function toLocalDateTime(value: string): string {
  const date = new Date(value)
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
  return localDate.toISOString().slice(0, 16)
}

function numberOrNull(value: string): number | null {
  return value.trim() ? Number(value) : null
}

export function AdminVoucherFormPage() {
  const { voucherId } = useParams()
  const id = voucherId ? Number(voucherId) : null
  const isEdit = id !== null
  const isValidId = !isEdit || (id !== null && Number.isInteger(id) && id > 0)
  const toast = useToast()
  const navigate = useNavigate()
  const [form, setForm] = useState<VoucherForm>(emptyForm)
  const [loaded, setLoaded] = useState<LoadedVoucher | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [isSaving, setIsSaving] = useState(false)
  const [reloadVersion, setReloadVersion] = useState(0)
  const requestKey = `${id}:${reloadVersion}`
  const voucher = loaded?.key === requestKey ? loaded.voucher : null
  const loadError = loaded?.key === requestKey ? loaded.error : null
  const isLoading = isEdit && loaded?.key !== requestKey

  useEffect(() => {
    if (!isEdit || id === null || !isValidId) {
      return
    }

    let isCurrent = true
    getVoucherApi(id)
      .then((loadedVoucher) => {
        if (isCurrent) {
          setLoaded({ key: requestKey, voucher: loadedVoucher })
          setForm({
            code: loadedVoucher.code,
            description: loadedVoucher.description ?? '',
            discountType: loadedVoucher.discountType,
            discountValue: String(loadedVoucher.discountValue),
            minOrderValue: String(loadedVoucher.minOrderValue),
            maxDiscount: loadedVoucher.maxDiscount === null ? '' : String(loadedVoucher.maxDiscount),
            usageLimit: loadedVoucher.usageLimit === null ? '' : String(loadedVoucher.usageLimit),
            startDate: toLocalDateTime(loadedVoucher.startDate),
            endDate: toLocalDateTime(loadedVoucher.endDate),
          })
        }
      })
      .catch((error: unknown) => {
        if (isCurrent) {
          setLoaded({ key: requestKey, error: getErrorMessage(error) })
        }
      })

    return () => {
      isCurrent = false
    }
  }, [id, isEdit, isValidId, reloadVersion, requestKey])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError(null)

    const discountValue = Number(form.discountValue)
    const minOrderValue = numberOrNull(form.minOrderValue)
    const maxDiscount =
      form.discountType === 'PERCENTAGE' ? numberOrNull(form.maxDiscount) : null
    const usageLimit = numberOrNull(form.usageLimit)
    const startDate = new Date(form.startDate)
    const endDate = new Date(form.endDate)

    if (!Number.isFinite(discountValue) || discountValue <= 0) {
      setFormError('Discount value must be greater than zero.')
      return
    }
    if (
      form.discountType === 'PERCENTAGE' &&
      (discountValue > 100 || discountValue <= 0)
    ) {
      setFormError('Percentage discount must be greater than zero and no more than 100.')
      return
    }
    if (minOrderValue !== null && (!Number.isFinite(minOrderValue) || minOrderValue < 0)) {
      setFormError('Minimum order value must be zero or greater.')
      return
    }
    if (maxDiscount !== null && (!Number.isFinite(maxDiscount) || maxDiscount <= 0)) {
      setFormError('Maximum discount must be greater than zero.')
      return
    }
    if (usageLimit !== null && (!Number.isInteger(usageLimit) || usageLimit < 1)) {
      setFormError('Usage limit must be a whole number of at least one.')
      return
    }
    if (
      !Number.isFinite(startDate.getTime()) ||
      !Number.isFinite(endDate.getTime()) ||
      startDate >= endDate
    ) {
      setFormError('End date must be later than start date.')
      return
    }

    const payload: VoucherRequest = {
      code: form.code.trim(),
      description: form.description.trim() || null,
      discountType: form.discountType,
      discountValue,
      minOrderValue,
      maxDiscount,
      usageLimit,
      startDate: startDate.toISOString(),
      endDate: endDate.toISOString(),
    }

    setIsSaving(true)
    try {
      if (id !== null) {
        await updateVoucherApi(id, payload)
      } else {
        await createVoucherApi(payload)
      }
      toast.success(`Voucher ${id === null ? 'created' : 'updated'} successfully.`)
      navigate('/admin/vouchers', { replace: true })
    } catch (error) {
      setFormError(getErrorMessage(error))
      toast.error(getErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  if (!isValidId) {
    return <section className="page-section"><p className="form-error" role="alert">Voucher ID is invalid.</p></section>
  }

  if (isLoading) {
    return <section className="page-section"><p className="loading-state" role="status">Loading voucher…</p></section>
  }

  if (loadError) {
    return (
      <section className="page-section">
        <p className="form-error" role="alert">{loadError}</p>
        <button
          className="button button-secondary"
          onClick={() => setReloadVersion((version) => version + 1)}
          type="button"
        >
          Retry
        </button>
      </section>
    )
  }

  if (isEdit && !voucher) {
    return <section className="page-section"><p className="loading-state" role="status">Loading voucher…</p></section>
  }

  return (
    <section className="page-section voucher-form-page">
      <header className="section-heading">
        <div>
          <p className="eyebrow">Admin · Vouchers</p>
          <h1>{isEdit ? 'Edit voucher' : 'Create voucher'}</h1>
        </div>
        <Link className="text-link" to="/admin/vouchers">Back to vouchers</Link>
      </header>

      {voucher && (
        <p className="voucher-usage-note">
          Usage: {voucher.usedCount} / {voucher.usageLimit ?? 'unlimited'}
        </p>
      )}

      <form className="voucher-form" onSubmit={handleSubmit}>
        <label>
          Code
          <input
            maxLength={50}
            minLength={3}
            onChange={(event) => setForm({ ...form, code: event.target.value })}
            pattern="[A-Za-z0-9_-]+"
            required
            value={form.code}
          />
        </label>
        <label className="voucher-form-wide">
          Description
          <input
            maxLength={255}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
            value={form.description}
          />
        </label>
        <label>
          Discount type
          <select
            onChange={(event) =>
              setForm({
                ...form,
                discountType: event.target.value === 'PERCENTAGE' ? 'PERCENTAGE' : 'FIXED',
              })
            }
            value={form.discountType}
          >
            <option value="FIXED">Fixed amount</option>
            <option value="PERCENTAGE">Percentage</option>
          </select>
        </label>
        <label>
          Discount value
          <input
            min="0.01"
            max={form.discountType === 'PERCENTAGE' ? 100 : undefined}
            onChange={(event) => setForm({ ...form, discountValue: event.target.value })}
            required
            step="0.01"
            type="number"
            value={form.discountValue}
          />
        </label>
        <label>
          Minimum order value
          <input
            min="0"
            onChange={(event) => setForm({ ...form, minOrderValue: event.target.value })}
            step="0.01"
            type="number"
            value={form.minOrderValue}
          />
        </label>
        {form.discountType === 'PERCENTAGE' && (
          <label>
            Maximum discount
            <input
              min="0.01"
              onChange={(event) => setForm({ ...form, maxDiscount: event.target.value })}
              step="0.01"
              type="number"
              value={form.maxDiscount}
            />
          </label>
        )}
        <label>
          Usage limit (optional)
          <input
            min="1"
            onChange={(event) => setForm({ ...form, usageLimit: event.target.value })}
            step="1"
            type="number"
            value={form.usageLimit}
          />
        </label>
        <label>
          Starts at
          <input
            onChange={(event) => setForm({ ...form, startDate: event.target.value })}
            required
            type="datetime-local"
            value={form.startDate}
          />
        </label>
        <label>
          Ends at
          <input
            onChange={(event) => setForm({ ...form, endDate: event.target.value })}
            required
            type="datetime-local"
            value={form.endDate}
          />
        </label>

        {formError && <p className="form-error voucher-form-wide" role="alert">{formError}</p>}
        <div className="voucher-form-actions voucher-form-wide">
          <button className="button button-primary" disabled={isSaving} type="submit">
            {isSaving ? 'Saving…' : isEdit ? 'Save changes' : 'Create voucher'}
          </button>
          <Link className="button button-secondary button-link" to="/admin/vouchers">Cancel</Link>
        </div>
      </form>
    </section>
  )
}
