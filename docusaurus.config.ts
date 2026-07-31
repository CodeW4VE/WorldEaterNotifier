import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

const config: Config = {
  title: 'WorldEaterNotifier',
  tagline:
    'Fabric mod that monitors world eaters, trenchers and bedrock breakers — Discord notifications when they stop, start, or get obstructed.',
  favicon: 'img/favicon.ico',

  url: 'https://codew4ve.github.io',
  baseUrl: '/WorldEaterNotifier/',

  organizationName: 'CodeW4VE',
  projectName: 'WorldEaterNotifier',

  onBrokenLinks: 'throw',

  markdown: {
    hooks: {
      onBrokenMarkdownLinks: ({sourceFilePath, url}) => {
        throw new Error(`Broken markdown link in ${sourceFilePath}: ${url}`);
      },
    },
  },

  i18n: {
    defaultLocale: 'en',
    locales: ['en', 'es'],
    localeConfigs: {
      en: {label: 'English', htmlLang: 'en'},
      es: {label: 'Español', htmlLang: 'es'},
    },
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          editUrl: 'https://github.com/CodeW4VE/WorldEaterNotifier/tree/docs-page/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'WorldEaterNotifier',
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docs',
          position: 'left',
          label: 'Docs',
        },
        {
          type: 'localeDropdown',
          position: 'right',
        },
        {
          href: 'https://github.com/CodeW4VE/WorldEaterNotifier',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {label: 'Quick Start', to: '/docs/quick-start'},
            {label: 'Minecraft Commands', to: '/docs/minecraft-commands'},
            {label: 'Webhook Mode', to: '/docs/webhook-setup'},
            {label: 'Bot Mode', to: '/docs/bot-setup'},
            {label: 'Message Templates', to: '/docs/message-templates'},
            {label: 'FAQ', to: '/docs/faq'},
          ],
        },
        {
          title: 'Project',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/CodeW4VE/WorldEaterNotifier',
            },
            {
              label: 'Releases',
              href: 'https://github.com/CodeW4VE/WorldEaterNotifier/releases',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} WorldEaterNotifier. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
