import { defineConfig } from 'vitepress'
import fixturesSidebar from './fixtures-sidebar.json'

export default defineConfig({
  title: 'Theatrical: Extra Lights',
  description: 'Wiki for the Theatrical: Extra Lights Minecraft mod — fixtures, DMX channel maps, guides and grandMA2 files.',
  base: '/',
  lang: 'en-US',
  lastUpdated: true,
  cleanUrls: true,
  sitemap: { hostname: 'https://extra-light.nailec.fr' },
  head: [['link', { rel: 'icon', href: '/favicon.png' }]],
  themeConfig: {
    logo: '/logo.png',
    nav: [
      { text: 'Guide', link: '/guide/introduction' },
      { text: 'Fixtures', link: '/fixtures/overview' },
      { text: 'grandMA2', link: '/guide/grandma2' },
      { text: 'Changelog', link: '/changelog' },
      { text: 'Download', link: 'https://github.com/dumann089/TheatricalExtraLights/releases' }
    ],
    sidebar: {
      '/guide/': [
        {
          text: 'Getting started',
          items: [
            { text: 'Introduction', link: '/guide/introduction' },
            { text: 'Installation', link: '/guide/installation' },
            { text: 'Quick start', link: '/guide/quick-start' },
            { text: 'Patching (DMX & Art-Net)', link: '/guide/patching' }
          ]
        },
        {
          text: 'Using the fixtures',
          items: [
            { text: 'Fixture config screen', link: '/guide/config-screen' },
            { text: 'Gobo heads & personalities', link: '/guide/gobo-heads' },
            { text: 'Custom gobos', link: '/guide/custom-gobos' },
            { text: 'Followspot console', link: '/guide/followspot' },
            { text: 'Lasers & emergency stop', link: '/guide/lasers' },
            { text: 'Pyro & safety arm', link: '/guide/pyro' }
          ]
        },
        {
          text: 'Desks & files',
          items: [
            { text: 'grandMA2 fixture files', link: '/guide/grandma2' }
          ]
        },
        {
          text: 'Configuration',
          items: [
            { text: 'Config file', link: '/guide/config-file' },
            { text: 'Rendering & performance', link: '/guide/rendering' },
            { text: 'Multiplayer notes', link: '/guide/multiplayer' },
            { text: 'Troubleshooting & FAQ', link: '/guide/faq' }
          ]
        }
      ],
      '/fixtures/': [
        { text: 'Fixture reference', items: [{ text: 'Overview', link: '/fixtures/overview' }] },
        ...fixturesSidebar
      ]
    },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/dumann089/TheatricalExtraLights' },
      { icon: 'discord', link: 'https://discord.gg/7qMs5d6' }
    ],
    search: { provider: 'local' },
    editLink: {
      pattern: 'https://github.com/dumann089/TheatricalExtraLights/edit/ver/1.20.1/docs/:path',
      text: 'Edit this page on GitHub'
    },
    footer: {
      message: 'Released under the MIT License. Theatrical: Extra Lights is an addon for Theatrical.',
      copyright: 'Theatrical: Extra Lights contributors · Made by <a href="https://nailec.fr" target="_blank" rel="noopener noreferrer">nailec</a> <span class="nailec-heart" aria-hidden="true">♥</span>'
    },
    outline: [2, 3]
  }
})
