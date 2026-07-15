import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { useToast } from '@/composables/useToast'
import type { ErrorResponse } from './responses/errorResponse'
import { translateErrorMessage } from '@/utils/errorTranslations'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

const api: AxiosInstance = axios.create({
  baseURL: API_URL,
  withCredentials: true,
})

let isRefreshing = false
const failedQueue: {
  resolve: (value?: unknown) => void
  reject: (reason?: any) => void
  config: AxiosRequestConfig
}[] = []

function processQueue(error: any, token: string | null = null) {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error)
    else resolve(token)
  })
  failedQueue.length = 0
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ErrorResponse>) => {
    const originalRequest = error.config
    const toast = useToast()

    if (!error.response || (originalRequest as any)?._retry) {
      if (!error.response) {
        toast.error('Não foi possível ligar ao servidor. Verifique a sua ligação.')
      }
      return Promise.reject(error)
    }

    const skipRefresh = (originalRequest as any)?.skipAuthRefresh

    if (error.response.status == 401 && skipRefresh) {
      return Promise.reject(error)
    }

    // Handle 401 Refresh Token logic
    if (error.response.status === 401) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject, config: originalRequest! })
        })
          .then(() => api(originalRequest!))
          .catch((err) => Promise.reject(err))
      }

      (originalRequest as any)._retry = true
      isRefreshing = true

      try {
        await axios.post(`${API_URL}/v1/auth/refresh-token`, {}, { withCredentials: true })
        processQueue(null)
        return api(originalRequest!)
      } catch (refreshError) {
        processQueue(refreshError)
        localStorage.removeItem('user')
        window.location.href = '/'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    // Global Error Messages
    const rawMessage = error.response.data?.message
    const message = translateErrorMessage(rawMessage || '')

    // Mutate the message so components using it also get the translation
    if (error.response.data) {
      error.response.data.message = message
    }

    switch (error.response.status) {
      case 403:
        toast.error('Não tem permissão para realizar esta ação.')
        break
      case 404:
        toast.error(rawMessage ? message : 'O recurso solicitado não foi encontrado.')
        break
      case 500:
        toast.error('Erro interno do servidor. Tente novamente mais tarde.')
        break
      case 400:
      case 401:
      case 409:
        if (rawMessage) {
          toast.error(message)
        } else {
          toast.error('Ocorreu um erro ao processar o pedido.')
        }
        break
      default:
        toast.error(message || 'Ocorreu um erro inesperado.')
    }

    return Promise.reject(error)
  }
)

export default api
