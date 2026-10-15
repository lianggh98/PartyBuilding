<template>
  <div class="login-page">
    <el-card class="login-card">
      <h1>智慧党建底座</h1>
      <p class="hint">第二阶段验证：管理员登录由服务端校验</p>
      <el-form :model="form" @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" autocomplete="username" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" show-password autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { login } from "../api";

const router = useRouter();
const loading = ref(false);
const form = reactive({ username: "", password: "" });

async function submit() {
  loading.value = true;
  try {
    const result = await login(form.username, form.password);
    sessionStorage.setItem("token", result.data.token);
    sessionStorage.setItem("user", JSON.stringify(result.data.user));
    router.push("/orgs");
  } catch (error) {
    ElMessage.error(error.message || "登录失败");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f4f6f8;
}
.login-card {
  width: 380px;
}
h1 {
  margin: 0 0 8px;
  font-size: 22px;
}
.hint {
  margin: 0 0 20px;
  color: #667085;
  font-size: 13px;
}
</style>
