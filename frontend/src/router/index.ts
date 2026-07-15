import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import DashboardLayout from '@/layouts/DashboardLayout.vue'
import Dashboard from '@/views/Dashboard.vue'
import Users from '@/views/Users.vue'
import Login from '@/views/Login.vue'
import NotFound from '@/views/NotFound.vue'
import Cohorts from '@/views/Cohorts.vue'
import Timetable from '@/views/Timetable.vue'
import Forbidden from '@/views/Forbidden.vue'
import Rooms from '@/views/Rooms.vue'
import Courses from '@/views/Courses.vue'
import MyTimetableView from '@/views/MyTimetableView.vue'

const routes = [
  {
    path: '/',
    name: 'Login',
    component: Login,
    meta: { requiresGuest: true },
  },
  {
    path: '/dashboard',
    component: DashboardLayout,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'DashboardHome',
        component: Dashboard,
        meta: { roles: ['ADMIN', 'DIRECTOR', 'ASISTENT', 'COORDINATOR'], label: 'Início', hasStickyHeader: true },
      },
      {
        path: 'rooms',
        name: 'Rooms',
        component: Rooms,
        meta: { roles: ['ADMIN', 'DIRECTOR', 'ASISTENT'], label: 'Salas' },
      },
      {
        path: 'courses',
        name: 'Courses',
        component: Courses,
        meta: { roles: ['ADMIN', 'COORDINATOR'], label: 'Cursos', hasStickyHeader: true },
      },
      { path: 'cohorts', name: 'Cohorts', component: Cohorts, meta: { roles: ['ADMIN', 'COORDINATOR', 'ASISTENT', 'DIRECTOR'], label: 'Turmas' } },
      {
        path: 'users',
        name: 'Users',
        component: Users,
        meta: { roles: ['ADMIN', 'ASISTENT', 'DIRECTOR'], label: 'Utilizadores' },
      },
      {
        path: 'timetable',
        name: 'Timetable',
        component: Timetable,
        meta: { roles: ['ADMIN', 'COORDINATOR', 'ASISTENT', 'DIRECTOR'], label: 'Horarios', hasStickyHeader: true },
      },
      {
        path: 'my-timetable',
        name: 'MyTimetableView',
        component: MyTimetableView,
        meta: { roles: ['STUDENT', 'TEACHER'], label: 'Meu Horario' },
      },

    ],
  },
  { path: '/forbidden', name: 'Forbidden', component: Forbidden },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: NotFound },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'Login' }
  }

  // Redirect authenticated users from guest-only routes (like Login)
  if (to.meta.requiresGuest && auth.isAuthenticated) {
    const roles = auth.user?.roles ?? []
    if (roles.includes('STUDENT') || roles.includes('TEACHER')) {
      return { name: 'MyTimetableView' }
    }
    return { name: 'DashboardHome' }
  }

  // Standard roles check
  if (to.meta.roles && auth.isAuthenticated) {
    const roles = auth.user?.roles ?? []
    const allowed = (to.meta.roles as string[]).some((r) => roles.includes(r))
    if (!allowed) {
      return { name: 'Forbidden', replace: true }
    }
  }

  return true
})

export default router
