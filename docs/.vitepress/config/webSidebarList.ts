export const webSidebarList = {
  'base': [
    {
      text: '前端基础知识',
      collapsed: false,
      link: '/web/base/index'
    },
    {
      text: '浏览器相关',
      collapsed: false,
      items: [
        {text: 'url到页面展示的全过程', link: '/web/base/Browser/urlToPage'}
      ]
    },
    {
      text: 'JavaScript',
      collapsed: false,
      items: [
        {text: '事件循环（消息循环）', link: '/web/base/JavaScript/messageLoop'},
        {text: 'web Worker', link: '/web/base/JavaScript/webWorker'},
        {text: 'Service Worker', link: '/web/base/JavaScript/serviceWorker'},
        {text: 'Web Component', link: '/web/base/JavaScript/webComponent'},
        {text: 'WebAssembly', link: '/web/base/JavaScript/webAssembly'}
      ]
    }
  ],
  'vue': [
    {
      text: 'vue',
      collapsed: false,
      link: '/web/vue/index'
    },
    {
      text: 'vue的其他姿势',
      collapsed: false,
      items: [
        {text: 'vue中的tsx', link: '/web/vue/posture/useTsx'}
      ]
    },
    {
      text: 'vue版本更新',
      collapsed: false,
      items: [
        {text: '3.5更新', link: '/web/vue/version/version3.5'}
      ]
    },
    {
      text: 'vue组件',
      collapsed: false,
      items: [
        {text: '大文件上传组件', link: '/web/vue/components/chunkUpload'},
        {text: 'loading组件及用法', link: '/web/vue/components/loading'}
      ]
    }
  ]
}
