<template>
  <el-container class="shell">
    <el-header class="header">
      <strong>智慧党建底座</strong>
      <div class="who">
        <span>{{ profile }}</span>
        <el-button link type="primary" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="200px" class="aside">
        <el-menu :default-active="route.path" router>
          <el-menu-item index="/orgs">党组织</el-menu-item>
          <el-menu-item index="/members">党员档案</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { roleLabel } from "../labels";

const route = useRoute();
const router = useRouter();
const user = computed(() => {
  try {
    return JSON.parse(sessionStorage.getItem("user") || "null");
  } catch {
    return null;
  }
});
const profile = computed(() => {
  if (!user.value) return "";
  const role = roleLabel[user.value.role] || user.value.role;
  const org = user.value.orgName ? ` · ${user.value.orgName}` : "";
  return `${user.value.displayName}（${role}${org}）`;
});

function logout() {
  sessionStorage.removeItem("token");
  sessionStorage.removeItem("user");
  router.push("/login");
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
}
.who {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #344054;
}
.aside {
  border-right: 1px solid #e5e7eb;
}
</style>
