import { defineStore } from 'pinia'
import { authService } from '@/services/authService'
import { userService } from '@/services/userService'
import { useAuthStore } from './auth'
import type {
  UserResponse,
  CreateUserRequest,
  UpdateUserProfileRequest,
  UpdateUserRequest,
} from '@/services/dto/user'
import type { Page } from '@/services/types/page'

interface State {
  users: UserResponse[]
  pagedUsers: Page<UserResponse> | null
  currentUser: UserResponse | null
  loading: boolean
  error: string | null
}

export const useUserStore = defineStore('user', {
  state: (): State => ({
    users: [],
    pagedUsers: null,
    currentUser: authService.getCurrentUser(),
    loading: false,
    error: null,
  }),

  getters: {
    myHighestRole(state): string {
      const hierarchy: Record<string, number> = {
        USER: 1,
        STUDENT: 2,
        TEACHER: 3,
        COORDINATOR: 4,
        ASISTENT: 5,
        DIRECTOR: 6,
        ADMIN: 7,
      }
      const auth = useAuthStore()
      const user = state.currentUser || auth.user
      if (!user) return 'USER'
      return user.roles.reduce((best, role) => {
        return (hierarchy[role] ?? 0) > (hierarchy[best] ?? 0) ? role : best
      }, 'USER')
    },

    myPriority(state): number {
      const hierarchy: Record<string, number> = {
        USER: 1,
        STUDENT: 2,
        TEACHER: 3,
        COORDINATOR: 4,
        ASISTENT: 5,
        DIRECTOR: 6,
        ADMIN: 7,
      }
      const auth = useAuthStore()
      const user = state.currentUser || auth.user
      if (!user) return 0
      return Math.max(...user.roles.map((r) => hierarchy[r] ?? 0))
    },
  },

  actions: {
    // ===============================
    // Authenticated user actions
    // ===============================
    async fetchCurrentUser() {
      this.loading = true
      try {
        this.currentUser = await userService.me.getProfile()
        this.error = null
      } catch (err: any) {
        this.error = err.message || 'Failed to fetch profile'
      } finally {
        this.loading = false
      }
    },

    async updateProfile(data: UpdateUserProfileRequest) {
      this.loading = true
      try {
        this.currentUser = await userService.me.updateProfile(data)
        this.error = null
      } catch (err: any) {
        this.error = err.message || 'Failed to update profile'
      } finally {
        this.loading = false
      }
    },

    async deleteProfile() {
      this.loading = true
      try {
        await userService.me.deleteProfile()
        this.currentUser = null
        this.error = null
      } catch (err: any) {
        this.error = err.message || 'Failed to delete profile'
      } finally {
        this.loading = false
      }
    },

    // ===============================
    // Admin actions
    // ===============================
    async fetchUsers(
      page = 0,
      size = 20,
      filters?: {
        username?: string
        email?: string
        role?: string
        status?: string
        teacherType?: string
      },
    ) {
      this.loading = true
      try {
        const paged = await userService.admin.getAll(page, size, filters)
        this.pagedUsers = paged
        this.users = paged.content
        this.error = null
      } catch (err: any) {
        this.error = err.message || 'Failed to fetch users'
      } finally {
        this.loading = false
      }
    },

    async fetchStudents(): Promise<UserResponse[]> {
      const page = await userService.admin.getStudents()
      return page.content
    },

    async createUser(data: CreateUserRequest) {
      this.loading = true
      try {
        const newUser = await userService.admin.create(data)
        this.users.push(newUser)
        this.error = null
        return newUser
      } catch (err: any) {
        this.error = err.message || 'Failed to create user'
        throw err
      } finally {
        this.loading = false
      }
    },

    async updateUser(id: number, data: UpdateUserRequest) {
      this.loading = true
      try {
        const updated = await userService.admin.update(id, data)
        const index = this.users.findIndex((u) => u.id === id)
        if (index !== -1) this.users[index] = updated
        this.error = null
        return updated
      } catch (err: any) {
        this.error = err.message || 'Failed to update user'
        throw err
      } finally {
        this.loading = false
      }
    },

    async resetPassword(id: number) {
      this.loading = true
      try {
        const res = await userService.admin.resetPassword(id)
        this.error = null
        return res.temporaryPassword
      } catch (err: any) {
        this.error = err.message || 'Failed to reset password'
        throw err
      } finally {
        this.loading = false
      }
    },

    async deleteUser(id: number) {
      this.loading = true
      try {
        await userService.admin.delete(id)
        this.users = this.users.filter((u) => u.id !== id)
        this.error = null
      } catch (err: any) {
        this.error = err.message || 'Failed to delete user'
        throw err
      } finally {
        this.loading = false
      }
    },
  },
})
