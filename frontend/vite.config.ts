import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { VitePWA } from 'vite-plugin-pwa'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'favicon.ico', 'apple-touch-icon-180x180.png'],
      manifest: {
        name: 'FIPE Explorer',
        short_name: 'FIPE Explorer',
        description: 'Exploração e análise da Tabela FIPE: busca, comparador e alertas de preço.',
        lang: 'pt-BR',
        theme_color: '#0f172a',
        background_color: '#f8fafc',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        icons: [
          { src: 'pwa-64x64.png', sizes: '64x64', type: 'image/png' },
          { src: 'pwa-192x192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512x512.png', sizes: '512x512', type: 'image/png' },
          { src: 'maskable-icon-512x512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        // Só as consultas públicas (busca, ficha, catálogos, stats) entram no cache do service
        // worker - isso é o "cache da última busca/consulta": se a API cair, a última resposta boa
        // ainda aparece. Nada sob /me/**, /auth/** ou /admin/** é cacheado (dado de usuário/sessão).
        runtimeCaching: [
          {
            urlPattern: ({ url }) =>
              /\/api\/v1\/(vehicles\/search|vehicles\/compare|models\/|brands|vehicle-types|fuel-types|stats\/)/.test(
                url.pathname,
              ),
            handler: 'NetworkFirst',
            options: {
              cacheName: 'fipe-api-cache',
              networkTimeoutSeconds: 5,
              expiration: {
                maxEntries: 50,
                maxAgeSeconds: 60 * 60 * 24, // 1 dia
              },
              cacheableResponse: {
                statuses: [0, 200],
              },
            },
          },
        ],
      },
    }),
  ],
})
