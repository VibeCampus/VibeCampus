<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import UserAvatar from '@/components/UserAvatar.vue'
import authApi from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const SOCIAL_KEYS = ['social_find', 'social_buddy', 'social_love']

const categories = [
  { name: '最新墙', key: '' },
  { name: '社交墙', key: 'social' },
  { name: '分享墙', key: 'share' },
  { name: '买卖墙', key: 'trade' },
  { name: '综合墙', key: 'general', badge: 'NEW' },
]

const searchQuery = ref('')
const isLoggedIn = computed(() => userStore.isLoggedIn)

function isNavActive(catKey) {
  const q = route.query.category || ''
  if (catKey === 'social') {
    return q === 'social' || SOCIAL_KEYS.includes(q)
  }
  return q === catKey
}
const showUserMenu = ref(false)

function closeUserMenu() {
  showUserMenu.value = false
}

function toggleCareMode() {
  document.documentElement.classList.toggle('care-mode')
  closeUserMenu()
}

function toggleA11yHint() {
  document.documentElement.classList.toggle('a11y-mode')
  closeUserMenu()
}

function selectCategory(key) {
  router.push({ path: '/', query: key ? { category: key } : {} })
}

function submitSearch() {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { keyword: searchQuery.value.trim() } })
  }
}

async function handleLogout() {
  showUserMenu.value = false
  try {
    await authApi.logout()
  } catch {
    // 无论接口是否成功，清理本地态
  }
  userStore.logout()
  router.push('/')
}
</script>

<template>
  <header class="sticky top-0 z-50 bg-white border-b border-[#EBEBEB]">
    <div class="max-w-[1180px] mx-auto px-5 h-[64px] flex items-center gap-0">

      <!-- ① Logo -->
      <RouterLink to="/"
        class="shrink-0 mr-6 text-[#1772F6] font-black text-[22px] tracking-tight cursor-pointer hover:opacity-85 transition-opacity select-none">
        校园信息墙
      </RouterLink>

      <!-- ② Nav tabs -->
      <nav class="flex items-center h-full shrink-0">
        <button
          v-for="cat in categories"
          :key="cat.key"
          @click="selectCategory(cat.key)"
          :class="[
            'relative flex items-center gap-1 px-3.5 h-full text-[15px] cursor-pointer transition-colors whitespace-nowrap select-none',
            isNavActive(cat.key)
              ? 'text-[#1772F6] font-medium after:absolute after:bottom-0 after:left-0 after:right-0 after:h-[2px] after:bg-[#1772F6] after:content-[\'\']'
              : 'text-[#444] hover:text-[#1772F6]',
          ]"
        >
          {{ cat.name }}
          <span v-if="cat.badge"
            class="text-[9px] font-bold px-1 py-0 bg-[#FF6B6B] text-white leading-[14px]">
            {{ cat.badge }}
          </span>
        </button>
      </nav>

      <!-- ③ Search bar（居中撑开区域） -->
      <div class="flex-1 flex justify-center px-6">
        <div class="flex w-full max-w-[320px]">
          <input
            v-model="searchQuery"
            @keydown.enter="submitSearch"
            placeholder="搜索校园帖子..."
            class="flex-1 h-[36px] px-3 text-[14px] bg-[#F6F6F6] border border-[#EBEBEB] border-r-0 outline-none focus:bg-white focus:border-[#1772F6] transition-colors"
          />
          <button @click="submitSearch"
            class="w-[44px] h-[36px] flex items-center justify-center bg-[#1772F6] text-white hover:bg-[#0d65e8] transition-colors cursor-pointer shrink-0">
            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-4.35-4.35M17 11A6 6 0 115 11a6 6 0 0112 0z" />
            </svg>
          </button>
        </div>
      </div>

      <!-- ④ Right actions -->
      <div class="flex items-center gap-1 shrink-0">

        <!-- 未登录 -->
        <template v-if="!isLoggedIn">
          <RouterLink to="/userlogin"
            class="px-4 h-[34px] flex items-center text-[14px] text-[#1772F6] border border-[#1772F6] hover:bg-[#E8F3FF] transition-colors cursor-pointer">
            登录
          </RouterLink>
          <RouterLink to="/register"
            class="px-4 h-[34px] flex items-center bg-[#1772F6] text-white text-[14px] hover:bg-[#0d65e8] transition-colors cursor-pointer ml-1">
            注册
          </RouterLink>
        </template>

        <!-- 已登录 -->
        <template v-else>
          <!-- 发布按钮（对应知乎"D·直答"胶囊） -->
          <RouterLink to="/post/create"
            class="flex items-center gap-1.5 px-3.5 h-[34px] bg-[#1772F6] text-white text-[14px] font-medium hover:bg-[#0d65e8] transition-colors cursor-pointer mr-2">
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
            </svg>
            发布
          </RouterLink>

          <!-- 私信（图标 + 文字） -->
          <RouterLink to="/message"
            class="flex flex-col items-center justify-center w-12 h-full text-[#8590A6] hover:text-[#1772F6] transition-colors cursor-pointer relative gap-0.5 pt-1">
            <div class="relative">
              <svg class="w-[22px] h-[22px]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
              </svg>
              <span class="absolute -top-1 -right-1.5 min-w-[16px] h-4 bg-[#FF6B6B] text-white text-[10px] font-bold flex items-center justify-center px-0.5 leading-none">3</span>
            </div>
            <span class="text-[11px] leading-none">私信</span>
          </RouterLink>

          <!-- 用户头像 + 下拉菜单 -->
          <div class="relative ml-2">
            <button
              @click="showUserMenu = !showUserMenu"
              class="w-9 h-9 overflow-hidden rounded-full cursor-pointer hover:opacity-85 transition-opacity shrink-0 flex items-center justify-center bg-[#F0F2F5] border border-[#EBEBEB]"
            >
              <UserAvatar
                :user="userStore.userInfo"
                :clickable="false"
                size-class="w-9 h-9"
                text-class="text-xs"
              />
            </button>

            <div
              v-if="showUserMenu"
              @click.stop
              class="absolute left-1/2 -translate-x-1/2 top-[calc(100%+10px)] z-50 w-[150px] rounded-lg bg-white shadow-[0_4px_24px_rgba(0,0,0,0.12)] border border-[#EBEBEB]"
            >
              <!-- 指向头像的小三角（与面板同宽居中） -->
              <div
                class="absolute -top-[9px] left-1/2 -translate-x-1/2 w-0 h-0 border-l-[8px] border-r-[8px] border-b-[9px] border-l-transparent border-r-transparent border-b-[#EBEBEB]"
                aria-hidden="true"
              />
              <div
                class="absolute -top-[7px] left-1/2 -translate-x-1/2 w-0 h-0 border-l-[6px] border-r-[6px] border-b-[7px] border-l-transparent border-r-transparent border-b-white"
                aria-hidden="true"
              />

              <nav class="relative py-1 rounded-lg overflow-hidden bg-white">
                <RouterLink
                  to="/User/profile"
                  @click="closeUserMenu"
                  class="flex items-center gap-2 px-2.5 py-2 text-[12px] text-[#5A6B7C] hover:bg-[#F7F8FA] transition-colors cursor-pointer"
                >
                  <svg class="w-[16px] h-[16px] shrink-0 text-[#8590A6]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.75">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                  </svg>
                  我的主页
                </RouterLink>

                <RouterLink
                  to="/search"
                  @click="closeUserMenu"
                  class="flex items-center gap-2 px-2.5 py-2 text-[12px] text-[#5A6B7C] hover:bg-[#F7F8FA] transition-colors cursor-pointer"
                >
                  <svg class="w-[16px] h-[16px] shrink-0 text-[#8590A6]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.75">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  最近浏览
                </RouterLink>

                <button
                  type="button"
                  @click="toggleA11yHint"
                  class="w-full flex items-center gap-2 px-2.5 py-2 text-left text-[12px] text-[#5A6B7C] hover:bg-[#F7F8FA] transition-colors cursor-pointer"
                >
                  <svg class="w-[16px] h-[16px] shrink-0 text-[#8590A6]" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M11.645 20.91l-.007-.003-.022-.012a15.247 15.247 0 01-.383-.218 25.18 25.18 0 01-4.244-3.17C4.688 15.36 2.25 12.174 2.25 8.25 2.25 5.322 4.714 3 7.688 3c1.876 0 3.426.805 4.5 2.09 1.074-1.285 2.624-2.09 4.5-2.09 2.974 0 5.438 2.322 5.438 5.25 0 3.924-2.438 7.11-5.366 9.257a25.128 25.128 0 01-4.244 3.17l-.022.012-.007.003-.002.001h-.002z" />
                  </svg>
                  无障碍
                </button>

                <button
                  type="button"
                  @click="toggleCareMode"
                  class="w-full flex items-center gap-2 px-2.5 py-2 text-left text-[12px] text-[#5A6B7C] hover:bg-[#F7F8FA] transition-colors cursor-pointer"
                >
                  <svg class="w-[16px] h-[16px] shrink-0 text-[#8590A6]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.75">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.182 15.182a4.5 4.5 0 01-6.364 0M21 12a9 9 0 11-18 0 9 9 0 0118 0zM9.75 9.75c0 .414-.168.75-.375.75S9 10.164 9 9.75 9.168 9 9.375 9s.375.336.375.75zm-.375 0h.008v.008H9.375V9.75zm4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 0h.008v.008h-.008V9.75z" />
                  </svg>
                  关怀版
                </button>

                <RouterLink
                  to="/User/profile"
                  @click="closeUserMenu"
                  class="flex items-center gap-2 px-2.5 py-2 text-[12px] text-[#5A6B7C] hover:bg-[#F7F8FA] transition-colors cursor-pointer"
                >
                  <svg class="w-[16px] h-[16px] shrink-0 text-[#8590A6]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.75">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.324.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 011.37.49l1.296 2.247a1.125 1.125 0 01-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 010 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 01-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 01-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 01-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 01-1.369-.49l-1.297-2.247a1.125 1.125 0 01.26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 010-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 01-.26-1.43l1.297-2.247a1.125 1.125 0 011.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281z" />
                    <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  设置
                </RouterLink>

                <button
                  type="button"
                  @click="handleLogout"
                  class="w-full flex items-center gap-2 px-2.5 py-2 text-left text-[12px] text-[#5A6B7C] hover:bg-[#F7F8FA] transition-colors cursor-pointer"
                >
                  <svg class="w-[16px] h-[16px] shrink-0 text-[#8590A6]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.75">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M5.636 5.636a9 9 0 1012.728 0M12 3v9" />
                  </svg>
                  退出
                </button>
              </nav>
            </div>
            <div v-if="showUserMenu" class="fixed inset-0 z-40" @click="showUserMenu = false" />
          </div>
        </template>
      </div>
    </div>
  </header>
</template>
