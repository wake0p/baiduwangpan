<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2 class="login-title">千度网盘登录</h2>
      </template>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="captcha">
          <div class="captcha-container">
            <el-input 
              v-model="loginForm.captcha"
              placeholder="请输入验证码"
              prefix-icon="Key"
            />
            <div 
              class="captcha-image" 
              @click="refreshCaptcha"
              :style="{
                backgroundColor: captchaData.bgColor,
                color: captchaData.textColor
              }"
            >
              {{ captchaData.captchaDisplay }}
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" class="login-button">
            登录
          </el-button>
        </el-form-item>
        <div class="action-links">
          <el-button type="text" @click="goToRegister">立即注册</el-button>
          <el-divider direction="vertical" />
          <el-button type="text" @click="goToResetPassword">忘记密码？</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import request from '@/utils/index'
import { useUserStore } from '@/store/user'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)
const userStore = useUserStore()

// 验证码相关状态
const captchaData = ref({
  bgColor: '#E0FFF4',
  textColor: '#AEC750',
  captchaDisplay: 'muq0'
})

const loginForm = reactive({
  username: '',
  password: '',
  captcha: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '验证码长度为 4 位', trigger: 'blur' }
  ]
}

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    const data = await request.get('/generateCaptcha')
    captchaData.value = data
  } catch (error) {
    console.error('获取验证码失败:', error)
    ElMessage.error('获取验证码失败，请重试')
  }
}

// 初始化时获取验证码
refreshCaptcha()

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()
    loading.value = true

    // 直接提交用户输入的验证码
    const res = await request.post('/login', {
      username: loginForm.username,
      password: loginForm.password,
      captcha: loginForm.captcha
    })

    if (res.success) {
      userStore.setUser(res.user)
      userStore.setStatus(res.user.status)
      ElMessage.success(res.message || '登录成功')
      router.push('/dashboard')
    } else {
      // 根据 errorType 判断
      if (res.errorType === 'captcha') {
        ElMessage.error(res.message || '验证码错误')
        refreshCaptcha()
      } else {
        ElMessage.error(res.message || '登录失败')
      }
    }
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error('登录请求异常')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/register')
}

const goToResetPassword = () => {
  router.push('/reset-password')
}
</script>

<style lang="scss" scoped>
.login-container {
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

  .login-card {
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

    .login-title {
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

    .captcha-container {
      display: flex;
      gap: 12px;
      align-items: center;

      .el-input {
        flex: 1;
      }

      .captcha-image {
        width: 120px;
        height: 40px;
        border-radius: 8px;
        overflow: hidden;
        cursor: pointer;
        transition: all 0.3s ease;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        font-weight: bold;
        letter-spacing: 2px;
        user-select: none;

        &:hover {
          transform: scale(1.02);
        }
      }
    }

    .login-button {
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
        margin: 0 4px;
        font-size: 14px;
      }

      .el-divider {
        margin: 0 8px;
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