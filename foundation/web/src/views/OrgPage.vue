<template>
  <div>
    <div class="toolbar">
      <h2>党组织</h2>
      <el-button v-if="canCreateCommittee" type="primary" @click="openCreate(null)">新增党委</el-button>
    </div>
    <el-tree
      v-loading="loading"
      :data="tree"
      node-key="id"
      default-expand-all
      :props="{ label: 'name', children: 'children' }"
    >
      <template #default="{ data }">
        <div class="node">
          <el-tag size="small">{{ orgTypeLabel[data.orgType] }}</el-tag>
          <span>{{ data.name }}</span>
          <el-button v-if="canAddChild(data)" link type="primary" @click.stop="openCreate(data)">新增下级</el-button>
          <el-button v-if="user?.canWrite" link type="primary" @click.stop="openRename(data)">改名</el-button>
        </div>
      </template>
    </el-tree>

    <el-dialog v-model="createVisible" title="新增组织" width="420px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="createForm.orgType" :disabled="typeOptions.length === 1" style="width: 100%">
            <el-option v-for="item in typeOptions" :key="item" :label="orgTypeLabel[item]" :value="item" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="renameVisible" title="修改名称" width="420px">
      <el-input v-model="renameForm.name" />
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRename">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createOrg, orgTree, renameOrg } from "../api";
import { orgTypeLabel } from "../labels";

const loading = ref(false);
const tree = ref([]);
const user = computed(() => JSON.parse(sessionStorage.getItem("user") || "null"));
const canCreateCommittee = computed(() => user.value?.role === "SYSTEM_ADMIN");

const createVisible = ref(false);
const createForm = reactive({ parentId: null, name: "", orgType: "BRANCH" });
const typeOptions = ref(["BRANCH"]);
const renameVisible = ref(false);
const renameForm = reactive({ id: null, name: "" });

function canAddChild(node) {
  if (!user.value?.canWrite || node.orgType === "BRANCH") return false;
  return true;
}

async function load() {
  loading.value = true;
  try {
    const result = await orgTree();
    tree.value = result.data;
  } catch (error) {
    ElMessage.error(error.message || "加载组织失败");
  } finally {
    loading.value = false;
  }
}

function openCreate(parent) {
  createForm.parentId = parent ? parent.id : null;
  createForm.name = "";
  if (!parent) {
    typeOptions.value = ["COMMITTEE"];
  } else if (parent.orgType === "COMMITTEE") {
    typeOptions.value = ["GENERAL_BRANCH", "BRANCH"];
  } else {
    typeOptions.value = ["BRANCH"];
  }
  createForm.orgType = typeOptions.value[0];
  createVisible.value = true;
}

async function submitCreate() {
  try {
    await createOrg({
      parentId: createForm.parentId,
      name: createForm.name,
      orgType: createForm.orgType,
      sortNo: 0,
    });
    createVisible.value = false;
    ElMessage.success("已新增");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "新增失败");
  }
}

function openRename(node) {
  renameForm.id = node.id;
  renameForm.name = node.name;
  renameVisible.value = true;
}

async function submitRename() {
  try {
    await renameOrg(renameForm.id, renameForm.name);
    renameVisible.value = false;
    ElMessage.success("已保存");
    await load();
  } catch (error) {
    ElMessage.error(error.message || "保存失败");
  }
}

onMounted(load);
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}
</style>
