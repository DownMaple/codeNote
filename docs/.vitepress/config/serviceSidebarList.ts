export const serviceSidebarList = {
  'node': [
    {
      text: 'node',
      collapsed: false,
      link: '/service/node/index'
    }, {
      text: 'express',
      collapsed: false,
      items: [
        {text: '大文件切片上传', link: '/service/node/express/uploadLargeFiles'}
      ]
    }
  ],
  'rust': [
    {
      text: 'rust',
      collapsed: false,
      link: '/service/rust/index'
    },
    {
      text: 'rust原生',
      collapsed: false,
      items: [
        {text: 'rust获取系统信息', link: '/service/rust/rust/SystemInfo'}
      ]
    },
    {
      text: 'web服务端',
      collapsed: false,
      items: [
        {text: 'rocket实现断点续传', link: '/service/rust/service/Breakpoint'}
      ]
    },
    {
      text: 'Tauri应用',
      collapsed: false,
      items: [
        {text: 'Tauri实现系统托盘', link: '/service/rust/tauri/TauriTrayIcon'}
      ]
    }
  ],
  'java': [
    {
      text: 'java',
      collapsed: false,
      link: '/service/java/index'
    },
    {
      text: 'SpringBoot',
      collapsed: false,
      items: [
        {text: '大文件切片上传', link: '/service/java/SpringBoot/ideaLombokFail'}
      ]
    },
    {
      text: '其他',
      collapsed: false,
      items: [
        {text: 'Lombok使用后构建错误', link: '/service/java/other/ideaLombokFail'}
      ]
    }
  ],
  'database': [
    {
      text: '数据库相关',
      collapsed: false,
      link: '/service/database/index'
    },
    {
      text: 'sql',
      collapsed: false,
      items: [
        {text: '关联查询', link: '/service/database/sql/AssociatedQuery'}
      ]
    }
  ]
}
