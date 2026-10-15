import { createRouter, createWebHistory } from "vue-router";
import Login from "./views/Login.vue";
import Layout from "./views/Layout.vue";
import OrgPage from "./views/OrgPage.vue";
import MemberPage from "./views/MemberPage.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", component: Login },
    {
      path: "/",
      component: Layout,
      children: [
        { path: "", redirect: "/orgs" },
        { path: "orgs", component: OrgPage },
        { path: "members", component: MemberPage },
      ],
    },
  ],
});

router.beforeEach((to) => {
  const token = sessionStorage.getItem("token");
  if (to.path === "/login") {
    return token ? "/orgs" : true;
  }
  return token ? true : "/login";
});
