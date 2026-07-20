// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  // TODO: point at the real domain before the first deploy — Starlight uses it
  // for canonical URLs and the sitemap.
  site: 'https://fenix.d4emon.fr',

  integrations: [
    starlight({
      title: 'Fenix',
      description: 'A modern Minecraft mod loader.',
      customCss: ['./src/styles/fenix.css'],

      social: [
        {
          icon: 'github',
          label: 'GitHub',
          href: 'https://github.com/D4EMONDEV/Fenix',
        },
      ],

      sidebar: [
        { label: 'Guides', autogenerate: { directory: 'guides' } },
        { label: 'Reference', autogenerate: { directory: 'reference' } },
      ],

      editLink: {
        baseUrl: 'https://github.com/D4EMONDEV/Fenix/edit/main/website/',
      },
    }),
  ],
});
