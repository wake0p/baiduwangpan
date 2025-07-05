import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/login/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/login/Register.vue')
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/pages/login/ResetPassword.vue')
  },
  {
    path: '/reset-password-set',
    name: 'ResetPasswordSet',
    component: () => import('@/pages/login/ResetPasswordSet.vue')
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/pages/dashboard/index.vue'),
    children: [
      {
        path: '',
        name: 'FileList',
        component: () => import('@/pages/dashboard/file-list.vue')
      },
      {
        path: 'recycle',
        name: 'Recycle',
        component: () => import('@/pages/dashboard/recycle.vue')
      },
      {
        path: 'user-management',
        name: 'UserManagement',
        component: () => import('@/pages/dashboard/user-management.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/pages/dashboard/profile.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录不允许访问 /dashboard
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isLoggedIn = userStore.user && userStore.user.status === 1

  if (to.path === '/login' && isLoggedIn) {
    next('/dashboard')
    return
  }

  if (to.path.startsWith('/dashboard')) {
    if (!isLoggedIn) {
      next('/login')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router