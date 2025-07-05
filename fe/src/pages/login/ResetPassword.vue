<template>
  <div class="reset-container">
    <el-card class="reset-card">
      <template #header>
        <h2 class="reset-title">找回密码</h2>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" prefix-icon="Phone" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleNext" :loading="loading" class="reset-button">下一步</el-button>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Phone } from '@element-plus/icons-vue'
import request from '@/utils/index'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  phone: ''
})

const rules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[2-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const handleNext = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    // 调用后端接口校验账号和手机号
    const res = await request.post('/verifyUserInfo', {
      username: form.username,
      phone: form.phone
    })
    if (res.success) {
      ElMessage.success(res.message || '验证成功，请设置新密码')
      // 跳转到修改密码页面，并传递用户名参数
      router.push({ path: '/reset-password-set', query: { username: form.username } })
    } else {
      ElMessage.error(res.message || '账号与手机号不匹配')
    }
  } catch (error) {
    console.error('验证失败:', error)
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.reset-container {
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

  .reset-card {
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

    .reset-title {
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

    .reset-button {
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