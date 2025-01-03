export const otherSidebarList = {
    'engineering':[
        {
            text: '工程化',
            collapsed: false,
            link: '/other/engineering/index'
        },
        {
            text: '构建工具相关',
            collapsed: false,
            items: [
                {text: 'vite插件开发', link: '/other/engineering/buildTools/vitePlugins'},
            ]
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
