import { clearToken, loadToken } from "./auth";

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

type JsonValue = null | boolean | number | string | JsonValue[] | { [key: string]: JsonValue };

export async function apiFetch<T extends JsonValue>(
  path: string,
  init: RequestInit & { auth?: boolean } = {}
): Promise<T> {
  const headers = new Headers(init.headers || {});
  headers.set("Accept", "application/json");

  const auth = init.auth ?? true;
  if (auth) {
    const token = loadToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(path, { ...init, headers });
  const body = (await response.json().catch(() => null)) as T | null;

  if (response.status === 401) {
    clearToken();
    if (typeof window !== "undefined") {
      window.location.assign("/signin");
    }
  }

  if (!response.ok) {
    const message =
      (body && typeof body === "object" && "message" in body && typeof (body as any).message === "string"
        ? (body as any).message
        : null) ||
      response.statusText ||
      "Request failed";
    throw new ApiError(message, response.status);
  }

  return (body ?? (null as unknown as T)) as T;
}
