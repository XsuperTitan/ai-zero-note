import { useLoginUserStore } from "./stores/loginUser";
const PUBLIC_PATHS = new Set(["/user/login", "/user/register"]);
let firstFetchLoginUser = true;
/**
 * Global auth guard (aligned with ni-ai-code-king yu-ai-code-mother-frontend access.ts): refresh session on first entry, block non-public routes when anonymous.
 */
export function setupRouteAccess(router) {
    router.beforeEach(async (to, _from, next) => {
        const loginUserStore = useLoginUserStore();
        if (firstFetchLoginUser) {
            await loginUserStore.fetchLoginUser();
            firstFetchLoginUser = false;
        }
        if (PUBLIC_PATHS.has(to.path)) {
            next();
            return;
        }
        const u = loginUserStore.loginUser;
        if (!("id" in u) || u.id == null) {
            next({
                path: "/user/login",
                query: to.fullPath !== "/" && to.fullPath !== "/user/login" ? { redirect: to.fullPath } : {},
                replace: true
            });
            return;
        }
        next();
    });
}
