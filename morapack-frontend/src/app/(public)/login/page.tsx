// (public)/login/page.tsx
import { redirect } from "next/navigation";
import { cookies } from "next/headers";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

export default function LoginPage() {
  async function login(formData: FormData) {
    "use server";

    const email = String(formData.get("email") || "");
    const pass  = String(formData.get("password") || "");
    const next  = String(formData.get("next") || "/");

    if (!email || !pass) {
      redirect("/login?error=missing");
    }

    // ¡Sin await! y antes del redirect
    cookies().set({
      name: "auth",
      value: "1",
      httpOnly: true,
      path: "/",
      sameSite: "lax",   // recomendable
      // secure: true     // solo en prod con HTTPS
    });

    return redirect(next || "/");
  }

  return (
    <main className="min-h-dvh grid place-items-center p-6">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Iniciar sesión</CardTitle>
        </CardHeader>
        <CardContent>
          <form action={login} className="space-y-4">
            {/* si venías redirigido desde middleware */}
            <input type="hidden" name="next" value="/" />
            <div className="grid gap-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" name="email" type="email" placeholder="you@demo.com" />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="password">Contraseña</Label>
              <Input id="password" name="password" type="password" />
            </div>
            <Button type="submit" className="w-full">Entrar</Button>
          </form>
        </CardContent>
      </Card>
    </main>
  );
}
