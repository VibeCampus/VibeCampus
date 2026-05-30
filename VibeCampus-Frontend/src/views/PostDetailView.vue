<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSocialStore } from '@/stores/social'
import UserAvatar from '@/components/UserAvatar.vue'
import postApi from '@/api/post'
import commentApi from '@/api/comment'
import { normalizeComment } from '@/api/normalize'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const socialStore = useSocialStore()
const loadError = ref('')

const liked = ref(false)
const saved = ref(false)
const localLikes = ref(0)
const localFavorites = ref(0)
const lightboxImg = ref(null)
const newComment = ref('')
const replyTarget = ref(null)
const replyContent = ref('')

const comments = ref([])
const commentsLoading = ref(false)
const commentError = ref('')
const submittingComment = ref(false)
const submittingReply = ref(false)

const categoryLabel = {
  social_find: '捞人',
  social_buddy: '找搭子',
  social_love: '恋爱',
  share: '分享墙',
  trade: '买卖墙',
  general: '综合墙',
}

const postId = computed(() => Number(route.params.id))
const post = computed(() => socialStore.getPostById(postId.value))
const isLoggedIn = computed(() => userStore.isLoggedIn)
const currentUser = computed(() => socialStore.currentUser)
const showFollowButton = computed(() =>
  !!post.value?.author &&
  !post.value?.anonymous &&
  post.value.author.id !== socialStore.currentUserId,
)

watch(
  () => post.value,
  next => {
    if (!next) return
    liked.value = !!next.liked
    saved.value = !!next.favorited
    localLikes.value = next.likes || 0
    localFavorites.value = next.favorites || 0
  },
  { immediate: true },
)

onMounted(async () => {
  loadError.value = ''
  try {
    const raw = await postApi.getDetail(postId.value)
    if (raw) {
      socialStore.upsertPostFromServer(raw)
      liked.value = !!raw.liked
      saved.value = !!raw.favorited
      localLikes.value = raw.likes || 0
      localFavorites.value = raw.favorites || 0
    }
  } catch (e) {
    loadError.value = e?.message || '加载失败'
  }
  await loadComments()
})

async function loadComments() {
  commentsLoading.value = true
  commentError.value = ''
  try {
    const res = await commentApi.getList(postId.value, { page: 1, pageSize: 100 })
    const list = Array.isArray(res?.list) ? res.list : []
    comments.value = list.map(normalizeComment).filter(Boolean)
  } catch (e) {
    commentError.value = e?.message || '评论加载失败'
    comments.value = []
  } finally {
    commentsLoading.value = false
  }
}

function toggleLike() {
  // 后端尚未暴露帖子点赞接口，先做本地乐观切换。
  if (liked.value) {
    liked.value = false
    localLikes.value = Math.max(0, localLikes.value - 1)
  } else {
    liked.value = true
    localLikes.value += 1
  }
}

function toggleSave() {
  if (saved.value) {
    saved.value = false
    localFavorites.value = Math.max(0, localFavorites.value - 1)
  } else {
    saved.value = true
    localFavorites.value += 1
  }
}

function toggleFollow() {
  if (!post.value?.author) return
  socialStore.toggleFollow(post.value.author.id)
}

async function submitComment() {
  const content = newComment.value.trim()
  if (!content || !post.value || submittingComment.value) return
  submittingComment.value = true
  commentError.value = ''
  try {
    const res = await commentApi.create(post.value.id, { content })
    const created = normalizeComment(res)
    if (created) {
      created.replies = created.replies || []
      comments.value.unshift(created)
    }
    newComment.value = ''
  } catch (e) {
    commentError.value = e?.message || '评论发布失败'
  } finally {
    submittingComment.value = false
  }
}

async function submitReply() {
  const content = replyContent.value.trim()
  if (!content || !replyTarget.value || submittingReply.value) return
  submittingReply.value = true
  try {
    const payload = { content }
    if (replyTarget.value.replyToUserId) {
      payload.replyToUserId = replyTarget.value.replyToUserId
    }
    const res = await commentApi.reply(replyTarget.value.commentId, payload)
    const created = normalizeComment(res)
    const parent = comments.value.find(c => c.id === replyTarget.value.commentId)
    if (parent && created) {
      parent.replies = [...(parent.replies || []), created]
    }
    replyContent.value = ''
    replyTarget.value = null
  } catch (e) {
    commentError.value = e?.message || '回复发布失败'
  } finally {
    submittingReply.value = false
  }
}

async function toggleCommentLike(comment) {
  if (!isLoggedIn.value) {
    router.push({ path: '/userlogin', query: { redirect: route.fullPath } })
    return
  }
  try {
    const res = await commentApi.toggleLike(comment.id)
    comment.liked = !!res?.liked
    comment.likes = res?.likeCount ?? comment.likes
  } catch (e) {
    commentError.value = e?.message || '点赞失败'
  }
}

function openReplyTo(comment, reply = null) {
  replyTarget.value = {
    commentId: comment.id,
    author: reply?.author?.username || comment.author?.username || '',
    replyToUserId: reply?.author?.id || null,
  }
  replyContent.value = ''
}
</script>

<template>
  <div class="min-h-screen bg-[#F6F6F6]">
    <div class="max-w-[860px] mx-auto px-4 py-5 flex gap-5 items-start">
      <main class="flex-1 min-w-0">
        <div v-if="!post" class="bg-white border border-[#EBEBEB] px-6 py-10 text-center">
          <p v-if="loadError" class="text-[13px] text-amber-800 bg-amber-50 border border-amber-200 px-3 py-2 mb-3 text-left">
            {{ loadError }}
          </p>
          <p class="text-[15px] text-[#1A1A1A] mb-3">帖子不存在或已删除</p>
          <button
            @click="router.push('/')"
            class="px-5 py-2 bg-[#1772F6] text-white text-[13px] hover:bg-[#0d65e8] transition-colors cursor-pointer"
          >
            返回首页
          </button>
        </div>

        <template v-else>
          <div class="bg-white border border-[#EBEBEB] mb-3">
            <div class="px-6 py-3 border-b border-[#EBEBEB] flex items-center gap-2 text-[13px] text-[#8590A6]">
              <button @click="router.back()" class="flex items-center gap-1 hover:text-[#1772F6] cursor-pointer transition-colors">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
                返回
              </button>
              <span class="text-[#EBEBEB]">/</span>
              <span>{{ categoryLabel[post.category] || '校园墙' }}</span>
            </div>

            <div class="px-6 py-6">
              <div class="flex items-center gap-3 mb-5">
                <UserAvatar
                  :user="post.author"
                  :anonymous="post.anonymous"
                  size-class="w-12 h-12"
                  text-class="text-sm"
                />
                <div class="min-w-0">
                  <p class="text-[15px] font-semibold text-[#1A1A1A]">
                    {{ post.anonymous ? '匿名用户' : post.author?.username }}
                  </p>
                  <p class="text-[12px] text-[#8590A6]">{{ post.time }}</p>
                </div>
                <button
                  v-if="showFollowButton"
                  @click="toggleFollow"
                  :class="[
                    'ml-auto px-4 py-1.5 text-[12px] border transition-colors cursor-pointer',
                    socialStore.isFollowing(post.author.id)
                      ? 'text-[#8590A6] border-[#D0D7E2] hover:border-[#1772F6] hover:text-[#1772F6]'
                      : 'bg-[#1772F6] border-[#1772F6] text-white hover:bg-[#0d65e8]'
                  ]"
                >
                  {{ socialStore.isFollowing(post.author.id) ? '已关注' : '关注' }}
                </button>
                <span class="text-[11px] px-2 py-0.5 bg-[#E8F3FF] text-[#1772F6] border border-[#C7DEFF]">
                  {{ categoryLabel[post.category] || '校园墙' }}
                </span>
              </div>

              <div class="text-[15px] text-[#1A1A1A] leading-[1.85] whitespace-pre-wrap mb-5">{{ post.content }}</div>

              <div
                v-if="post.images && post.images.length"
                class="grid gap-1 mb-5"
                :class="post.images.length === 1 ? 'grid-cols-1' : post.images.length === 2 ? 'grid-cols-2' : 'grid-cols-3'"
              >
                <div
                  v-for="(img, i) in post.images"
                  :key="i"
                  class="aspect-video overflow-hidden bg-[#F6F6F6] cursor-pointer"
                  @click="lightboxImg = img"
                >
                  <img :src="img" class="w-full h-full object-cover hover:opacity-90 transition-opacity" />
                </div>
              </div>

              <div class="flex items-center gap-4 pt-4 border-t border-[#EBEBEB]">
                <button
                  @click="toggleLike"
                  :class="['flex items-center gap-1.5 text-[13px] transition-colors cursor-pointer', liked ? 'text-[#1772F6]' : 'text-[#8590A6] hover:text-[#1772F6]']"
                >
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                    <path stroke-linecap="round" stroke-linejoin="round"
                      d="M6.633 10.5c.806 0 1.533-.446 2.031-1.08a9.041 9.041 0 012.861-2.4c.723-.384 1.35-.956 1.653-1.715a4.498 4.498 0 00.322-1.672V3a.75.75 0 01.75-.75A2.25 2.25 0 0116.5 4.5c0 1.152-.26 2.243-.723 3.218-.266.558.107 1.282.725 1.282h3.126c1.026 0 1.945.694 2.054 1.715.045.422.068.85.068 1.285a11.95 11.95 0 01-2.649 7.521c-.388.482-.987.729-1.605.729H13.48c-.483 0-.964-.078-1.423-.23l-3.114-1.04a4.501 4.501 0 00-1.423-.23H5.904" />
                  </svg>
                  赞同 {{ localLikes }}
                </button>
                <button
                  @click="toggleSave"
                  :class="['flex items-center gap-1.5 text-[13px] transition-colors cursor-pointer', saved ? 'text-amber-500' : 'text-[#8590A6] hover:text-amber-500']"
                >
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M17.593 3.322c1.1.128 1.907 1.077 1.907 2.185V21L12 17.25 4.5 21V5.507c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0z" />
                  </svg>
                  {{ saved ? '已收藏' : '收藏' }}
                </button>
                <button class="flex items-center gap-1.5 text-[13px] text-[#8590A6] hover:text-[#1772F6] transition-colors cursor-pointer ml-auto">
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M7.217 10.907a2.25 2.25 0 100 2.186m0-2.186c.18.324.283.696.283 1.093s-.103.77-.283 1.093m0-2.186l9.566-5.314m-9.566 7.5l9.566 5.314m0 0a2.25 2.25 0 103.935 2.186 2.25 2.25 0 00-3.935-2.186zm0-12.814a2.25 2.25 0 103.933-2.185 2.25 2.25 0 00-3.933 2.185z" />
                  </svg>
                  分享
                </button>
              </div>
            </div>
          </div>

          <div class="bg-white border border-[#EBEBEB]">
            <div class="px-6 py-4 border-b border-[#EBEBEB] flex items-center justify-between">
              <h2 class="text-[14px] font-semibold text-[#1A1A1A]">{{ comments.length }} 条评论</h2>
              <span v-if="commentsLoading" class="text-[12px] text-[#8590A6]">加载中…</span>
            </div>

            <p
              v-if="commentError"
              class="mx-6 mt-3 text-[13px] text-amber-800 bg-amber-50 border border-amber-200 px-3 py-2"
            >
              {{ commentError }}
            </p>

            <div class="px-6 py-4 border-b border-[#EBEBEB]">
              <div v-if="!isLoggedIn" class="flex items-center gap-3 py-2">
                <p class="text-[13px] text-[#8590A6]">登录后参与讨论</p>
                <RouterLink to="/userlogin"
                  class="px-4 py-1.5 bg-[#1772F6] text-white text-[13px] hover:bg-[#0d65e8] transition-colors">
                  登录
                </RouterLink>
              </div>
              <div v-else class="flex gap-3">
                <UserAvatar :user="currentUser" :clickable="false" size-class="w-8 h-8" text-class="text-xs" />
                <div class="flex-1">
                  <textarea
                    v-model="newComment"
                    placeholder="写下你的评论…"
                    rows="3"
                    class="w-full px-3 py-2 text-[14px] border border-[#EBEBEB] outline-none focus:border-[#1772F6] resize-none transition-colors"
                  />
                  <div class="flex justify-end mt-2">
                    <button
                      @click="submitComment"
                      :disabled="!newComment.trim() || submittingComment"
                      class="px-5 py-1.5 bg-[#1772F6] text-white text-[13px] hover:bg-[#0d65e8] disabled:opacity-40 cursor-pointer transition-colors"
                    >
                      {{ submittingComment ? '发布中…' : '发布' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="!commentsLoading && comments.length === 0" class="px-6 py-12 text-center text-[13px] text-[#8590A6]">
              暂无评论，来抢沙发吧
            </div>

            <div v-for="comment in comments" :key="comment.id" class="px-6 py-4 border-b border-[#EBEBEB] last:border-b-0">
              <div class="flex gap-3">
                <UserAvatar :user="comment.author" size-class="w-8 h-8" text-class="text-xs" />
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="text-[13px] font-medium text-[#1A1A1A]">{{ comment.author?.username }}</span>
                    <span class="text-[12px] text-[#8590A6]">{{ comment.time }}</span>
                  </div>
                  <p class="text-[14px] text-[#1A1A1A] leading-relaxed">{{ comment.content }}</p>
                  <div class="flex items-center gap-4 mt-2 text-[12px] text-[#8590A6]">
                    <button
                      @click="toggleCommentLike(comment)"
                      :class="['flex items-center gap-1 cursor-pointer transition-colors', comment.liked ? 'text-[#1772F6]' : 'hover:text-[#1772F6]']"
                    >
                      <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6.633 10.5c.806 0 1.533-.446 2.031-1.08a9.041 9.041 0 012.861-2.4c.723-.384 1.35-.956 1.653-1.715a4.498 4.498 0 00.322-1.672V3a.75.75 0 01.75-.75A2.25 2.25 0 0116.5 4.5c0 1.152-.26 2.243-.723 3.218-.266.558.107 1.282.725 1.282h3.126c1.026 0 1.945.694 2.054 1.715.045.422.068.85.068 1.285a11.95 11.95 0 01-2.649 7.521c-.388.482-.987.729-1.605.729H13.48" />
                      </svg>
                      {{ comment.likes }}
                    </button>
                    <button
                      @click="openReplyTo(comment)"
                      class="hover:text-[#1772F6] cursor-pointer transition-colors"
                    >
                      回复
                    </button>
                  </div>

                  <div v-if="replyTarget?.commentId === comment.id" class="mt-3 flex gap-2">
                    <input
                      v-model="replyContent"
                      :placeholder="`回复 ${replyTarget.author}…`"
                      class="flex-1 h-8 px-3 text-[13px] border border-[#EBEBEB] outline-none focus:border-[#1772F6] transition-colors"
                    />
                    <button
                      @click="submitReply"
                      :disabled="!replyContent.trim() || submittingReply"
                      class="px-4 h-8 bg-[#1772F6] text-white text-[12px] hover:bg-[#0d65e8] disabled:opacity-40 cursor-pointer transition-colors"
                    >
                      {{ submittingReply ? '发布中…' : '回复' }}
                    </button>
                    <button
                      @click="replyTarget = null"
                      class="px-3 h-8 border border-[#EBEBEB] text-[#8590A6] text-[12px] hover:bg-[#F6F6F6] cursor-pointer transition-colors"
                    >
                      取消
                    </button>
                  </div>

                  <div v-if="comment.replies && comment.replies.length" class="mt-3 bg-[#F7F8FA] border-l-2 border-[#1772F6] pl-3">
                    <div v-for="reply in comment.replies" :key="reply.id" class="py-3 border-b border-[#EBEBEB] last:border-b-0">
                      <div class="flex items-start gap-2.5">
                        <UserAvatar :user="reply.author" size-class="w-7 h-7" text-class="text-[11px]" />
                        <div class="min-w-0 flex-1">
                          <div class="flex items-center gap-2">
                            <span class="text-[12px] font-medium text-[#1772F6]">{{ reply.author?.username }}</span>
                            <span v-if="reply.replyToUser" class="text-[12px] text-[#8590A6]">
                              回复 <span class="text-[#1772F6]">@{{ reply.replyToUser.username }}</span>
                            </span>
                            <span class="text-[12px] text-[#8590A6]">· {{ reply.time }}</span>
                          </div>
                          <p class="text-[13px] text-[#1A1A1A] mt-0.5">{{ reply.content }}</p>
                          <button
                            v-if="isLoggedIn"
                            @click="openReplyTo(comment, reply)"
                            class="text-[12px] text-[#8590A6] hover:text-[#1772F6] mt-1 cursor-pointer transition-colors"
                          >
                            回复
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </main>

      <div v-if="post" class="hidden lg:flex flex-col gap-2 sticky top-20 shrink-0">
        <button @click="toggleLike"
          :class="['w-12 h-12 flex flex-col items-center justify-center gap-0.5 border transition-colors cursor-pointer text-[11px]',
            liked ? 'bg-[#1772F6] border-[#1772F6] text-white' : 'bg-white border-[#EBEBEB] text-[#8590A6] hover:border-[#1772F6] hover:text-[#1772F6]']">
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6.633 10.5c.806 0 1.533-.446 2.031-1.08a9.041 9.041 0 012.861-2.4c.723-.384 1.35-.956 1.653-1.715a4.498 4.498 0 00.322-1.672V3a.75.75 0 01.75-.75A2.25 2.25 0 0116.5 4.5c0 1.152-.26 2.243-.723 3.218-.266.558.107 1.282.725 1.282h3.126c1.026 0 1.945.694 2.054 1.715.045.422.068.85.068 1.285a11.95 11.95 0 01-2.649 7.521c-.388.482-.987.729-1.605.729H13.48" />
          </svg>
          {{ localLikes }}
        </button>
        <button @click="toggleSave"
          :class="['w-12 h-12 flex flex-col items-center justify-center gap-0.5 border transition-colors cursor-pointer text-[11px]',
            saved ? 'bg-amber-400 border-amber-400 text-white' : 'bg-white border-[#EBEBEB] text-[#8590A6] hover:border-amber-400 hover:text-amber-400']">
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M17.593 3.322c1.1.128 1.907 1.077 1.907 2.185V21L12 17.25 4.5 21V5.507c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0z" />
          </svg>
          收藏
        </button>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="lightboxImg" class="fixed inset-0 bg-black/85 z-50 flex items-center justify-center"
        @click="lightboxImg = null">
        <img :src="lightboxImg" class="max-w-[90vw] max-h-[90vh] object-contain" @click.stop />
        <button @click="lightboxImg = null"
          class="absolute top-4 right-4 w-10 h-10 flex items-center justify-center bg-white/10 text-white hover:bg-white/20 transition-colors cursor-pointer text-xl">
          ✕
        </button>
      </div>
    </Teleport>
  </div>
</template>
