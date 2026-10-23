import { useState, type FormEvent } from 'react'
import { createReviewApi } from '../../api/reviews.api'
import type { ReviewResponse } from '../../types/review'
import { useToast } from '../../hooks/useToast'
import { getErrorMessage } from '../../utils/getErrorMessage'

export function OrderReviewForm({
  orderItemId,
  onSubmitted,
}: {
  orderItemId: number
  onSubmitted: (review: ReviewResponse) => void
}) {
  const toast = useToast()
  const [rating, setRating] = useState('')
  const [comment, setComment] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const numericRating = Number(rating)
    if (!Number.isInteger(numericRating) || numericRating < 1 || numericRating > 5) {
      toast.error('Choose a rating from 1 to 5.')
      return
    }

    setIsSubmitting(true)
    try {
      const review = await createReviewApi({
        orderItemId,
        rating: numericRating,
        comment: comment.trim() || null,
      })
      onSubmitted(review)
      toast.success('Review submitted.')
    } catch (error) {
      toast.error(getErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="order-review-form" onSubmit={handleSubmit}>
      <label>
        Rating
        <select onChange={(event) => setRating(event.target.value)} required value={rating}>
          <option value="">Choose a rating</option>
          {[1, 2, 3, 4, 5].map((value) => (
            <option key={value} value={value}>{value} / 5</option>
          ))}
        </select>
      </label>
      <label>
        Comment (optional)
        <textarea
          maxLength={2000}
          onChange={(event) => setComment(event.target.value)}
          rows={3}
          value={comment}
        />
      </label>
      <button className="button button-secondary" disabled={isSubmitting} type="submit">
        {isSubmitting ? 'Submitting…' : 'Submit review'}
      </button>
    </form>
  )
}
