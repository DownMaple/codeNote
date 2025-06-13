export const otherSidebarList = {
    'engineering':[
        {
            text: '工程化',
            collapsed: false,
            link: '/other/engineering/index'
        },
        {
            text: 'VoidZero',
            collapsed: false,
            items: [
                {text: 'OxLint的基本用法', link: '/other/engineering/voidZero/OxLint'},
                {text: 'vite插件开发', link: '/other/engineering/voidZero/vitePlugins'},
            ]
        },
        {
            text: '其他构建工具',
            collapsed: false,
            items: []
        },
        {
            text: '包管理工具相关',
            collapsed: false,
            items: [
                {text: 'pnpm的Workspace', link: '/other/engineering/package/workspace'},
            ]
        },
        {
            text: 'git相关',
            collapsed: false,
            items: [
                {text: 'Submodules', link: '/other/engineering/git/Submodules'},
            ]
        }
    ]
}
