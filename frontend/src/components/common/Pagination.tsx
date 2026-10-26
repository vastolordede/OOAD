export interface PaginationProps {
  page: number
  totalPages: number
  totalElements?: number
  onPageChange: (newPage: number) => void
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
}: PaginationProps) {
  const hasPages = totalPages > 0
  const isFirstPage = page === 0
  const isLastPage = !hasPages || page >= totalPages - 1

  return (
    <nav aria-label="Product pages" className="pagination catalog-pagination">
      <button
        className="button button-secondary"
        disabled={isFirstPage || !hasPages}
        onClick={() => onPageChange(Math.max(0, page - 1))}
        type="button"
      >
        Previous
      </button>
      <span aria-live="polite" className="pagination-summary">
        {hasPages ? `Page ${page + 1} of ${totalPages}` : 'No pages'}
        {totalElements !== undefined && ` · ${totalElements} products`}
      </span>
      <button
        className="button button-secondary"
        disabled={isLastPage}
        onClick={() => onPageChange(page + 1)}
        type="button"
      >
        Next
      </button>
    </nav>
  )
}
