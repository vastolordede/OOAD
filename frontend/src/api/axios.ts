import axios from 'axios'

const baseURL = import.meta.env.VITE_API_URL || '/api'

export const api = axios.create({ baseURL })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

// Clear stale cached sessions after the server rejects a protected request.
// Do not redirect here: React routing reacts to the updated AuthContext.
api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError(error) && error.response?.status === 401
        && localStorage.getItem('accessToken')) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('authUser')
      window.dispatchEvent(new Event('auth-expired'))
    }
    return Promise.reject(error)
  },
)
