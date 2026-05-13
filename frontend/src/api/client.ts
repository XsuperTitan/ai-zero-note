export const API_ORIGIN = import.meta.env.VITE_API_ORIGIN ?? "http://localhost:8080";

/**
 * Fetch with session cookie for cross-origin localhost development (Vue → Spring Boot).
 */
export async function apiFetch(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
  return fetch(input, { ...init, credentials: "include" });
}
