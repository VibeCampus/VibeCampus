<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSocialStore } from '@/stores/social'
import authApi from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const socialStore = useSocialStore()

const form = ref({ account: '', password: '', captcha: '' })
const captchaId = ref('')
const captchaImage = ref('')
const captchaFallback = ref('')
const showPwd = ref(false)
const loading = ref(false)
const error = ref('')

function generateLocalCaptcha() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  return Array.from({ length: 4 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

async function refreshCaptcha() {
  try {
    const res = await authApi.getCaptcha()
    if (res?.captchaId && res?.image) {
      captchaId.value = res.captchaId
      captchaImage.value = res.image.startsWith('data:') ? res.image : `data:image/png;base64,${res.image}`
      captchaFallback.value = ''
      return
    }
  } catch {
    // 后端验证码接口不可用时，使用本地前端验证码
  }
  captchaId.value = ''
  captchaImage.value = ''
  captchaFallback.value = generateLocalCaptcha()
}

onMounted(refreshCaptcha)

async function submit() {
  if (!form.value.account || !form.value.password || !form.value.captcha) {
    error.value = '请填写所有字段'
    return
  }
  if (captchaFallback.value && form.value.captcha.toUpperCase() !== captchaFallback.value) {
    error.value = '验证码错误'
    refreshCaptcha()
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await authApi.login({
      account: form.value.account,
      password: form.value.password,
      captcha: form.value.captcha,
      captchaId: captchaId.value,
    })
    userStore.login(data)
    if (data.user) {
      socialStore.syncUserFromProfile(data.user)
    }
    router.push('/')
  } catch (e) {
    error.value = e?.message || '登录失败'
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-[#F6F6F6] flex items-center justify-center py-12">
    <div class="w-full max-w-[360px] mx-4">
      <!-- Logo -->
      <div class="flex flex-col items-center mb-8">
        <div class="w-10 h-10 bg-[#1772F6] flex items-center justify-center mb-3">
          <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 12h6" />
          </svg>
        </div>
        <h1 class="text-[20px] font-bold text-[#1A1A1A]">校园信息墙</h1>
        <p class="text-[13px] text-[#8590A6] mt-1">登录你的账号</p>
      </div>

      <!-- Card -->
      <div class="bg-white border border-[#EBEBEB]">
        <div class="px-8 py-8 space-y-4">
          <div v-if="error" class="text-[13px] text-red-500 bg-red-50 border border-red-100 px-3 py-2">
            {{ error }}
          </div>

          <div>
            <label class="block text-[13px] text-[#1A1A1A] mb-1.5">账号</label>
            <input v-model="form.account" type="text" placeholder="用户名 / 手机号"
              class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] bg-white outline-none focus:border-[#1772F6] transition-colors" />
          </div>

          <div>
            <label class="block text-[13px] text-[#1A1A1A] mb-1.5">密码</label>
            <div class="relative">
              <input v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="请输入密码"
                class="w-full h-10 px-3 pr-10 text-[14px] border border-[#EBEBEB] bg-white outline-none focus:border-[#1772F6] transition-colors" />
              <button @click="showPwd = !showPwd" type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-[#8590A6] hover:text-[#444] cursor-pointer">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path v-if="showPwd" stroke-linecap="round" stroke-linejoin="round"
                    d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                  <path v-else stroke-linecap="round" stroke-linejoin="round"
                    d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                </svg>
              </button>
            </div>
          </div>

          <div>
            <label class="block text-[13px] text-[#1A1A1A] mb-1.5">验证码</label>
            <div class="flex gap-2">
              <input v-model="form.captcha" type="text" placeholder="图形验证码" maxlength="4"
                class="flex-1 h-10 px-3 text-[14px] border border-[#EBEBEB] bg-white outline-none focus:border-[#1772F6] transition-colors tracking-widest" />
              <!-- 后端验证码图片 -->
              <img
                v-if="captchaImage"
                :src="captchaImage"
                @click="refreshCaptcha"
                class="h-10 cursor-pointer hover:opacity-80 transition-opacity shrink-0"
                alt="验证码"
              />
              <!-- 前端本地验证码（后端不可用时降级） -->
              <div
                v-else
                @click="refreshCaptcha"
                class="h-10 px-4 flex items-center justify-center bg-[#F6F6F6] border border-[#EBEBEB] cursor-pointer hover:bg-[#EBEBEB] select-none font-mono font-bold text-[16px] text-[#1772F6] tracking-widest transition-colors shrink-0"
              >
                {{ captchaFallback }}
              </div>
            </div>
          </div>

          <div class="flex items-center justify-end">
            <RouterLink to="/forgot-password"
              class="text-[12px] text-[#8590A6] hover:text-[#1772F6] transition-colors">
              忘记密码？
            </RouterLink>
          </div>

          <button @click="submit" :disabled="loading"
            class="w-full h-10 bg-[#1772F6] text-white font-medium text-[14px] hover:bg-[#0d65e8] disabled:opacity-60 transition-colors cursor-pointer">
            {{ loading ? '登录中…' : '登录' }}
          </button>
        </div>

        <div class="border-t border-[#EBEBEB] px-8 py-4 text-center text-[13px] text-[#8590A6]">
          还没有账号？
          <RouterLink to="/register" class="text-[#1772F6] hover:underline">立即注册</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>
