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
		},
		{
			text: 'Tauri应用',
			collapsed: false,
			items: [
				{text: 'Tauri实现系统托盘', link: '/service/rust/TauriTrayIcon'},
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
