<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSocialStore } from '@/stores/social'
import userApi from '@/api/user'
import UserAvatar from '@/components/UserAvatar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const socialStore = useSocialStore()

const activeTab = ref('profile')
const profileMsg = ref('')
const pwdMsg = ref('')
const avatarInput = ref(null)
const loadingUser = ref(false)
const loadError = ref('')

const pwdForm = ref({ old: '', new_: '', confirm: '' })
const profileForm = ref({ username: '', phone: '', email: '', gender: '保密', bio: '' })

const categoryLabel = {
  social_find: '捞人',
  social_buddy: '找搭子',
  social_love: '恋爱',
  share: '分享墙',
  trade: '买卖墙',
  general: '综合墙',
}

const viewedUserId = computed(() => Number(route.params.id || socialStore.currentUserId))
const viewedUser = computed(() => socialStore.findUserById(viewedUserId.value))
const isSelf = computed(() => viewedUser.value?.id === socialStore.currentUserId)
const stats = computed(() => socialStore.getUserStats(viewedUserId.value))
const userPosts = computed(() => socialStore.getUserPosts(viewedUserId.value))
const userComments = computed(() => socialStore.getUserComments(viewedUserId.value))
const favoritePosts = computed(() => (isSelf.value ? socialStore.getFavoritePosts(viewedUserId.value) : []))
const followingUsers = computed(() => socialStore.getFollowing(viewedUserId.value))
const followerUsers = computed(() => socialStore.getFollowers(viewedUserId.value))
const isFollowingUser = computed(() => !isSelf.value && socialStore.isFollowing(viewedUserId.value))

const tabs = computed(() => {
  if (isSelf.value) {
    return [
      { key: 'profile', label: '基本资料' },
      { key: 'password', label: '修改密码' },
      { key: 'posts', label: '我的发布' },
      { key: 'comments', label: '我的评论' },
      { key: 'favorites', label: '我的收藏' },
      { key: 'following', label: '我的关注' },
      { key: 'followers', label: '我的粉丝' },
    ]
  }
  return [
    { key: 'profile', label: 'TA的资料' },
    { key: 'posts', label: 'TA的发布' },
    { key: 'comments', label: 'TA的评论' },
    { key: 'following', label: 'TA的关注' },
    { key: 'followers', label: 'TA的粉丝' },
  ]
})

function syncProfileForm() {
  const user = viewedUser.value
  if (!user) return
  profileForm.value = {
    username: user.username || '',
    phone: user.phone || '',
    email: user.email || '',
    gender: user.gender || '保密',
    bio: user.bio || '',
  }
}

watch(() => route.fullPath, async () => {
  activeTab.value = 'profile'
  profileMsg.value = ''
  pwdMsg.value = ''
  loadError.value = ''
  await loadViewedUser()
  syncProfileForm()
}, { immediate: true })

async function loadViewedUser() {
  const id = viewedUserId.value
  if (!id) return
  loadingUser.value = true
  try {
    const user = route.params.id
      ? await userApi.getUserDetail(id)
      : await userApi.getCurrentUserDetail()
    if (user) {
      socialStore.syncUserFromProfile(user)
      if (!route.params.id) {
        userStore.updateUserInfo(user)
      }
    }
  } catch (e) {
    loadError.value = e?.message || '用户资料加载失败'
  } finally {
    loadingUser.value = false
  }
}

async function saveProfile() {
  if (!isSelf.value) return
  profileMsg.value = ''
  try {
    const user = await userApi.updateProfile(profileForm.value)
    userStore.updateUserInfo(user)
    socialStore.syncUserFromProfile(user)
    syncProfileForm()
    profileMsg.value = '资料已保存'
  } catch (e) {
    profileMsg.value = e?.message || '资料保存失败'
  }
}

async function changePwd() {
  if (!pwdForm.value.old || !pwdForm.value.new_ || !pwdForm.value.confirm) {
    pwdMsg.value = '请填写所有字段'
    return
  }
  if (pwdForm.value.new_ !== pwdForm.value.confirm) {
    pwdMsg.value = '两次密码不一致'
    return
  }
  try {
    await userApi.changePassword({
      oldPassword: pwdForm.value.old,
      newPassword: pwdForm.value.new_,
    })
    pwdMsg.value = '密码修改成功'
    pwdForm.value = { old: '', new_: '', confirm: '' }
  } catch (e) {
    pwdMsg.value = e?.message || '密码修改失败'
  }
}

async function handleAvatarChange(event) {
  const file = event.target.files?.[0]
  if (!file || !isSelf.value) return
  try {
    const formData = new FormData()
    formData.append('avatar', file)
    const res = await userApi.uploadAvatar(formData)
    const avatar = res?.avatarUrl || res?.avatar
    if (!avatar) throw new Error('头像响应缺少 avatarUrl')
    userStore.updateUserInfo({ avatar })
    socialStore.updateCurrentUserProfile({ avatar })
    profileMsg.value = '头像已更新'
  } catch (e) {
    profileMsg.value = e?.message || '头像更新失败'
  }
  event.target.value = ''
}

function toggleFollow(userId = viewedUserId.value) {
  socialStore.toggleFollow(userId)
}

function openUserProfile(userId) {
  router.push(`/users/${userId}`)
}

function userCardStats(userId) {
  return socialStore.getUserStats(userId)
}
</script>

<template>
  <div class="min-h-screen bg-[#F6F6F6]">
    <div class="max-w-[980px] mx-auto px-4 py-5">
      <div v-if="loadingUser" class="bg-white border border-[#EBEBEB] px-6 py-10 text-center">
        <p class="text-[15px] text-[#1A1A1A] mb-3">用户资料加载中...</p>
      </div>

      <div v-else-if="loadError" class="bg-white border border-[#EBEBEB] px-6 py-10 text-center">
        <p class="text-[15px] text-[#1A1A1A] mb-3">{{ loadError }}</p>
        <button
          @click="loadViewedUser"
          class="px-5 py-2 bg-[#1772F6] text-white text-[13px] hover:bg-[#0d65e8] transition-colors cursor-pointer"
        >
          重新加载
        </button>
      </div>

      <div v-else-if="!viewedUser" class="bg-white border border-[#EBEBEB] px-6 py-10 text-center">
        <p class="text-[15px] text-[#1A1A1A] mb-3">用户不存在</p>
        <button
          @click="router.push('/')"
          class="px-5 py-2 bg-[#1772F6] text-white text-[13px] hover:bg-[#0d65e8] transition-colors cursor-pointer"
        >
          返回首页
        </button>
      </div>

      <template v-else>
        <div class="bg-white border border-[#EBEBEB] mb-3 px-6 py-5 flex items-start gap-4">
          <div class="relative">
            <UserAvatar
              :user="viewedUser"
              :clickable="false"
              size-class="w-20 h-20"
              text-class="text-xl"
            />
            <button
              v-if="isSelf"
              @click="avatarInput?.click()"
              class="absolute -bottom-2 left-1/2 -translate-x-1/2 px-3 py-1 bg-[#1772F6] text-white text-[11px] whitespace-nowrap rounded-full cursor-pointer hover:bg-[#0d65e8] transition-colors"
            >
              更换头像
            </button>
            <input
              ref="avatarInput"
              type="file"
              accept="image/*"
              class="hidden"
              @change="handleAvatarChange"
            />
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0">
                <h1 class="text-[18px] font-semibold text-[#1A1A1A]">{{ viewedUser.username }}</h1>
                <p class="text-[13px] text-[#8590A6] mt-1">{{ viewedUser.bio || '这个人很低调，还没有填写简介。' }}</p>
                <p class="text-[12px] text-[#A0A8B8] mt-2">
                  {{ viewedUser.gender || '保密' }} · {{ viewedUser.major }} · 加入于 {{ viewedUser.joinedAt }}
                </p>
              </div>
              <button
                v-if="!isSelf"
                @click="toggleFollow()"
                :class="[
                  'shrink-0 px-5 py-2 text-[13px] border transition-colors cursor-pointer',
                  isFollowingUser
                    ? 'text-[#8590A6] border-[#D0D7E2] hover:border-[#1772F6] hover:text-[#1772F6]'
                    : 'bg-[#1772F6] border-[#1772F6] text-white hover:bg-[#0d65e8]'
                ]"
              >
                {{ isFollowingUser ? '已关注' : '关注 TA' }}
              </button>
            </div>

            <div class="grid grid-cols-4 gap-3 mt-5">
              <div class="bg-[#F7F8FA] px-4 py-3">
                <p class="text-[12px] text-[#8590A6]">发布</p>
                <p class="text-[18px] font-semibold text-[#1A1A1A] mt-1">{{ stats.postCount }}</p>
              </div>
              <div class="bg-[#F7F8FA] px-4 py-3">
                <p class="text-[12px] text-[#8590A6]">{{ isSelf ? '我的关注' : 'TA的关注' }}</p>
                <p class="text-[18px] font-semibold text-[#1A1A1A] mt-1">{{ stats.followingCount }}</p>
              </div>
              <div class="bg-[#F7F8FA] px-4 py-3">
                <p class="text-[12px] text-[#8590A6]">{{ isSelf ? '我的粉丝' : 'TA的粉丝' }}</p>
                <p class="text-[18px] font-semibold text-[#1A1A1A] mt-1">{{ stats.followerCount }}</p>
              </div>
              <div class="bg-[#F7F8FA] px-4 py-3">
                <p class="text-[12px] text-[#8590A6]">收藏</p>
                <p class="text-[18px] font-semibold text-[#1A1A1A] mt-1">{{ stats.favoriteCount }}</p>
              </div>
            </div>
          </div>
        </div>

        <div class="flex gap-4 items-start">
          <nav class="w-44 bg-white border border-[#EBEBEB] shrink-0">
            <button
              v-for="tab in tabs"
              :key="tab.key"
              @click="activeTab = tab.key"
              :class="[
                'w-full text-left px-4 py-3 text-[14px] border-b border-[#EBEBEB] last:border-b-0 transition-colors cursor-pointer',
                activeTab === tab.key
                  ? 'text-[#1772F6] bg-[#E8F3FF] font-medium border-l-2 border-l-[#1772F6] pl-[14px]'
                  : 'text-[#444] hover:bg-[#F6F6F6]'
              ]"
            >
              {{ tab.label }}
            </button>
          </nav>

          <div class="flex-1 min-w-0 bg-white border border-[#EBEBEB]">
            <div v-if="activeTab === 'profile'" class="px-6 py-6">
              <h2 class="text-[14px] font-semibold text-[#1A1A1A] mb-5">{{ isSelf ? '基本资料' : '公开资料' }}</h2>

              <div v-if="profileMsg" class="mb-4 text-[13px] px-3 py-2 border border-emerald-100 text-emerald-600 bg-emerald-50">
                {{ profileMsg }}
              </div>

              <div v-if="isSelf" class="space-y-4 max-w-[460px]">
                <div>
                  <label class="block text-[13px] text-[#8590A6] mb-1.5">用户名</label>
                  <input
                    v-model="profileForm.username"
                    type="text"
                    class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] outline-none focus:border-[#1772F6] transition-colors"
                  />
                </div>
                <div>
                  <label class="block text-[13px] text-[#8590A6] mb-1.5">手机号</label>
                  <input
                    v-model="profileForm.phone"
                    type="text"
                    class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] outline-none focus:border-[#1772F6] transition-colors"
                  />
                </div>
                <div>
                  <label class="block text-[13px] text-[#8590A6] mb-1.5">性别</label>
                  <select
                    v-model="profileForm.gender"
                    class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] outline-none focus:border-[#1772F6] transition-colors bg-white cursor-pointer"
                  >
                    <option value="男">男</option>
                    <option value="女">女</option>
                    <option value="保密">保密</option>
                  </select>
                </div>
                <div>
                  <label class="block text-[13px] text-[#8590A6] mb-1.5">邮箱</label>
                  <input
                    v-model="profileForm.email"
                    type="email"
                    class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] outline-none focus:border-[#1772F6] transition-colors"
                  />
                </div>
                <div>
                  <label class="block text-[13px] text-[#8590A6] mb-1.5">个人简介</label>
                  <textarea
                    v-model="profileForm.bio"
                    rows="4"
                    class="w-full px-3 py-2 text-[14px] border border-[#EBEBEB] outline-none focus:border-[#1772F6] resize-none transition-colors"
                  />
                </div>
                <button
                  @click="saveProfile"
                  class="px-8 py-2 bg-[#1772F6] text-white text-[13px] hover:bg-[#0d65e8] transition-colors cursor-pointer"
                >
                  保存修改
                </button>
              </div>

              <div v-else class="grid md:grid-cols-2 gap-4">
                <div class="bg-[#F7F8FA] px-4 py-4">
                  <p class="text-[12px] text-[#8590A6]">用户名</p>
                  <p class="text-[14px] text-[#1A1A1A] mt-1">{{ viewedUser.username }}</p>
                </div>
                <div class="bg-[#F7F8FA] px-4 py-4">
                  <p class="text-[12px] text-[#8590A6]">性别</p>
                  <p class="text-[14px] text-[#1A1A1A] mt-1">{{ viewedUser.gender || '保密' }}</p>
                </div>
                <div class="bg-[#F7F8FA] px-4 py-4">
                  <p class="text-[12px] text-[#8590A6]">专业</p>
                  <p class="text-[14px] text-[#1A1A1A] mt-1">{{ viewedUser.major }}</p>
                </div>
                <div class="bg-[#F7F8FA] px-4 py-4">
                  <p class="text-[12px] text-[#8590A6]">加入时间</p>
                  <p class="text-[14px] text-[#1A1A1A] mt-1">{{ viewedUser.joinedAt }}</p>
                </div>
                <div class="bg-[#F7F8FA] px-4 py-4 md:col-span-2">
                  <p class="text-[12px] text-[#8590A6]">个人简介</p>
                  <p class="text-[14px] text-[#1A1A1A] mt-1 leading-relaxed">{{ viewedUser.bio || '暂无简介' }}</p>
                </div>
              </div>
            </div>

            <div v-else-if="activeTab === 'password'" class="px-6 py-6">
              <h2 class="text-[14px] font-semibold text-[#1A1A1A] mb-5">修改密码</h2>
              <div class="space-y-4 max-w-[440px]">
                <div v-if="pwdMsg" :class="['text-[13px] px-3 py-2 border', pwdMsg.includes('成功') ? 'text-emerald-600 bg-emerald-50 border-emerald-100' : 'text-red-500 bg-red-50 border-red-100']">
                  {{ pwdMsg }}
                </div>
                <div>
                  <label class="block text-[13px] text-[#8590A6] mb-1.5">当前密码</label>
                  <input v-model="pwdForm.old" type="password"
                    class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] outline-none focus:border-[#1772F6] transition-colors" />
                </div>
                <div>
                  <label class="block text-[13px] text-[#8590A6] mb-1.5">新密码</label>
                  <input v-model="pwdForm.new_" type="password"
                    class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] outline-none focus:border-[#1772F6] transition-colors" />
                </div>
                <div>
                  <label class="block text-[13px] text-[#8590A6] mb-1.5">确认新密码</label>
                  <input v-model="pwdForm.confirm" type="password"
                    class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] outline-none focus:border-[#1772F6] transition-colors" />
                </div>
                <button @click="changePwd"
                  class="px-8 py-2 bg-[#1772F6] text-white text-[13px] hover:bg-[#0d65e8] transition-colors cursor-pointer">
                  确认修改
                </button>
              </div>
            </div>

            <div v-else-if="activeTab === 'posts'">
              <div class="px-6 py-4 border-b border-[#EBEBEB]">
                <h2 class="text-[14px] font-semibold text-[#1A1A1A]">{{ isSelf ? '我的发布' : 'TA的发布' }}（{{ userPosts.length }}）</h2>
              </div>
              <div v-if="userPosts.length === 0" class="py-16 flex flex-col items-center gap-3 text-[#8590A6]">
                <p class="text-[14px]">暂无发布</p>
              </div>
              <div v-else>
                <div
                  v-for="post in userPosts"
                  :key="post.id"
                  @click="$router.push(`/c/${post.id}`)"
                  class="px-6 py-4 border-b border-[#EBEBEB] last:border-b-0 cursor-pointer hover:bg-[#FAFAFA] transition-colors"
                >
                  <div class="flex items-center gap-2 mb-1.5">
                    <span class="text-[11px] px-1.5 py-0.5 bg-[#E8F3FF] text-[#1772F6] border border-[#C7DEFF]">{{ categoryLabel[post.category] }}</span>
                    <span class="text-[12px] text-[#8590A6]">{{ post.time }}</span>
                  </div>
                  <p class="text-[14px] text-[#1A1A1A] line-clamp-2 mb-2">{{ post.content }}</p>
                  <div class="flex items-center gap-4 text-[12px] text-[#8590A6]">
                    <span>赞同 {{ post.likes }}</span>
                    <span>{{ post.comments }} 评论</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-else-if="activeTab === 'comments'">
              <div class="px-6 py-4 border-b border-[#EBEBEB]">
                <h2 class="text-[14px] font-semibold text-[#1A1A1A]">{{ isSelf ? '我的评论' : 'TA的评论' }}（{{ userComments.length }}）</h2>
              </div>
              <div v-if="userComments.length === 0" class="py-16 flex flex-col items-center gap-3 text-[#8590A6]">
                <p class="text-[14px]">暂无评论</p>
              </div>
              <div v-else>
                <div
                  v-for="comment in userComments"
                  :key="comment.id"
                  @click="$router.push(`/c/${comment.postId}`)"
                  class="px-6 py-4 border-b border-[#EBEBEB] last:border-b-0 cursor-pointer hover:bg-[#FAFAFA] transition-colors"
                >
                  <p class="text-[12px] text-[#8590A6] mb-1 truncate">评论了：{{ comment.postTitle }}</p>
                  <p class="text-[14px] text-[#1A1A1A]">{{ comment.content }}</p>
                  <p class="text-[12px] text-[#8590A6] mt-1">{{ comment.time }}</p>
                </div>
              </div>
            </div>

            <div v-else-if="activeTab === 'favorites'">
              <div class="px-6 py-4 border-b border-[#EBEBEB]">
                <h2 class="text-[14px] font-semibold text-[#1A1A1A]">我的收藏（{{ favoritePosts.length }}）</h2>
              </div>
              <div v-if="favoritePosts.length === 0" class="py-16 flex flex-col items-center gap-3 text-[#8590A6]">
                <p class="text-[14px]">暂无收藏</p>
              </div>
              <div v-else>
                <div
                  v-for="post in favoritePosts"
                  :key="post.id"
                  @click="$router.push(`/c/${post.id}`)"
                  class="px-6 py-4 border-b border-[#EBEBEB] last:border-b-0 cursor-pointer hover:bg-[#FAFAFA] transition-colors"
                >
                  <div class="flex items-center gap-2 mb-1.5">
                    <span class="text-[11px] px-1.5 py-0.5 bg-[#E8F3FF] text-[#1772F6] border border-[#C7DEFF]">{{ categoryLabel[post.category] }}</span>
                    <span class="text-[12px] text-[#8590A6]">{{ post.time }}</span>
                  </div>
                  <p class="text-[14px] text-[#1A1A1A] line-clamp-2 mb-2">{{ post.content }}</p>
                  <div class="text-[12px] text-[#8590A6]">赞同 {{ post.likes }}</div>
                </div>
              </div>
            </div>

            <div v-else-if="activeTab === 'following'">
              <div class="px-6 py-4 border-b border-[#EBEBEB]">
                <h2 class="text-[14px] font-semibold text-[#1A1A1A]">{{ isSelf ? '我的关注' : 'TA的关注' }}（{{ followingUsers.length }}）</h2>
              </div>
              <div v-if="followingUsers.length === 0" class="py-16 flex flex-col items-center gap-3 text-[#8590A6]">
                <p class="text-[14px]">暂无关注</p>
              </div>
              <div v-else class="divide-y divide-[#EBEBEB]">
                <div v-for="user in followingUsers" :key="user.id" class="px-6 py-4 flex items-center gap-3">
                  <UserAvatar :user="user" size-class="w-11 h-11" text-class="text-sm" />
                  <div class="flex-1 min-w-0 cursor-pointer" @click="openUserProfile(user.id)">
                    <p class="text-[14px] font-medium text-[#1A1A1A]">{{ user.username }}</p>
                    <p class="text-[12px] text-[#8590A6] mt-0.5 truncate">{{ user.bio }}</p>
                    <p class="text-[12px] text-[#A0A8B8] mt-1">粉丝 {{ userCardStats(user.id).followerCount }} · 发布 {{ userCardStats(user.id).postCount }}</p>
                  </div>
                  <button
                    v-if="user.id !== socialStore.currentUserId"
                    @click="toggleFollow(user.id)"
                    :class="[
                      'px-4 py-1.5 text-[12px] border transition-colors cursor-pointer',
                      socialStore.isFollowing(user.id)
                        ? 'text-[#8590A6] border-[#D0D7E2] hover:border-[#1772F6] hover:text-[#1772F6]'
                        : 'bg-[#1772F6] border-[#1772F6] text-white hover:bg-[#0d65e8]'
                    ]"
                  >
                    {{ socialStore.isFollowing(user.id) ? '已关注' : '关注' }}
                  </button>
                </div>
              </div>
            </div>

            <div v-else-if="activeTab === 'followers'">
              <div class="px-6 py-4 border-b border-[#EBEBEB]">
                <h2 class="text-[14px] font-semibold text-[#1A1A1A]">{{ isSelf ? '我的粉丝' : 'TA的粉丝' }}（{{ followerUsers.length }}）</h2>
              </div>
              <div v-if="followerUsers.length === 0" class="py-16 flex flex-col items-center gap-3 text-[#8590A6]">
                <p class="text-[14px]">暂无粉丝</p>
              </div>
              <div v-else class="divide-y divide-[#EBEBEB]">
                <div v-for="user in followerUsers" :key="user.id" class="px-6 py-4 flex items-center gap-3">
                  <UserAvatar :user="user" size-class="w-11 h-11" text-class="text-sm" />
                  <div class="flex-1 min-w-0 cursor-pointer" @click="openUserProfile(user.id)">
                    <p class="text-[14px] font-medium text-[#1A1A1A]">{{ user.username }}</p>
                    <p class="text-[12px] text-[#8590A6] mt-0.5 truncate">{{ user.bio }}</p>
                    <p class="text-[12px] text-[#A0A8B8] mt-1">粉丝 {{ userCardStats(user.id).followerCount }} · 发布 {{ userCardStats(user.id).postCount }}</p>
                  </div>
                  <button
                    v-if="user.id !== socialStore.currentUserId"
                    @click="toggleFollow(user.id)"
                    :class="[
                      'px-4 py-1.5 text-[12px] border transition-colors cursor-pointer',
                      socialStore.isFollowing(user.id)
                        ? 'text-[#8590A6] border-[#D0D7E2] hover:border-[#1772F6] hover:text-[#1772F6]'
                        : 'bg-[#1772F6] border-[#1772F6] text-white hover:bg-[#0d65e8]'
                    ]"
                  >
                    {{ socialStore.isFollowing(user.id) ? '已关注' : '回关' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
