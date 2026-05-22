<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useSocialStore } from '@/stores/social'
import authApi from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const socialStore = useSocialStore()
const form = ref({ username: '', phone: '', gender: '保密', password: '', confirm: '', captcha: '' })
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
    // fallback
  }
  captchaId.value = ''
  captchaImage.value = ''
  captchaFallback.value = generateLocalCaptcha()
}

onMounted(refreshCaptcha)

const strength = computed(() => {
  const p = form.value.password
  if (!p) return 0
  let s = 0
  if (p.length >= 8) s++
  if (/[A-Z]/.test(p)) s++
  if (/[0-9]/.test(p)) s++
  if (/[^A-Za-z0-9]/.test(p)) s++
  return s
})

const strengthLabel = computed(() => ['', '弱', '中等', '强', '很强'][strength.value])
const strengthColor = computed(() => ['', 'bg-red-400', 'bg-amber-400', 'bg-emerald-400', 'bg-emerald-500'][strength.value])

async function submit() {
  error.value = ''
  if (!form.value.username || !form.value.phone || !form.value.password || !form.value.confirm || !form.value.captcha) {
    error.value = '请填写所有字段'
    return
  }
  if (form.value.password !== form.value.confirm) {
    error.value = '两次密码不一致'
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
    const res = await authApi.register({
      username: form.value.username,
      password: form.value.password,
      phone: form.value.phone,
      gender: form.value.gender,
      captcha: form.value.captcha,
      captchaId: captchaId.value,
    })
    if (res?.token) {
      userStore.login({ token: res.token, user: res.user || { username: form.value.username } })
      if (!res.user) {
        await userStore.refreshFromServer()
      }
      if (userStore.userInfo) {
        socialStore.syncUserFromProfile(userStore.userInfo)
      }
      router.push('/')
    } else {
      router.push({ path: '/userlogin', query: { from: 'register' } })
    }
  } catch (e) {
    error.value = e?.message || '注册失败'
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-[#F6F6F6] flex items-center justify-center py-12">
    <div class="w-full max-w-[380px] mx-4">
      <div class="flex flex-col items-center mb-8">
        <div class="w-10 h-10 bg-[#1772F6] flex items-center justify-center mb-3">
          <svg class="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 12h6" />
          </svg>
        </div>
        <h1 class="text-[20px] font-bold text-[#1A1A1A]">创建账号</h1>
        <p class="text-[13px] text-[#8590A6] mt-1">加入校园信息墙</p>
      </div>

      <div class="bg-white border border-[#EBEBEB]">
        <div class="px-8 py-8 space-y-4">
          <div v-if="error" class="text-[13px] text-red-500 bg-red-50 border border-red-100 px-3 py-2">
            {{ error }}
          </div>

          <div>
            <label class="block text-[13px] text-[#1A1A1A] mb-1.5">用户名</label>
            <input v-model="form.username" type="text" placeholder="3-32个字符"
              class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] bg-white outline-none focus:border-[#1772F6] transition-colors" />
          </div>

          <div>
            <label class="block text-[13px] text-[#1A1A1A] mb-1.5">手机号</label>
            <input v-model="form.phone" type="tel" placeholder="请输入手机号"
              class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] bg-white outline-none focus:border-[#1772F6] transition-colors" />
          </div>

          <div>
            <label class="block text-[13px] text-[#1A1A1A] mb-1.5">性别</label>
            <select v-model="form.gender"
              class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] bg-white outline-none focus:border-[#1772F6] transition-colors cursor-pointer">
              <option value="男">男</option>
              <option value="女">女</option>
              <option value="保密">保密</option>
            </select>
          </div>

          <div>
            <label class="block text-[13px] text-[#1A1A1A] mb-1.5">密码</label>
            <div class="relative">
              <input v-model="form.password" :type="showPwd ? 'text' : 'password'" placeholder="至少6位"
                class="w-full h-10 px-3 pr-10 text-[14px] border border-[#EBEBEB] bg-white outline-none focus:border-[#1772F6] transition-colors" />
              <button @click="showPwd = !showPwd" type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-[#8590A6] hover:text-[#444] cursor-pointer">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path v-if="!showPwd" stroke-linecap="round" stroke-linejoin="round"
                    d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  <path v-else stroke-linecap="round" stroke-linejoin="round"
                    d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242" />
                </svg>
              </button>
            </div>
            <div v-if="form.password" class="flex gap-1 mt-1.5">
              <div v-for="i in 4" :key="i"
                :class="['flex-1 h-1 transition-colors', i <= strength ? strengthColor : 'bg-[#EBEBEB]']" />
            </div>
            <p v-if="strength" class="text-[11px] mt-1" :class="['text-[#8590A6]']">密码强度：{{ strengthLabel }}</p>
          </div>

          <div>
            <label class="block text-[13px] text-[#1A1A1A] mb-1.5">确认密码</label>
            <input v-model="form.confirm" :type="showPwd ? 'text' : 'password'" placeholder="再次输入密码"
              class="w-full h-10 px-3 text-[14px] border border-[#EBEBEB] bg-white outline-none focus:border-[#1772F6] transition-colors" />
          </div>

          <div>
            <label class="block text-[13px] text-[#1A1A1A] mb-1.5">验证码</label>
            <div class="flex gap-2">
              <input v-model="form.captcha" type="text" placeholder="图形验证码" maxlength="4"
                class="flex-1 h-10 px-3 text-[14px] border border-[#EBEBEB] bg-white outline-none focus:border-[#1772F6] transition-colors tracking-widest" />
              <img
                v-if="captchaImage"
                :src="captchaImage"
                @click="refreshCaptcha"
                class="h-10 cursor-pointer hover:opacity-80 transition-opacity shrink-0"
                alt="验证码"
              />
              <div
                v-else
                @click="refreshCaptcha"
                class="h-10 px-4 flex items-center justify-center bg-[#F6F6F6] border border-[#EBEBEB] cursor-pointer hover:bg-[#EBEBEB] select-none font-mono font-bold text-[16px] text-[#1772F6] tracking-widest transition-colors shrink-0"
              >
                {{ captchaFallback }}
              </div>
            </div>
          </div>

          <button @click="submit" :disabled="loading"
            class="w-full h-10 bg-[#1772F6] text-white font-medium text-[14px] hover:bg-[#0d65e8] disabled:opacity-60 transition-colors cursor-pointer">
            {{ loading ? '注册中…' : '注册' }}
          </button>
        </div>

        <div class="border-t border-[#EBEBEB] px-8 py-4 text-center text-[13px] text-[#8590A6]">
          已有账号？
          <RouterLink to="/userlogin" class="text-[#1772F6] hover:underline">立即登录</RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>
