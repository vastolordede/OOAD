import { useCallback, useMemo, useRef, useState, useEffect, type ReactNode } from 'react'
import { ToastContext, type ToastType } from '../../contexts/toast-context'

interface ToastMessage {
  id: number
  type: ToastType
  message: string
}

function ToastItem({
  toast,
  onDismiss,
}: {
  toast: ToastMessage
  onDismiss: (id: number) => void
}) {
  useEffect(() => {
    const timeout = window.setTimeout(() => onDismiss(toast.id), 5000)
    return () => window.clearTimeout(timeout)
  }, [onDismiss, toast.id])

  return (
    <div
      className={`toast toast-${toast.type}`}
      role={toast.type === 'error' ? 'alert' : 'status'}
    >
      <span>{toast.message}</span>
      <button
        aria-label="Dismiss notification"
        className="toast-dismiss"
        onClick={() => onDismiss(toast.id)}
        type="button"
      >
        ×
      </button>
    </div>
  )
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([])
  const nextId = useRef(0)

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const addToast = useCallback((type: ToastType, message: string) => {
    const id = ++nextId.current
    setToasts((current) => [...current, { id, type, message }])
  }, [])

  const value = useMemo(
    () => ({
      success: (message: string) => addToast('success', message),
      error: (message: string) => addToast('error', message),
      info: (message: string) => addToast('info', message),
    }),
    [addToast],
  )

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div aria-label="Notifications" className="toast-viewport">
        {toasts.map((toast) => (
          <ToastItem key={toast.id} onDismiss={dismiss} toast={toast} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}
