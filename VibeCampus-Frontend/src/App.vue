<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from './components/AppHeader.vue'
import { pingApi } from './api'

const route = useRoute()
const noHeaderRoutes = ['/userlogin', '/register', '/forgot-password', '/admin/login']
const showHeader = computed(() => !noHeaderRoutes.includes(route.path))

onMounted(async () => {
  try {
    const res = await pingApi.ping()
    console.debug('[ping]', res)
  } catch (e) {
    console.warn('[ping] failed:', e?.message || e)
  }
})
</script>

<template>
  <div class="min-h-screen bg-[#F6F6F6]">
    <AppHeader v-if="showHeader" />
    <RouterView />
  </div>
</template>
