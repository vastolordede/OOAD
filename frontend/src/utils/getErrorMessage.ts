import axios from 'axios'
import { translateErrorMessage, translateFieldName } from './localization'

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
        return [
          translateErrorMessage(message),
          ...Object.entries(data.fields)
            .filter((entry): entry is [string, string] => typeof entry[1] === 'string')
            .map(([field, fieldMessage]) =>
              `${translateFieldName(field)}: ${translateErrorMessage(fieldMessage)}`,
            ),
        ].join(' ')
      }

      if (typeof message === 'string' && message.length > 0) {
        return translateErrorMessage(message)
      }
    }

    return error.response
      ? `Yêu cầu không thành công (mã lỗi ${error.response.status}).`
      : 'Không thể kết nối đến máy chủ. Vui lòng kiểm tra địa chỉ API và thử lại.'
  }

  if (error instanceof Error) {
    return translateErrorMessage(error.message)
  }

  return 'Đã xảy ra lỗi không mong muốn.'
}
