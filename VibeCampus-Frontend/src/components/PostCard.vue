<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useSocialStore } from '@/stores/social'
import UserAvatar from '@/components/UserAvatar.vue'

const props = defineProps({
  post: { type: Object, required: true },
})

const router = useRouter()
const socialStore = useSocialStore()
const liked = ref(!!props.post.liked)
const localLikes = ref(props.post.likes || 0)

watch(
  () => [props.post.liked, props.post.likes],
  ([newLiked, newLikes]) => {
    liked.value = !!newLiked
    localLikes.value = newLikes || 0
  },
)

const categoryConfig = {
  social_find: { label: '捞人', color: 'text-violet-600 bg-violet-50 border-violet-100' },
  social_buddy: { label: '找搭子', color: 'text-cyan-700 bg-cyan-50 border-cyan-100' },
  social_love: { label: '恋爱', color: 'text-rose-500 bg-rose-50 border-rose-100' },
  share: { label: '分享墙', color: 'text-sky-600 bg-sky-50 border-sky-100' },
  trade: { label: '买卖墙', color: 'text-amber-600 bg-amber-50 border-amber-100' },
  general: { label: '综合墙', color: 'text-emerald-600 bg-emerald-50 border-emerald-100' },
  latest: { label: '最新墙', color: 'text-[#1772F6] bg-[#E8F3FF] border-[#C7DEFF]' },
}

const catInfo = computed(() => categoryConfig[props.post.category] || categoryConfig.latest)
const authorUser = computed(() => props.post.author || null)
const showFollowButton = computed(() =>
  !props.post.anonymous &&
  !!authorUser.value &&
  authorUser.value.id !== socialStore.currentUserId,
)

function goDetail() {
  router.push(`/c/${props.post.id}`)
}

function toggleLike(e) {
  e.stopPropagation()
  // 乐观更新：后端暂未提供 POST /api/posts/{id}/like，先做本地切换
  if (liked.value) {
    liked.value = false
    localLikes.value = Math.max(0, localLikes.value - 1)
  } else {
    liked.value = true
    localLikes.value += 1
  }
}

function toggleFollow(e) {
  e.stopPropagation()
  if (!authorUser.value) return
  socialStore.toggleFollow(authorUser.value.id)
}
</script>

<template>
  <article
    @click="goDetail"
    class="bg-white border-b border-[#EBEBEB] px-6 py-5 cursor-pointer hover:bg-[#FAFAFA] transition-colors group"
  >
    <!-- Author row -->
    <div class="flex items-center gap-2.5 mb-3">
      <UserAvatar
        :user="authorUser"
        :anonymous="post.anonymous"
        size-class="w-8 h-8"
        text-class="text-xs"
      />
      <div class="flex-1 min-w-0">
        <span class="text-[13px] font-medium text-[#1A1A1A]">
          {{ post.anonymous ? '匿名用户' : authorUser?.username }}
        </span>
        <span class="mx-1.5 text-[#EBEBEB]">·</span>
        <span class="text-[12px] text-[#8590A6]">{{ post.time }}</span>
      </div>
      <button
        v-if="showFollowButton"
        @click="toggleFollow"
        :class="[
          'shrink-0 px-3 py-1 text-[12px] border transition-colors cursor-pointer',
          socialStore.isFollowing(authorUser.id)
            ? 'text-[#8590A6] border-[#D0D7E2] hover:border-[#1772F6] hover:text-[#1772F6]'
            : 'text-white bg-[#1772F6] border-[#1772F6] hover:bg-[#0d65e8]'
        ]"
      >
        {{ socialStore.isFollowing(authorUser.id) ? '已关注' : '关注' }}
      </button>
      <span :class="['shrink-0 text-[11px] px-1.5 py-0.5 border', catInfo.color]">
        {{ catInfo.label }}
      </span>
    </div>

    <!-- Content -->
    <p class="text-[15px] text-[#1A1A1A] leading-[1.7] line-clamp-3 mb-3">{{ post.content }}</p>

    <!-- Images -->
    <div v-if="post.images && post.images.length" class="flex gap-1.5 mb-3">
      <div
        v-for="(img, i) in post.images.slice(0, 3)"
        :key="i"
        class="relative w-20 h-20 overflow-hidden bg-[#F6F6F6] shrink-0"
      >
        <img :src="img" :alt="`图片${i+1}`" class="w-full h-full object-cover" />
        <div
          v-if="i === 2 && post.images.length > 3"
          class="absolute inset-0 bg-black/50 flex items-center justify-center text-white text-sm font-bold"
        >
          +{{ post.images.length - 3 }}
        </div>
      </div>
    </div>

    <!-- Footer -->
    <div class="flex items-center gap-5 text-[13px] text-[#8590A6]">
      <button
        @click="toggleLike"
        :class="['flex items-center gap-1 hover:text-[#1772F6] transition-colors cursor-pointer', liked ? 'text-[#1772F6]' : '']"
      >
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M6.633 10.5c.806 0 1.533-.446 2.031-1.08a9.041 9.041 0 012.861-2.4c.723-.384 1.35-.956 1.653-1.715a4.498 4.498 0 00.322-1.672V3a.75.75 0 01.75-.75A2.25 2.25 0 0116.5 4.5c0 1.152-.26 2.243-.723 3.218-.266.558.107 1.282.725 1.282h3.126c1.026 0 1.945.694 2.054 1.715.045.422.068.85.068 1.285a11.95 11.95 0 01-2.649 7.521c-.388.482-.987.729-1.605.729H13.48c-.483 0-.964-.078-1.423-.23l-3.114-1.04a4.501 4.501 0 00-1.423-.23H5.904M14.25 9h2.25M5.904 18.75c.083.205.173.405.27.602.197.4-.078.898-.523.898h-.908c-.889 0-1.713-.518-1.972-1.368a12 12 0 01-.521-3.507c0-1.553.295-3.036.831-4.398C3.387 10.203 4.167 9.75 5 9.75h1.053c.472 0 .745.556.5.96a8.958 8.958 0 00-1.302 4.665c0 1.194.232 2.333.654 3.375z" />
        </svg>
        赞同 {{ localLikes }}
      </button>
      <button @click.stop class="flex items-center gap-1 hover:text-[#1772F6] transition-colors cursor-pointer">
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
        </svg>
        {{ post.comments || 0 }} 评论
      </button>
      <button @click.stop class="flex items-center gap-1 hover:text-[#1772F6] transition-colors cursor-pointer ml-auto">
        <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M7.217 10.907a2.25 2.25 0 100 2.186m0-2.186c.18.324.283.696.283 1.093s-.103.77-.283 1.093m0-2.186l9.566-5.314m-9.566 7.5l9.566 5.314m0 0a2.25 2.25 0 103.935 2.186 2.25 2.25 0 00-3.935-2.186zm0-12.814a2.25 2.25 0 103.933-2.185 2.25 2.25 0 00-3.933 2.185z" />
        </svg>
        分享
      </button>
    </div>
  </article>
</template>
