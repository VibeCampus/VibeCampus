<script setup>
import { toastState, toast } from '@/composables/useToast'

const styleMap = {
  error: 'bg-red-50 border-red-200 text-red-700',
  success: 'bg-emerald-50 border-emerald-200 text-emerald-700',
  info: 'bg-sky-50 border-sky-200 text-sky-700',
  warning: 'bg-amber-50 border-amber-200 text-amber-700',
}
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-4 left-1/2 -translate-x-1/2 z-[9999] flex flex-col gap-2 pointer-events-none">
      <transition-group name="toast">
        <div
          v-for="item in toastState.list"
          :key="item.id"
          :class="['min-w-[260px] max-w-[420px] px-4 py-2.5 text-[13px] border shadow-sm pointer-events-auto cursor-pointer', styleMap[item.type] || styleMap.info]"
          role="alert"
          @click="toast.dismiss(item.id)"
        >
          {{ item.message }}
        </div>
      </transition-group>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: opacity 200ms ease, transform 200ms ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
