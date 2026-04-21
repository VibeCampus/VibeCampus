<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PostCard from '@/components/PostCard.vue'
import HotRankSidebar from '@/components/HotRankSidebar.vue'
import { useSocialStore } from '@/stores/social'

const SOCIAL_KEYS = ['social_find', 'social_buddy', 'social_love']

const route = useRoute()
const router = useRouter()
const socialStore = useSocialStore()
const loading = false
const deployProbeLabel = 'cicd-check-20260421'

const activeCategory = computed(() => route.query.category || '')

const filtered = computed(() => {
  const q = activeCategory.value
  if (!q) return socialStore.feedPosts
  if (q === 'social') return socialStore.feedPosts.filter(p => SOCIAL_KEYS.includes(p.category))
  return socialStore.feedPosts.filter(p => p.category === q)
})

const showSocialSubNav = computed(() => {
  const q = activeCategory.value
  return q === 'social' || SOCIAL_KEYS.includes(q)
})

const socialSubItems = [
  { key: 'social', label: '全部' },
  { key: 'social_find', label: '捞人' },
  { key: 'social_buddy', label: '找搭子' },
  { key: 'social_love', label: '恋爱' },
]

function setSocialSubFilter(key) {
  router.push({ path: '/', query: key ? { category: key } : {} })
}

function isSocialSubActive(key) {
  const q = activeCategory.value
  if (key === 'social') return q === 'social'
  return q === key
}
</script>

<template>
  <div class="min-h-screen bg-[#F6F6F6]">
    <!-- Publish hint -->
    <div class="bg-white border-b border-[#EBEBEB]">
      <div class="max-w-[1100px] mx-auto px-4 py-3 flex items-center gap-3">
        <span
          class="shrink-0 text-[12px] px-2 py-1 bg-[#e8f3ff] text-[#1772F6] border border-[#b9d8ff] rounded"
        >
          部署验证: {{ deployProbeLabel }}
        </span>
        <RouterLink to="/post/create"
          class="flex-1 text-[14px] text-[#8590A6] bg-[#F6F6F6] border border-[#EBEBEB] px-4 py-2 cursor-pointer hover:border-[#1772F6] hover:text-[#1772F6] transition-colors">
          分享此刻的想法…
        </RouterLink>
      </div>
    </div>

    <div class="max-w-[1100px] mx-auto px-4 pt-4 flex gap-5 items-start">
      <!-- Feed -->
      <main class="flex-1 min-w-0">
        <!-- 社交墙：子类型筛选 -->
        <div v-if="showSocialSubNav" class="mb-3 flex flex-wrap items-center gap-2">
          <span class="text-[12px] text-[#8590A6] shrink-0">类型</span>
          <button
            v-for="item in socialSubItems"
            :key="item.key"
            type="button"
            @click="setSocialSubFilter(item.key)"
            :class="[
              'px-3 py-1 text-[13px] rounded border transition-colors cursor-pointer',
              isSocialSubActive(item.key)
                ? 'bg-[#1772F6] text-white border-[#1772F6]'
                : 'bg-white text-[#444] border-[#EBEBEB] hover:border-[#1772F6] hover:text-[#1772F6]',
            ]"
          >
            {{ item.label }}
          </button>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="bg-white border border-[#EBEBEB]">
          <div v-for="i in 3" :key="i" class="p-6 border-b border-[#EBEBEB] last:border-b-0 animate-pulse">
            <div class="flex gap-3 mb-3">
              <div class="w-8 h-8 bg-[#EBEBEB]" />
              <div class="flex-1">
                <div class="h-3.5 w-24 bg-[#EBEBEB] mb-1.5" />
                <div class="h-3 w-16 bg-[#F0F0F0]" />
              </div>
            </div>
            <div class="space-y-2">
              <div class="h-3.5 bg-[#EBEBEB] w-full" />
              <div class="h-3.5 bg-[#EBEBEB] w-5/6" />
            </div>
          </div>
        </div>

        <!-- Empty -->
        <div v-else-if="filtered.length === 0"
          class="bg-white border border-[#EBEBEB] py-16 flex flex-col items-center gap-3 text-[#8590A6]">
          <svg class="w-12 h-12 opacity-30" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
              d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
          <p class="text-[14px]">该分类暂无内容</p>
          <RouterLink to="/post/create"
            class="px-5 py-2 bg-[#1772F6] text-white text-[13px] hover:bg-[#0d65e8] transition-colors">
            去发布第一条
          </RouterLink>
        </div>

        <!-- Post list -->
        <div v-else class="bg-white border border-[#EBEBEB]">
          <PostCard v-for="post in filtered" :key="post.id" :post="post" />
        </div>
      </main>

      <!-- Sidebar -->
      <HotRankSidebar />
    </div>
  </div>
</template>
