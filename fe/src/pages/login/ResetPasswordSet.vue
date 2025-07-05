<template>
  <div class="reset-set-container">
    <el-card class="reset-set-card">
      <template #header>
        <h2 class="reset-set-title">设置新密码</h2>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入新密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleReset" :loading="loading" class="reset-set-button">提交</el-button>
        </el-form-item>
        <div class="action-links">
          <el-button type="text" @click="goToLogin">返回登录</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import request from '@/utils/index'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  password: '',
  confirmPassword: ''
})

const validatePass = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入新密码'))
  } else {
    if (form.confirmPassword !== '') {
      formRef.value?.validateField('confirmPassword')
    }
    callback()
  }
}

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  password: [
    { required: true, validator: validatePass, trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' }
  ]
}

const handleReset = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    // 获取用户名参数
    const username = route.query.username
    if (!username) {
      ElMessage.error('未获取到用户名，请重新操作')
      return
    }
    // 调用后端接口重置密码
    const res = await request.post('/changePassword', {
      username,
      password: form.password
    })
    if (res.success) {
      ElMessage.success(res.message || '密码重置成功，请登录！')
      router.push('/login')
    } else {
      ElMessage.error(res.message || '密码重置失败')
    }
  } catch (error) {
    console.error('密码重置失败:', error)
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.reset-set-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0) 60%);
    animation: rotate 30s linear infinite;
  }

  .reset-set-card {
    width: 420px;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(10px);
    border-radius: 16px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    padding: 20px;
    position: relative;
    z-index: 1;
    transition: transform 0.3s ease;

    &:hover {
      transform: translateY(-5px);
    }
    
    :deep(.el-card__header) {
      padding: 20px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.05);
    }

    .reset-set-title {
      text-align: center;
      margin: 0;
      color: #2c3e50;
      font-size: 24px;
      font-weight: 600;
      letter-spacing: 1px;
    }

    :deep(.el-form-item) {
      margin-bottom: 25px;
    }

    :deep(.el-input) {
      .el-input__wrapper {
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
        border-radius: 8px;
        padding: 8px 15px;
        transition: all 0.3s ease;

        &:hover, &.is-focus {
          box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
        }
      }

      .el-input__inner {
        height: 40px;
      }
    }

    .reset-set-button {
      width: 100%;
      height: 44px;
      border-radius: 8px;
      font-size: 16px;
      font-weight: 500;
      letter-spacing: 1px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border: none;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
      }

      &:active {
        transform: translateY(0);
      }
    }

    .action-links {
      text-align: center;
      margin-top: 16px;
      color: #606266;
      font-size: 14px;

      .el-button {
        margin-left: 4px;
        font-size: 14px;
      }
    }
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style> 