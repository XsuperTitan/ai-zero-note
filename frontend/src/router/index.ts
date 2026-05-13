import { createRouter, createWebHistory } from "vue-router";
import HomeView from "../views/HomeView.vue";
import UserLoginPage from "../views/user/UserLoginPage.vue";
import UserRegisterPage from "../views/user/UserRegisterPage.vue";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "home", component: HomeView },
    { path: "/login", redirect: "/user/login" },
    { path: "/user/login", name: "login", component: UserLoginPage },
    { path: "/user/register", name: "register", component: UserRegisterPage }
  ]
});
