export const navList = [
	{
		text: '前端',
		items: [
			{ text: 'base', link: '/web/base', activeMatch: '/web/base/' },
			{ text: 'vue', link: '/web/vue', activeMatch: '/web/vue/' },
		],
		activeMatch: '/web/'
	},
	{
		text: '后端',
		items: [
			{ text: 'node', link: '/service/node', activeMatch: '/service/node/' },
			{ text: 'rust', link: '/service/rust', activeMatch: '/service/rust/' },
			{ text: 'database', link: '/service/database', activeMatch: '/service/database/' },
		],
		activeMatch: '/service/'
	},
	{
		text: '其他',
		items: [
			{ text: '工程化', link: '/other/engineering', activeMatch: '/other/engineering/' },
		],
		activeMatch: '/other/'
	},
	{text: 'GitHub', link: 'https://github.com/DownMaple/code-note'}
]
