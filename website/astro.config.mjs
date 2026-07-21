// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import starlightVersions from 'starlight-versions';

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

      // The mark beside the name, not instead of it: a logo nobody has seen
      // before does not tell a first visitor what site they are on.
      logo: { src: './src/assets/logo.svg', alt: '' },

      favicon: '/favicon.svg',
      head: [
        // Safari still does not take an SVG favicon, and a broken favicon is
        // the one asset every visitor sees.
        { tag: 'link', attrs: { rel: 'icon', href: '/favicon.png', type: 'image/png', sizes: '64x64' } },
      ],
      customCss: ['./src/styles/fenix.css'],

      // English is the source of truth and lives at the root; French sits under
      // /fr/. A page with no translation falls back to English rather than
      // 404ing, so translations can land one at a time.
      defaultLocale: 'root',
      locales: {
        root: { label: 'English', lang: 'en' },
        fr: { label: 'Français', lang: 'fr' },
      },

      // Versioned docs.
      //
      // The pages at the root describe what is being written now; each entry
      // here is a snapshot of what a release actually shipped. Somebody playing
      // 0.1 and following instructions written for 0.3 is worse served than
      // somebody reading something honestly older.
      //
      // A new release is one line here plus
      // `npx starlight-versions create <slug>`, which copies the current docs
      // into a folder of their own and leaves them alone from then on.
      plugins: [starlightVersions({ versions: [{ slug: '0.1' }] })],

      social: [
        {
          icon: 'github',
          label: 'GitHub',
          href: 'https://github.com/D4EMONDEV/Fenix',
        },
      ],

      // Two audiences that want opposite things: someone installing a loader
      // and someone writing against one. Splitting them is the difference
      // between a page that answers your question and a page that mentions it.
      sidebar: [
        {
          label: 'Playing',
          translations: { fr: 'Jouer' },
          items: [
            { label: 'Install Fenix', translations: { fr: 'Installer Fenix' }, slug: 'play/install' },
            { label: 'Adding mods', translations: { fr: 'Ajouter des mods' }, slug: 'play/mods' },
          ],
        },
        {
          label: 'Making mods',
          translations: { fr: 'Créer un mod' },
          items: [
            { label: 'Getting started', translations: { fr: 'Démarrer' }, slug: 'guides/getting-started' },
            { label: 'Generating resources', translations: { fr: 'Générer les ressources' }, slug: 'guides/ember' },
          ],
        },
        {
          label: 'Why Fenix',
          translations: { fr: 'Pourquoi Fenix' },
          items: [
            { label: 'Speed', translations: { fr: 'Vitesse' }, slug: 'why/performance' },
            { label: 'Compared to other loaders', translations: { fr: 'Comparé aux autres chargeurs' }, slug: 'why/comparison' },
          ],
        },
        {
          label: 'Reference',
          translations: { fr: 'Référence' },
          items: [{ autogenerate: { directory: 'reference' } }],
        },
      ],

      editLink: {
        baseUrl: 'https://github.com/D4EMONDEV/Fenix/edit/main/website/',
      },
    }),
  ],
});
