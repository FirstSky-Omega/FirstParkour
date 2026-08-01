import { defineConfig } from 'vitepress'

const repo = 'https://github.com/LostUmbrella58/IP-Reborn'

export default defineConfig({
  title: 'Infinite Parkour Reborn',
  description: 'Documentation for Infinite Parkour Reborn',
  base: '/IP-Reborn/',
  cleanUrls: true,
  lastUpdated: true,
  head: [
    ['meta', { name: 'theme-color', content: '#7c3aed' }]
  ],
  sitemap: {
    hostname: 'https://lostumbrella58.github.io/IP-Reborn/'
  },
  locales: {
    root: {
      label: 'English',
      lang: 'en'
    },
    zh: {
      label: '简体中文',
      lang: 'zh-CN',
      link: '/zh/'
    }
  },
  themeConfig: {
    logo: 'https://cdn.modrinth.com/data/cached_images/60d2d87882c3a8137076c6d33065b7c750320b64_0.webp',
    socialLinks: [{ icon: 'github', link: repo }],
    search: { provider: 'local' },
    locales: {
      root: {
        nav: [
          { text: 'Guide', link: '/guide/introduction' },
          { text: 'Reference', link: '/guide/commands' },
          { text: 'GitHub', link: repo }
        ],
        sidebar: {
          '/guide/': [
            {
              text: 'Getting started',
              items: [
                { text: 'Introduction', link: '/guide/introduction' },
                { text: 'Installation', link: '/guide/installation' },
                { text: 'Migration', link: '/guide/migration' },
                { text: 'Game modes', link: '/guide/game-modes' }
              ]
            },
            {
              text: 'Server administration',
              items: [
                { text: 'Commands', link: '/guide/commands' },
                { text: 'Permissions', link: '/guide/permissions' },
                { text: 'Configuration', link: '/guide/configuration' },
                { text: 'Rewards & schematics', link: '/guide/rewards-schematics' },
                { text: 'Integrations', link: '/guide/integrations' }
              ]
            },
            {
              text: 'Reference',
              items: [
                { text: 'PlaceholderAPI', link: '/guide/placeholders' },
                { text: 'Developer API', link: '/guide/developer-api' },
                { text: 'Troubleshooting', link: '/guide/troubleshooting' }
              ]
            }
          ]
        },
        editLink: {
          pattern: `${repo}/edit/main/docs/:path`,
          text: 'Edit this page on GitHub'
        },
        outline: { label: 'On this page', level: [2, 3] },
        lastUpdated: { text: 'Last updated' },
        docFooter: { prev: 'Previous', next: 'Next' },
        footer: {
          message: 'Released under the GPL-3.0 license.',
          copyright: 'Infinite Parkour was originally created by Efnilite.'
        }
      },
      zh: {
        nav: [
          { text: '指南', link: '/zh/guide/introduction' },
          { text: '参考', link: '/zh/guide/commands' },
          { text: 'GitHub', link: repo }
        ],
        sidebar: {
          '/zh/guide/': [
            {
              text: '快速开始',
              items: [
                { text: '介绍', link: '/zh/guide/introduction' },
                { text: '安装', link: '/zh/guide/installation' },
                { text: '迁移', link: '/zh/guide/migration' },
                { text: '游戏模式', link: '/zh/guide/game-modes' }
              ]
            },
            {
              text: '服务器管理',
              items: [
                { text: '命令', link: '/zh/guide/commands' },
                { text: '权限', link: '/zh/guide/permissions' },
                { text: '配置', link: '/zh/guide/configuration' },
                { text: '奖励与建筑模板', link: '/zh/guide/rewards-schematics' },
                { text: '插件联动', link: '/zh/guide/integrations' }
              ]
            },
            {
              text: '参考资料',
              items: [
                { text: 'PlaceholderAPI', link: '/zh/guide/placeholders' },
                { text: '开发者 API', link: '/zh/guide/developer-api' },
                { text: '故障排查', link: '/zh/guide/troubleshooting' }
              ]
            }
          ]
        },
        editLink: {
          pattern: `${repo}/edit/main/docs/:path`,
          text: '在 GitHub 上编辑此页'
        },
        outline: { label: '本页目录', level: [2, 3] },
        lastUpdated: { text: '最后更新' },
        docFooter: { prev: '上一页', next: '下一页' },
        footer: {
          message: '基于 GPL-3.0 许可证发布。',
          copyright: 'Infinite Parkour 最初由 Efnilite 创作。'
        }
      }
    }
  }
})
