import { defineCollection } from 'astro:content';
import { docsLoader, i18nLoader } from '@astrojs/starlight/loaders';
import { docsSchema, i18nSchema } from '@astrojs/starlight/schema';

export const collections = {
  docs: defineCollection({ loader: docsLoader(), schema: docsSchema() }),
  // Starlight translates its own interface; the version plugin's strings are
  // ours to translate, and they sit on the page a reader of an old version sees
  // first.
  i18n: defineCollection({ loader: i18nLoader(), schema: i18nSchema() }),
};
