import axios from 'axios'

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const responseBody: unknown = error.response?.data

    if (isRecord(responseBody)) {
      const message = responseBody.message
      const data = responseBody.data

      if (typeof message === 'string' && isRecord(data) && isRecord(data.fields)) {
        const fieldMessages = Object.values(data.fields).filter(
          (value): value is string => typeof value === 'string',
        )
        return [message, ...fieldMessages].join(': ')
      }

      if (typeof message === 'string' && message.length > 0) {
        return message
      }
    }

    return error.response
      ? `Request failed with status ${error.response.status}.`
      : 'Unable to connect to the server. Check the API URL and try again.'
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'An unexpected error occurred.'
}
