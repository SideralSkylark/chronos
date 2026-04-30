import { defineStore } from 'pinia'
import api from '@/services/api'
import type { Notification } from '@/services/dto/notification'
import type { ApiResponse } from '@/services/responses/apiResponse'

interface NotificationState {
  notifications: Notification[]
  unreadCount: number
  loading: boolean
}

let pollingInterval: number | null = null

export const useNotificationStore = defineStore('notification', {
  state: (): NotificationState => ({
    notifications: [],
    unreadCount: 0,
    loading: false,
  }),

  actions: {
    async fetchNotifications() {
      this.loading = true
      try {
        const { data } = await api.get<ApiResponse<Notification[]>>('/v1/notifications')
        this.notifications = data.data
      } catch (err) {
        console.error('Erro ao buscar notificações:', err)
      } finally {
        this.loading = false
      }
    },

    async fetchUnreadCount() {
      try {
        const { data } = await api.get<ApiResponse<{ count: number }>>('/v1/notifications/unread-count')
        this.unreadCount = data.data.count

        if (pollingInterval === null) {
          this.startPolling()
        }
      } catch (err) {
        console.error('Erro ao buscar contagem de não lidas:', err)
      }
    },

    async markAllRead() {
      try {
        await api.post('/v1/notifications/mark-read')
        this.unreadCount = 0
        this.notifications = this.notifications.map((n) => ({ ...n, read: true }))
      } catch (err) {
        console.error('Erro ao marcar como lidas:', err)
      }
    },

    startPolling() {
      if (pollingInterval !== null) return
      pollingInterval = window.setInterval(() => {
        this.fetchUnreadCount()
      }, 30000)
    },

    stopPolling() {
      if (pollingInterval !== null) {
        clearInterval(pollingInterval)
        pollingInterval = null
      }
    },
  },
})
