# 图片标注与夹角计算组件

本篇笔记主要记录如何使用 Vue 实现一个带图片标注（画箭头）并计算标注线与垂直方向夹角的组件。

祖传开篇：作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## 需求背景

在特定的业务场景（比如对照片中的阴影、建筑进行方位角标记时），我们需要在弹窗展示的图片上进行线条标注，并求出标记角度。具体功能要求如下：

1. 弹出模态框加载超大原图，需支持滚轮 **缩放** 与鼠标 **拖拽平移**。
2. 内部通过一个固定的中心绘制区域（示例中为一个蓝框），支持该区域内利用鼠标画 **箭头形线段**。
3. **计算夹角**：需要计算绘制出的箭头与图片垂直方向（即 Y轴正向，图片中为“向上”方向）的顺时针夹角（方位角）。
4. 在绘制后，支持选中箭头、删除箭头，或者清空全部箭头，最终计算多条线段的夹角平均值暴露给外部组件。

## 核心实现思路

1. **图片适应与容器变换**：图片原始尺寸通常不固定，我们获取原图物理宽高 `imgNaturalWidth` 与 `imgNaturalHeight` 作为底图大小，并利用 CSS 的 `transform: translate(x, y) scale(s)` 来控制整个图片层和绘制层的缩放、平移。
2. **绘制系统（基于 SVG）**：将一个自适应的 `<svg>` 节点盖在图片上，其 `viewBox` 一比一绑定原图宽高；利用 SVG 的 `<line>` 标签以及带箭头的 `<marker>` 定义图例。
3. **坐标系转换（难点）**：用户的每次点击与拖动发生在屏幕上，拥有的是屏幕客户端坐标（`clientX`，`clientY`）。为了在 `<svg>` 内准确画对位置，必须将屏幕坐标“逆变换”为原图片的内部坐标：
   ```javascript
   imgX = (mouseX - translateX) / scale
   imgY = (mouseY - translateY) / scale
   ```
4. **夹角计算算法**：在计算机默认图片坐标系中，原点在左上角，`X轴`向右为正，`Y轴`向下为正。而在真实的视觉中，“向上”（图片顶部）才是我们业务规定的基准方向 0 度。利用数学反正切函数 `Math.atan2(dx, -dy)` 可得到顺时针夹角。

## 完整代码实现

以下提供了一个完整的 Vue 组件。组件内引入了部分 Ant Design Vue 的基础结构（`<a-modal>`、`<a-button>` 等），可随时替换为 Element UI，或其它组件库。

```vue
<template>
	<a-modal
		:visible="visible"
		:footer="null"
		:closable="false"
		:destroyOnClose="true"
		:maskClosable="false"
		width="80vw"
		:bodyStyle="{ padding: 0, height: '80vh', overflow: 'hidden', background: '#f2f2f2' }"
		@cancel="handleClose"
	>
		<div class="shadow-annotation-container" ref="container">
			<!-- 图片+蓝框+箭头画布 -->
			<div
				class="canvas-wrapper"
				ref="canvasWrapper"
				:style="{ cursor: currentCursor }"
				@wheel.prevent="handleWheel"
				@mousedown="handleWrapperMouseDown"
				@mousemove="handleWrapperMouseMove"
				@mouseup="handleWrapperMouseUp"
				@mouseleave="handleWrapperMouseUp"
			>
				<div class="image-layer" ref="imageLayer" :style="imageLayerStyle">
					<img ref="annotationImg" :src="imgSrc" class="annotation-img" draggable="false" @load="onImageLoad" />
					<!-- 蓝色框 -->
					<div class="blue-rect" :style="blueRectStyle"></div>
					<!-- SVG 箭头层 -->
					<svg class="arrow-svg" :viewBox="`0 0 ${imgNaturalWidth} ${imgNaturalHeight}`">
						<defs>
							<marker
								v-for="(arrow, idx) in arrows"
								:key="'marker-' + idx"
								:id="'arrowhead-' + idx"
								markerWidth="10"
								markerHeight="7"
								refX="10"
								refY="3.5"
								orient="auto"
							>
								<polygon points="0 0, 10 3.5, 0 7" fill="red" />
							</marker>
						</defs>
						<!-- 已有箭头 -->
						<g v-for="(arrow, idx) in arrows" :key="'arrow-' + idx">
							<line
								:x1="arrow.startX"
								:y1="arrow.startY"
								:x2="arrow.endX"
								:y2="arrow.endY"
								stroke="red"
								:stroke-width="3 / scale"
								:marker-end="'url(#arrowhead-' + idx + ')'"
								style="cursor: pointer"
								@mousedown.stop="onArrowClick(idx, $event)"
							/>
							<!-- 删除按钮 -->
							<g
								v-if="selectedArrowIndex === idx"
								:transform="`translate(${(arrow.startX + arrow.endX) / 2}, ${(arrow.startY + arrow.endY) / 2})`"
								style="cursor: pointer"
								@mousedown.stop="deleteArrow(idx)"
							>
								<circle r="10" fill="white" stroke="red" :stroke-width="1.5 / scale" />
								<text
									text-anchor="middle"
									dominant-baseline="central"
									fill="red"
									:font-size="12 / scale"
									font-weight="bold"
									style="pointer-events: none"
								>
									✕
								</text>
							</g>
						</g>
						<!-- 正在绘制的箭头 -->
						<line
							v-if="isDrawing && drawStart && drawEnd"
							:x1="drawStart.x"
							:y1="drawStart.y"
							:x2="drawEnd.x"
							:y2="drawEnd.y"
							stroke="red"
							:stroke-width="3 / scale"
							marker-end="url(#arrowhead-drawing)"
						/>
						<defs v-if="isDrawing">
							<marker id="arrowhead-drawing" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
								<polygon points="0 0, 10 3.5, 0 7" fill="red" />
							</marker>
						</defs>
					</svg>
					<!-- 提示文字 -->
					<div class="hint-text" v-if="arrows.length === 0 && !isDrawing">请在蓝框中选择影子</div>
				</div>
			</div>
			<!-- 底部操作栏 -->
			<div class="action-bar">
				<div class="action-buttons">
					<a-button type="primary" @click="handleConfirm">确认</a-button>
					<a-button style="margin-left: 8px" @click="handleClose">取消</a-button>
					<a-button type="danger" style="margin-left: 8px" @click="clearArrows">清空</a-button>
				</div>
			</div>
		</div>
	</a-modal>
</template>
<script>
export default {
	name: 'ShadowAnnotation',
	props: {
		visible: { type: Boolean, default: false },
		imgSrc: { type: String, default: '' },
		snapTime: { type: String, default: '' },
		longitude: { type: [Number, String], default: null },
		latitude: { type: [Number, String], default: null },
	},
	data() {
		return {
			imgNaturalWidth: 1920,
			imgNaturalHeight: 1080,
			scale: 1,
			translateX: 0,
			translateY: 0,
			isDragging: false,
			dragStartX: 0,
			dragStartY: 0,
			dragStartTranslateX: 0,
			dragStartTranslateY: 0,
			isDrawing: false,
			drawStart: null,
			drawEnd: null,
			arrows: [],
			selectedArrowIndex: -1,
			currentCursor: 'crosshair',
		}
	},
	computed: {
		imageLayerStyle() {
			return {
				transform: `translate(${this.translateX}px, ${this.translateY}px) scale(${this.scale})`,
				transformOrigin: '0 0',
				width: this.imgNaturalWidth + 'px',
				height: this.imgNaturalHeight + 'px',
			}
		},
		blueRectStyle() {
			const left = this.imgNaturalWidth * 0.12
			const top = this.imgNaturalHeight * 0.12
			const width = this.imgNaturalWidth * 0.76
			const height = this.imgNaturalHeight * 0.76
			return {
				left: left + 'px',
				top: top + 'px',
				width: width + 'px',
				height: height + 'px',
			}
		},
	},
	watch: {
		visible(val) {
			if (val) this.resetState()
		},
	},
	methods: {
		resetState() {
			this.scale = 1
			this.translateX = 0
			this.translateY = 0
			this.arrows = []
			this.selectedArrowIndex = -1
			this.isDrawing = false
			this.drawStart = null
			this.drawEnd = null
		},
		onImageLoad() {
			const img = this.$refs.annotationImg
			if (img) {
				this.imgNaturalWidth = img.naturalWidth || 1920
				this.imgNaturalHeight = img.naturalHeight || 1080
				this.$nextTick(() => {
					this.fitImageToContainer()
				})
			}
		},
		fitImageToContainer() {
			const wrapper = this.$refs.canvasWrapper
			if (!wrapper) return
			const wrapperW = wrapper.clientWidth
			const wrapperH = wrapper.clientHeight
			const scaleX = wrapperW / this.imgNaturalWidth
			const scaleY = wrapperH / this.imgNaturalHeight
			this.scale = Math.min(scaleX, scaleY, 1)
			this.translateX = (wrapperW - this.imgNaturalWidth * this.scale) / 2
			this.translateY = (wrapperH - this.imgNaturalHeight * this.scale) / 2
		},
		handleWheel(e) {
			const delta = e.deltaY > 0 ? -0.1 : 0.1
			const newScale = Math.max(0.1, Math.min(5, this.scale + delta))
			const wrapper = this.$refs.canvasWrapper
			if (!wrapper) return
			const rect = wrapper.getBoundingClientRect()
			const mouseX = e.clientX - rect.left
			const mouseY = e.clientY - rect.top
			const imgX = (mouseX - this.translateX) / this.scale
			const imgY = (mouseY - this.translateY) / this.scale
			this.scale = newScale
			this.translateX = mouseX - imgX * newScale
			this.translateY = mouseY - imgY * newScale
		},
		screenToImageCoords(clientX, clientY) {
			const wrapper = this.$refs.canvasWrapper
			if (!wrapper) return { x: 0, y: 0 }
			const rect = wrapper.getBoundingClientRect()
			const mouseX = clientX - rect.left
			const mouseY = clientY - rect.top
			const imgX = (mouseX - this.translateX) / this.scale
			const imgY = (mouseY - this.translateY) / this.scale
			return { x: imgX, y: imgY }
		},
		isInBlueRect(x, y) {
			const left = this.imgNaturalWidth * 0.12
			const top = this.imgNaturalHeight * 0.12
			const right = this.imgNaturalWidth * 0.88
			const bottom = this.imgNaturalHeight * 0.88
			return x >= left && x <= right && y >= top && y <= bottom
		},
		clampToBlueRect(x, y) {
			const left = this.imgNaturalWidth * 0.12
			const top = this.imgNaturalHeight * 0.12
			const right = this.imgNaturalWidth * 0.88
			const bottom = this.imgNaturalHeight * 0.88
			return {
				x: Math.max(left, Math.min(right, x)),
				y: Math.max(top, Math.min(bottom, y)),
			}
		},
		handleWrapperMouseDown(e) {
			if (e.button !== 0) return
			const imgCoords = this.screenToImageCoords(e.clientX, e.clientY)
			if (this.isInBlueRect(imgCoords.x, imgCoords.y)) {
				this.selectedArrowIndex = -1
				this.isDrawing = true
				this.drawStart = { x: imgCoords.x, y: imgCoords.y }
				this.drawEnd = { x: imgCoords.x, y: imgCoords.y }
			} else {
				this.selectedArrowIndex = -1
				this.isDragging = true
				this.dragStartX = e.clientX
				this.dragStartY = e.clientY
				this.dragStartTranslateX = this.translateX
				this.dragStartTranslateY = this.translateY
			}
		},
		handleWrapperMouseMove(e) {
			const imgCoords = this.screenToImageCoords(e.clientX, e.clientY)
			if (this.isInBlueRect(imgCoords.x, imgCoords.y)) {
				this.currentCursor = 'crosshair'
			} else {
				this.currentCursor = 'move'
			}

			if (this.isDrawing && this.drawStart) {
				const clamped = this.clampToBlueRect(imgCoords.x, imgCoords.y)
				this.drawEnd = { x: clamped.x, y: clamped.y }
			} else if (this.isDragging) {
				const dx = e.clientX - this.dragStartX
				const dy = e.clientY - this.dragStartY
				this.translateX = this.dragStartTranslateX + dx
				this.translateY = this.dragStartTranslateY + dy
			}
		},
		handleWrapperMouseUp(e) {
			if (this.isDrawing && this.drawStart && this.drawEnd) {
				const dx = this.drawEnd.x - this.drawStart.x
				const dy = this.drawEnd.y - this.drawStart.y
				const len = Math.sqrt(dx * dx + dy * dy)
				if (len > 5) {
					this.arrows.push({
						startX: this.drawStart.x,
						startY: this.drawStart.y,
						endX: this.drawEnd.x,
						endY: this.drawEnd.y,
					})
				}
				this.isDrawing = false
				this.drawStart = null
				this.drawEnd = null
			}
			this.isDragging = false
		},
		onArrowClick(idx, e) {
			e.stopPropagation()
			this.selectedArrowIndex = this.selectedArrowIndex === idx ? -1 : idx
		},
		deleteArrow(idx) {
			this.arrows.splice(idx, 1)
			this.selectedArrowIndex = -1
		},
		clearArrows() {
			this.arrows = []
			this.selectedArrowIndex = -1
		},
		calcAzimuth(arrow) {
			const dx = arrow.endX - arrow.startX
			const dy = arrow.endY - arrow.startY
			let angle = Math.atan2(dx, -dy) * (180 / Math.PI)
			if (angle < 0) angle += 360
			return Math.round(angle * 100) / 100
		},
		handleConfirm() {
			if (this.arrows.length === 0) {
				this.$message.warning('请至少绘制一个箭头')
				return
			}
			const arrowData = this.arrows.map((arrow) => {
				const azimuth = this.calcAzimuth(arrow)
				return { ...arrow, azimuth, direction: '' }
			})
			const azimuthValues = arrowData.map((item) => item.azimuth)
			const averageAzimuth =
				azimuthValues.length > 0 ? azimuthValues.reduce((sum, val) => sum + val, 0) / azimuthValues.length : 0
			this.$emit('confirm', {
				azimuth: averageAzimuth,
				arrows: arrowData,
				snapTime: this.snapTime,
				longitude: this.longitude,
				latitude: this.latitude,
			})
		},
		handleClose() {
			this.$emit('close')
		},
	},
}
</script>
<style scoped lang="less">
.shadow-annotation-container {
	width: 100%;
	height: 100%;
	display: flex;
	flex-direction: column;
	background: #1a1a1a;
	position: relative;
}

.canvas-wrapper {
	flex: 1;
	overflow: hidden;
	position: relative;
	user-select: none;
}

.image-layer {
	position: absolute;
	top: 0;
	left: 0;
}

.annotation-img {
	width: 100%;
	height: 100%;
	display: block;
	user-select: none;
	-webkit-user-drag: none;
	cursor: move;
}

.blue-rect {
	position: absolute;
	border: 2px solid #0603ff;
	box-sizing: border-box;
	pointer-events: none;
	z-index: 2;
	cursor: crosshair;
	border-radius: 2px;
}

.arrow-svg {
	position: absolute;
	top: 0;
	left: 0;
	width: 100%;
	height: 100%;
	z-index: 3;
}

.hint-text {
	position: absolute;
	top: 18%;
	left: 50%;
	transform: translateX(-50%);
	color: #0603ff;
	font-size: 18px;
	pointer-events: none;
	z-index: 4;
	text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8);
}

.action-bar {
	position: absolute;
	right: 0;
	bottom: 0;
	height: 50px;
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 0 20px;
}
</style>
```

## 细节补充解析

### 1. 缩放平移与防抖 (`handleWheel`)

为了保障图片放大时的中心依旧在视野核心，所以在监听到 `@wheel` 时不仅获取到了此时坐标下 `(-0.1 / +0.1)` 步长的新 `scale`。同时需获取到当前鼠标处于原图实际的 `imgX` 与 `imgY` 坐标。从而在最后赋值计算时反向偏置，实现鼠标定位的无缝缩放：

```javascript
// 核心偏置逻辑
this.translateX = mouseX - imgX * newScale
this.translateY = mouseY - imgY * newScale
```

### 2. SVG 画线箭头标记

借助原生的 SVG 特性，只需要提供起点（`x1`, `y1`）以及终点坐标（`x2`, `y2`），并将 `viewBox` 与由于图片放大而放大后的缩放比例脱钩同步（`stroke-width="3 / scale"`），这样无论怎么缩放都能保证画线箭头大小符合常理。配合箭头配置：`<marker marker-end="url(#arrowhead)"></marker>` 生成指向明确的标线。

### 3. 多绘制并返回平均方位角

在用户调用 `handleConfirm` 时将所有标线的方向角归总，借取数组特性进行平均值分摊：

```javascript
const sum = azimuthValues.reduce((sum, val) => sum + val, 0)
const averageAzimuth = sum / azimuthValues.length
```

最后调用 `$emit` 将这个夹角传送出去完成后续业务逻辑。
