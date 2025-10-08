## Estructura de las carpetas


### 🧩 Descripción de carpetas principales

| Carpeta | Descripción |
|----------|--------------|
| **`app/`** | Contiene las páginas, layouts y rutas del proyecto **Next.js**. Cada subcarpeta dentro de `app/` representa una ruta (por ejemplo, `/login`, `/mapa`, `/vuelos`). También puede incluir endpoints y layouts globales. |
| **`components/`** | Componentes reutilizables de la interfaz. Aquí se almacenan tanto los componentes **UI** generados por *shadcn/ui* (`src/components/ui`) como los componentes de **layout** (`src/components/layout`) que estructuran la aplicación (sidebar, navegación, etc.). |
| **`data/`** | Archivos de datos estáticos o de prueba (mock data) utilizados durante el desarrollo. |
| **`hooks/`** | Custom Hooks de React que encapsulan lógica reutilizable, como control de estado o efectos. |
| **`lib/`** | Funciones utilitarias y helpers generales del proyecto, como `cn.ts` (fusión de clases Tailwind) o configuraciones para clientes HTTP, validaciones, etc. |
| **`styles/`** | Archivos de estilos globales. Contiene `globals.css`, donde se importan las configuraciones base de **TailwindCSS** y variables de tema. |
| **`types/`** | Definiciones **TypeScript** para entidades del sistema (interfaces y tipos para usuarios, vuelos, pedidos, etc.). |

---

📌 **Notas:**
- El proyecto está preparado para escalar modularmente, permitiendo agregar nuevas rutas en `app/` o nuevos componentes en `components/` sin romper la estructura.  
- Los componentes de *shadcn/ui* se gestionan mediante el archivo `components.json` y se ubican automáticamente en `src/components/ui/`.  
- Tailwind y shadcn/ui usan la convención `@/` como alias para `src/`, configurado en `tsconfig.json`.

---




This is a [Next.js](https://nextjs.org) project bootstrapped with [`create-next-app`](https://nextjs.org/docs/app/api-reference/cli/create-next-app).

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
