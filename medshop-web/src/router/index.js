import { createRouter, createWebHashHistory } from 'vue-router'

/**
 * 路由：每个页面独立懒加载组件，结构清晰、各司其职。
 * 需登录的页面用 meta.requiresAuth，在守卫里拦截。
 */
const routes = [
  { path: '/', redirect: '/home' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', plain: true }
  },
  {
    path: '/home',
    name: 'home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页', requiresAuth: true }
  },
  {
    path: '/drugs',
    name: 'drugs',
    component: () => import('@/views/DrugListView.vue'),
    meta: { title: '药品', requiresAuth: true }
  },
  {
    path: '/drugs/:id',
    name: 'drug-detail',
    component: () => import('@/views/DrugDetailView.vue'),
    meta: { title: '药品详情', requiresAuth: true }
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('@/views/CartView.vue'),
    meta: { title: '购物车', requiresAuth: true, role: 0 }
  },
  {
    path: '/orders/:id',
    name: 'order-detail',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { title: '订单', requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/views/OrdersView.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/consult',
    name: 'consult',
    component: () => import('@/views/ConsultView.vue'),
    meta: { title: '智能药师', requiresAuth: true, role: 0 }
  },
  {
    path: '/prescriptions',
    name: 'prescriptions',
    component: () => import('@/views/PrescriptionView.vue'),
    meta: { title: '我的处方', requiresAuth: true }
  },
  {
    path: '/privacy',
    name: 'privacy',
    component: () => import('@/views/PrivacyView.vue'),
    meta: { title: '隐私与授权', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/AdminView.vue'),
    meta: { title: '管理后台', requiresAuth: true, role: 3 }
  },
  // 药师工作台：拆为「转人工工单」与「处方审核」两个并列页
  { path: '/workbench', redirect: '/workbench/handoff' },
  {
    path: '/workbench/handoff',
    name: 'workbench-handoff',
    component: () => import('@/views/HandoffView.vue'),
    meta: { title: '转人工工单', requiresAuth: true, role: 1 }
  },
  {
    path: '/workbench/prescriptions',
    name: 'workbench-rx',
    component: () => import('@/views/RxReviewView.vue'),
    meta: { title: '处方审核', requiresAuth: true, role: 1 }
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 登录守卫 + 角色守卫
router.beforeEach((to) => {
  const token = localStorage.getItem('medshop_token')
  if (to.meta.requiresAuth && !token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && token) {
    return { name: 'home' }
  }
  // 角色守卫：页面声明了 meta.role 时，校验当前用户角色
  if (to.meta.role != null && token) {
    const user = JSON.parse(localStorage.getItem('medshop_user') || 'null')
    if (user?.role !== to.meta.role) {
      return { name: 'home' }
    }
  }
  document.title = to.meta.title ? `${to.meta.title} · 松和堂` : '松和堂'
})

export default router
