<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSocialStore } from '@/stores/social'
import postApi from '@/api/post'

const router = useRouter()
const userStore = useUserStore()
const socialStore = useSocialStore()
const submitError = ref('')

const socialCategories = [
  { key: 'social_find', label: '捞人' },
  { key: 'social_buddy', label: '找搭子' },
  { key: 'social_love', label: '恋爱' },
]
const otherCategories = [
  { key: 'share', label: '分享墙' },
  { key: 'trade', label: '买卖墙' },
  { key: 'general', label: '综合墙' },
]

const selectedCategory = ref('')
const content = ref('')
const anonymous = ref(false)
const images = ref([])
const video = ref(null)
const submitting = ref(false)
const sensitiveWarning = ref(false)

const charLimit = 2000
const remaining = computed(() => charLimit - content.value.length)

const sensitiveWords = ['作弊', '代考', '违禁', '枪手']

const VIDEO_MAX_BYTES = 100 * 1024 * 1024
const videoError = ref('')

function checkSensitive() {
  sensitiveWarning.value = sensitiveWords.some(w => content.value.includes(w))
}

function handleImageUpload(e) {
  const files = Array.from(e.target.files)
  const remaining = 9 - images.value.length
  files.slice(0, remaining).forEach(file => {
    const reader = new FileReader()
    reader.onload = ev => images.value.push({ url: ev.target.result, file })
    reader.readAsDataURL(file)
  })
}

function removeImage(i) {
  images.value.splice(i, 1)
}

function handleVideoUpload(e) {
  videoError.value = ''
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('video/')) {
    videoError.value = '请选择视频文件'
    e.target.value = ''
    return
  }
  if (file.size > VIDEO_MAX_BYTES) {
    videoError.value = '视频大小不能超过 100MB'
    e.target.value = ''
    return
  }
  if (video.value?.url) {
    URL.revokeObjectURL(video.value.url)
  }
  video.value = { file, url: URL.createObjectURL(file), name: file.name, size: file.size }
}

function removeVideo() {
  if (video.value?.url) {
    URL.revokeObjectURL(video.value.url)
  }
  video.value = null
  videoError.value = ''
}

function formatSize(bytes) {
  if (!Number.isFinite(bytes)) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

async function submit() {
  if (!selectedCategory.value || !content.value.trim()) return
  if (!userStore.isLoggedIn) {
    router.push({ path: '/userlogin', query: { redirect: '/post/create' } })
    return
  }
  submitting.value = true
  submitError.value = ''
  try {
    let res
    const hasImages = images.value.some(x => x.file)
    const hasVideo = !!video.value?.file
    if (hasImages || hasVideo) {
      const formData = new FormData()
      formData.append('category', selectedCategory.value)
      formData.append('content', content.value.trim())
      formData.append('anonymous', String(anonymous.value))
      images.value.forEach(item => {
        if (item.file) {
          formData.append('images', item.file)
        }
      })
      if (hasVideo) {
        formData.append('video', video.value.file)
      }
      res = await postApi.create(formData)
    } else {
      res = await postApi.create({
        category: selectedCategory.value,
        content: content.value.trim(),
        anonymous: anonymous.value,
      })
    }
    if (res?.id) {
      socialStore.upsertPostFromServer(res)
      router.push(`/c/${res.id}`)
    } else {
      router.push('/')
    }
  } catch (e) {
    submitError.value = e?.message || '发布失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-[#F6F6F6]">
    <div class="max-w-[720px] mx-auto px-4 py-5">
      <div class="bg-white border border-[#EBEBEB]">
        <!-- Header -->
        <div class="px-6 py-4 border-b border-[#EBEBEB] flex items-center justify-between">
          <h1 class="text-[16px] font-semibold text-[#1A1A1A]">发布内容</h1>
          <button @click="router.back()"
            class="text-[13px] text-[#8590A6] hover:text-[#1A1A1A] cursor-pointer transition-colors">
            取消
          </button>
        </div>

        <div class="px-6 py-5 space-y-5">
          <p
            v-if="submitError"
            class="text-[13px] text-red-600 bg-red-50 border border-red-100 px-3 py-2"
          >
            {{ submitError }}
          </p>
          <!-- Category -->
          <div>
            <label class="block text-[13px] font-medium text-[#1A1A1A] mb-2">选择分类 <span class="text-red-500">*</span></label>
            <p class="text-[12px] text-[#8590A6] mb-2">社交墙</p>
            <div class="flex gap-2 flex-wrap mb-4">
              <button
                v-for="cat in socialCategories"
                :key="cat.key"
                @click="selectedCategory = cat.key"
                :class="[
                  'px-4 py-1.5 text-[13px] border transition-colors cursor-pointer',
                  selectedCategory === cat.key
                    ? 'bg-[#1772F6] text-white border-[#1772F6]'
                    : 'bg-white text-[#444] border-[#EBEBEB] hover:border-[#1772F6] hover:text-[#1772F6]'
                ]"
              >{{ cat.label }}</button>
            </div>
            <p class="text-[12px] text-[#8590A6] mb-2">其他板块</p>
            <div class="flex gap-2 flex-wrap">
              <button
                v-for="cat in otherCategories"
                :key="cat.key"
                @click="selectedCategory = cat.key"
                :class="[
                  'px-4 py-1.5 text-[13px] border transition-colors cursor-pointer',
                  selectedCategory === cat.key
                    ? 'bg-[#1772F6] text-white border-[#1772F6]'
                    : 'bg-white text-[#444] border-[#EBEBEB] hover:border-[#1772F6] hover:text-[#1772F6]'
                ]"
              >{{ cat.label }}</button>
            </div>
          </div>

          <!-- Content -->
          <div>
            <label class="block text-[13px] font-medium text-[#1A1A1A] mb-2">内容 <span class="text-red-500">*</span></label>
            <textarea
              v-model="content"
              @input="checkSensitive"
              :maxlength="charLimit"
              placeholder="分享你的校园动态、寻人启事、二手转让或任何想说的……"
              rows="8"
              class="w-full px-4 py-3 text-[14px] text-[#1A1A1A] border border-[#EBEBEB] bg-white resize-none outline-none focus:border-[#1772F6] transition-colors leading-relaxed"
            />
            <div class="flex items-center justify-between mt-1.5">
              <p v-if="sensitiveWarning" class="text-[12px] text-amber-600">
                内容包含敏感词，请修改后再发布
              </p>
              <span v-else />
              <span :class="['text-[12px]', remaining < 100 ? 'text-red-500' : 'text-[#8590A6]']">
                {{ remaining }}/{{ charLimit }}
              </span>
            </div>
          </div>

          <!-- Images -->
          <div>
            <label class="block text-[13px] font-medium text-[#1A1A1A] mb-2">图片（最多9张）</label>
            <div class="flex flex-wrap gap-2">
              <div
                v-for="(img, i) in images"
                :key="i"
                class="relative w-20 h-20 border border-[#EBEBEB] overflow-hidden group"
              >
                <img :src="img.url" class="w-full h-full object-cover" />
                <button @click="removeImage(i)"
                  class="absolute top-0.5 right-0.5 w-5 h-5 bg-black/60 text-white text-[10px] flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
                  ✕
                </button>
              </div>
              <label
                v-if="images.length < 9"
                class="w-20 h-20 border border-dashed border-[#EBEBEB] flex flex-col items-center justify-center cursor-pointer hover:border-[#1772F6] hover:bg-[#F0F7FF] transition-colors text-[#8590A6] hover:text-[#1772F6]"
              >
                <svg class="w-6 h-6 mb-1" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 4v16m8-8H4" />
                </svg>
                <span class="text-[11px]">添加图片</span>
                <input type="file" accept="image/*" multiple class="hidden" @change="handleImageUpload" />
              </label>
            </div>
          </div>

          <!-- Video -->
          <div>
            <label class="block text-[13px] font-medium text-[#1A1A1A] mb-2">视频（可选，最大 100MB）</label>
            <div v-if="video" class="border border-[#EBEBEB] bg-[#FAFAFA] p-3 flex items-center gap-3">
              <video :src="video.url" class="w-28 h-20 bg-black object-cover" muted />
              <div class="flex-1 min-w-0">
                <p class="text-[13px] text-[#1A1A1A] truncate">{{ video.name }}</p>
                <p class="text-[12px] text-[#8590A6] mt-0.5">{{ formatSize(video.size) }}</p>
              </div>
              <button
                @click="removeVideo"
                class="text-[12px] text-[#8590A6] hover:text-red-500 cursor-pointer transition-colors"
              >
                移除
              </button>
            </div>
            <label
              v-else
              class="inline-flex items-center gap-2 px-4 py-2 text-[13px] text-[#444] border border-dashed border-[#EBEBEB] hover:border-[#1772F6] hover:text-[#1772F6] cursor-pointer transition-colors"
            >
              <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 10.5l4.72-4.72a.75.75 0 011.28.53v11.38a.75.75 0 01-1.28.53l-4.72-4.72M4.5 18.75h9a2.25 2.25 0 002.25-2.25v-9a2.25 2.25 0 00-2.25-2.25h-9A2.25 2.25 0 002.25 7.5v9a2.25 2.25 0 002.25 2.25z" />
              </svg>
              <span>上传视频</span>
              <input type="file" accept="video/*" class="hidden" @change="handleVideoUpload" />
            </label>
            <p v-if="videoError" class="text-[12px] text-red-500 mt-1.5">{{ videoError }}</p>
          </div>

          <!-- Anonymous toggle -->
          <div class="flex items-center justify-between py-3 border-t border-[#EBEBEB]">
            <div>
              <p class="text-[13px] font-medium text-[#1A1A1A]">匿名发布</p>
              <p class="text-[12px] text-[#8590A6] mt-0.5">隐藏你的用户名，以「匿名用户」显示</p>
            </div>
            <button
              @click="anonymous = !anonymous"
              :class="['relative w-10 h-6 transition-colors cursor-pointer shrink-0', anonymous ? 'bg-[#1772F6]' : 'bg-[#EBEBEB]']"
            >
              <span :class="['absolute top-1 w-4 h-4 bg-white shadow transition-all', anonymous ? 'left-5' : 'left-1']" />
            </button>
          </div>
        </div>

        <!-- Submit -->
        <div class="px-6 py-4 border-t border-[#EBEBEB] flex items-center justify-end gap-3">
          <button @click="router.back()"
            class="px-6 py-2 text-[13px] border border-[#EBEBEB] text-[#444] hover:bg-[#F6F6F6] transition-colors cursor-pointer">
            取消
          </button>
          <button
            @click="submit"
            :disabled="!selectedCategory || !content.trim() || submitting || sensitiveWarning"
            class="px-8 py-2 text-[13px] font-medium bg-[#1772F6] text-white hover:bg-[#0d65e8] disabled:opacity-40 disabled:cursor-not-allowed transition-colors cursor-pointer"
          >
            {{ submitting ? '发布中…' : '发布' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
