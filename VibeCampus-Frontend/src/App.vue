<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import { useUserStore } from './stores/user'
import { useSocialStore } from './stores/social'

const route = useRoute()
const userStore = useUserStore()
const socialStore = useSocialStore()
const noHeaderRoutes = ['/userlogin', '/register', '/forgot-password', '/admin/login']
const showHeader = computed(() => !noHeaderRoutes.includes(route.path))

onMounted(async () => {
  if (userStore.userInfo) {
    socialStore.syncUserFromProfile(userStore.userInfo)
  }
  if (!userStore.token) {
    return
  }
  try {
    await userStore.refreshFromServer()
    if (userStore.userInfo) {
      socialStore.syncUserFromProfile(userStore.userInfo)
    }
  } catch {
    // 401 时由 axios 拦截器跳转登录
  }
})
</script>

<template>
  <div class="min-h-screen bg-[#F6F6F6]">
    <AppHeader v-if="showHeader" />
    <RouterView />
  </div>
</template>
