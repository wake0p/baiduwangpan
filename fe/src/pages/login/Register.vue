<template>
    <div class="register-container">
      <el-card class="register-card">
        <template #header>
          <h2 class="register-title">用户注册</h2>
        </template>
        <el-form 
          :model="registerForm" 
          :rules="rules" 
          ref="registerFormRef"
          label-position="top"
        >
          <el-form-item label="用户名" prop="username">
            <el-input 
              v-model="registerForm.username"
              placeholder="请输入用户名"
              prefix-icon="User"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input 
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input 
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input 
              v-model="registerForm.email"
              placeholder="请输入邮箱"
              prefix-icon="Message"
            />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input 
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              prefix-icon="Phone"
            />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input 
              v-model="registerForm.nickname"
              placeholder="请输入昵称"
              prefix-icon="User"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleRegister" :loading="loading" class="register-button">
              注册
            </el-button>
          </el-form-item>
          <div class="action-links">
            <span>已有账号？</span>
            <el-button type="text" @click="goToLogin">立即登录</el-button>
          </div>
        </el-form>
      </el-card>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive } from 'vue'
  import { useRouter } from 'vue-router'
  import { ElMessage } from 'element-plus'
  import { User, Lock, Message, Phone } from '@element-plus/icons-vue'
  import request from '@/utils/index'
  
  const router = useRouter()
  const registerFormRef = ref(null)
  const loading = ref(false)
  
  const registerForm = reactive({
    username: '',
    password: '',
    confirmPassword: '',
    email: '',
    phone: '',
    nickname: ''
  })
  
  const validatePass = (rule, value, callback) => {
    if (value === '') {
      callback(new Error('请输入密码'))
    } else {
      // 检查是否包含数字和字符
      const hasNumber = /\d/.test(value)
      const hasLetter = /[a-zA-Z]/.test(value)
      
      if (!hasNumber || !hasLetter) {
        callback(new Error('密码必须包含数字和字母'))
      } else {
        callback()
      }
    }
  }
  
  const validatePass2 = (rule, value, callback) => {
    if (value === '') {
      callback(new Error('请再次输入密码'))
    } else if (value !== registerForm.password) {
      callback(new Error('两次输入密码不一致!'))
    } else {
      callback()
    }
  }
  
  const rules = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
    ],
    password: [
      { required: true, validator: validatePass, trigger: 'blur' },
      { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
    ],
    confirmPassword: [
      { required: true, validator: validatePass2, trigger: 'blur' }
    ],
    email: [
      { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
    ],
    phone: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^1[2-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
    ],
    nickname: [
      { required: true, message: '请输入昵称', trigger: 'blur' },
      { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
    ]
  }
  
  const handleRegister = async () => {
    if (!registerFormRef.value) return
    try {
      await registerFormRef.value.validate()
      loading.value = true
      // 构造注册参数，去除confirmPassword
      const { confirmPassword, ...registerData } = registerForm
      // 发送POST请求
      const res = await request.post('/register', registerData)
      if (res.success) {
        console.log(registerData)
        console.log(res)
        ElMessage.success(res.message || '注册成功，请登录！')
        router.push('/login')
      } else {
        ElMessage.error(res.message || '注册失败')
      }
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      loading.value = false
    }
  }
  
  const goToLogin = () => {
    router.push('/login')
  }
  </script>
  
  <style lang="scss" scoped>
  .register-container {
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
  
    .register-card {
      width: 480px;
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
  
      .register-title {
        text-align: center;
        margin: 0;
        color: #2c3e50;
        font-size: 24px;
        font-weight: 600;
        letter-spacing: 1px;
      }
  
      :deep(.el-form-item) {
        margin-bottom: 15px;
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
  
      .register-button {
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