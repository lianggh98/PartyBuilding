import axios from "axios";

const http = axios.create({
  baseURL: "/api",
  timeout: 10000,
});

http.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status;
    const payload = error.response?.data || { message: "请求失败" };
    if (status === 401 && !error.config?.url?.endsWith("/auth/login")) {
      sessionStorage.removeItem("token");
      if (location.pathname !== "/login") {
        location.href = "/login";
      }
    }
    return Promise.reject(payload);
  }
);

export function login(username, password) {
  return http.post("/auth/login", { username, password });
}

export function me() {
  return http.get("/auth/me");
}

export function orgTree() {
  return http.get("/orgs/tree");
}

export function orgFlat() {
  return http.get("/orgs");
}

export function createOrg(body) {
  return http.post("/orgs", body);
}

export function renameOrg(id, name) {
  return http.put(`/orgs/${id}`, { name });
}

export function listMembers(params) {
  return http.get("/members", { params });
}

export function createMember(body) {
  return http.post("/members", body);
}
