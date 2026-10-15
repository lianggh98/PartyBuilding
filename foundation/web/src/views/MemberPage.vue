<template>
  <div>
    <div class="toolbar">
      <h2>党员档案</h2>
      <el-button v-if="user?.canWrite" type="primary" @click="openCreate">新增党员</el-button>
    </div>
    <el-form :inline="true" :model="query" @submit.prevent="load">
      <el-form-item label="姓名">
        <el-input v-model="query.name" clearable placeholder="按姓名查询" />
      </el-form-item>
      <el-form-item label="组织">
        <el-select v-model="query.orgId" clearable placeholder="可见范围内的组织" style="width: 240px">
          <el-option v-for="org in orgs" :key="org.id" :label="org.name" :value="org.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" native-type="submit">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="80" />
      <el-table-column prop="memberNo" label="党员编号" width="160" />
      <el-table-column prop="orgName" label="所属党支部" />
      <el-table-column prop="phone" label="手机" width="140" />
      <el-table-column prop="joinedAt" label="入党日期" width="140" />
      <el-table-column prop="status" label="状态" width="100" />
    </el-table>

    <el-dialog v-model="visible" title="新增党员" width="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="姓名">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" clearable style="width: 100%">
            <el-option label="男" value="男" />
            <el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="党支部">
          <el-select v-model="form.orgId" style="width: 100%">
            <el-option v-for="org in branches" :key="org.id" :label="org.name" :value="org.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="党员编号">
          <el-input v-model="form.memberNo" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="入党日期">
          <el-date-picker v-model="form.joinedAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createMember, listMembers, orgFlat } from "../api";

const user = computed(() => JSON.parse(sessionStorage.getItem("user") || "null"));
const loading = ref(false);
const rows = ref([]);
const orgs = ref([]);
const query = reactive({ name: "", orgId: null });
const visible = ref(false);
const form = reactive({ name: "", gender: "", orgId: null, memberNo: "", phone: "", joinedAt: "" });
const branches = computed(() => orgs.value.filter((org) => org.orgType === "BRANCH"));

async function loadOrgs() {
  const result = await orgFlat();
  orgs.value = result.data;
}

async function load() {
  loading.value = true;
  try {
    const result = await listMembers({
      name: query.name || undefined,
      orgId: query.orgId || undefined,
    });
    rows.value = result.data;
  } catch (error) {
    ElMessage.error(error.message || "查询失败");
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  form.name = "";
  form.gender = "";
  form.orgId = branches.value[0]?.id ?? null;
  form.memberNo = "";
  form.phone = "";
  form.joinedAt = "";
  visible.value = true;
}

async function submit() {
  try {
    await createMember({ ...form, status: "ACTIVE" });
    visible.value = false;
    ElMessage.success("已新增");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "新增失败");
  }
}

onMounted(async () => {
  try {
    await loadOrgs();
    await load();
  } catch (error) {
    ElMessage.error(error.message || "加载失败");
  }
});
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
