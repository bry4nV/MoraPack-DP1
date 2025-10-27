// src/app/actions/login.ts
"use server";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export type LoginState = { error?: string };

export async function loginAction(
  _prev: LoginState,
  formData: FormData
): Promise<LoginState> {
  const email = String(formData.get("email") || "");
  const password = String(formData.get("password") || "");

  // TODO: valida contra tu API
  if (email && password) {
    const cookieStore = await cookies();
    cookieStore.set("auth", "1", { httpOnly: true, path: "/" });
    redirect("/");
  }

  // Si no redirige, SIEMPRE retorna LoginState
  return { error: "Credenciales inválidas" };
}
