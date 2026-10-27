import type { ReactNode } from 'react'

export function LoadingState({ message = 'Loading...' }: { message?: string }) {
  return (
    <p className="feedback-state loading-state" role="status">
      {message}
    </p>
  )
}

export function ErrorState({
  message,
  onRetry,
}: {
  message: string
  onRetry?: () => void
}) {
  return (
    <div className="feedback-state feedback-error" role="alert">
      <p>{message}</p>
      {onRetry && (
        <button className="button button-secondary" onClick={onRetry} type="button">
          Retry
        </button>
      )}
    </div>
  )
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <section className="feedback-state feedback-empty">
      <h2>{title}</h2>
      {description && <p>{description}</p>}
      {action && <div className="feedback-action">{action}</div>}
    </section>
  )
}
