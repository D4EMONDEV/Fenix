// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  // Canonical URLs and the sitemap.
  //
  // Not on GitHub Pages under this repository: that URL is the Maven repository
  // already (https://d4emondev.github.io/Fenix/), and the two cannot share it.
  // The site wants its own home -- a custom domain, or a user Pages site with
  // the Maven repo staying where it is.
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
        { label: 'Download', link: '/download/' },
        { label: 'Guides', items: [{ autogenerate: { directory: 'guides' } }] },
        { label: 'Reference', items: [{ autogenerate: { directory: 'reference' } }] },
      ],

      editLink: {
        baseUrl: 'https://github.com/D4EMONDEV/Fenix/edit/main/website/',
      },
    }),
  ],
});
