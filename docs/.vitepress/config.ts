import {navList} from "./config/navList";
import {webSidebarList} from "./config/webSidebarList";
import {serviceSidebarList} from "./config/serviceSidebarList";
import {otherSidebarList} from "./config/outerSidebarList";

// https://vitepress.dev/reference/site-config
export default {
	base: '/code-note/',
	title: 'Maple',
	description: 'maple的技术笔记本',
	cleanUrls: true,
	lang: 'zh-CN',
	// 最后更新时间配置
	lastUpdated: true,
	themeConfig: {
		siteTitle: 'Maple的笔记本',
		nav: navList,
		sidebar: {
			'/web/vue/': webSidebarList.vue,
			'/service/rust/': serviceSidebarList.rust,
			'/service/database/': serviceSidebarList.database,
			'/other/engineering/': otherSidebarList.engineering
		},
		outline: {
			label: '本页目录',
			level: [2, 3],
		},
		// 文章翻页
		docFooter: {
			prev: '上一篇',
			next: '下一篇'
		},
		// 移动端 - 外观
		darkModeSwitchLabel: '外观',
		// 移动端 - 返回顶部
		returnToTopLabel: '返回顶部',
		// 移动端 - menu
		sidebarMenuLabel: '菜单',
		lightModeSwitchTitle: '',
		search: {
			provider: 'local',
			options: {
				translations: {
					button: {
						buttonText: '搜索文档',
						buttonAriaLabel: '搜索文档'
					},
					modal: {
						noResultsText: '无法找到相关结果',
						resetButtonTitle: '清除查询条件',
						footer: {
							selectText: '选择',
							navigateText: '切换'
						}
					}
				}
			}
		}
	},
}
