# Módulo: Frontend (M14)

Next.js 16 (App Router) · TypeScript · React 19 · Tailwind CSS 4.
A adicionar conforme os blocos: `bpmn-js`, `bpmn-moddle`, Monaco Editor, TanStack Query,
Zustand, shadcn/ui.

## Organização de código (Seção 6 / M14 do plano)

```
src/
├── app/                 rotas (App Router) — ver árvore de rotas na ficha M14 do plano
├── features/<modulo>/   componentes, hooks e serviços por módulo, espelhando M02–M13
├── components/ui/       biblioteca de componentes (shadcn/ui)
├── lib/api/             cliente HTTP tipado, gerado/escrito a partir do OpenAPI
└── lib/bpmn/            integração com bpmn-js
```

**Regra (M14):** cada *feature* consome exclusivamente os endpoints do módulo backend
correspondente. Nada de `fetch` direto em componente — sempre via `lib/api/`.

## Executar

```bash
cp .env.example .env.local
npm install
npm run dev            # http://localhost:3000
npm run build
npm run lint
```

Requer Node 24 (ver `.nvmrc`). Backend esperado em `NEXT_PUBLIC_API_BASE_URL`.
