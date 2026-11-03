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
    <nav aria-label="Các trang" className="pagination catalog-pagination">
      <button
        className="button button-secondary"
        disabled={isFirstPage || !hasPages}
        onClick={() => onPageChange(Math.max(0, page - 1))}
        type="button"
      >
        Trước
      </button>
      <span aria-live="polite" className="pagination-summary">
        {hasPages ? `Trang ${page + 1} / ${totalPages}` : 'Không có trang nào'}
        {totalElements !== undefined && ` · ${totalElements} mục`}
      </span>
      <button
        className="button button-secondary"
        disabled={isLastPage}
        onClick={() => onPageChange(page + 1)}
        type="button"
      >
        Sau
      </button>
    </nav>
  )
}
