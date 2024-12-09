export const serviceSidebarList = {
	'rust':[
		{
			text: 'rust',
			collapsed: false,
			link: '/service/rust/index'
		},
		{
			text: 'web服务端',
			collapsed: false,
			items: [
				{text: 'rocket实现断点续传', link: '/service/rust/Breakpoint'},
			]
		}
	],
	'database':[
		{
			text: '数据库相关',
			collapsed: false,
			link: '/service/database/index'
		},
		{
			text: 'sql',
			collapsed: false,
			items: [
				{text: '关联查询', link: '/service/database/AssociatedQuery'},
			]
		}
	]
}
