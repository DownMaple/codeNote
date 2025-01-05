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
				{text: 'url到页面展示的全过程', link: '/web/base/Browser/urlToPage'},
			]
		},
		{
			text: 'JavaScript基础',
			collapsed: false,
			items: [
				{text: '事件循环（消息循环）', link: '/web/base/JavaScript/messageLoop'},
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
			text: 'vue版本更新',
			collapsed: false,
			items: [
				{text: '3.5更新', link: '/web/vue/version3.5'},
			]
		}
	]
}
