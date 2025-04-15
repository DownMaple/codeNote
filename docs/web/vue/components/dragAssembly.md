本文简单介绍 怎么制作一个可以将内部元素像图片一样拖拽查看，通过鼠标滚轮可以放大缩小的组件。

祖传开篇：作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

环境要求：

## 基本介绍

在 Vue 应用中，我们经常需要实现一些交互性强的 UI 组件，比如可拖拽、可缩放的容器。本文将介绍如何使用 Vue 3 和 TypeScript
实现一个功能相对完善的可拖拽缩放组件

### 特点

- 按住空格键可以拖动内容
- 支持鼠标滚轮缩放
- 拖动和缩放时内部元素不可操作
- 平滑的动画效果
- 支持自定义控制面板

### 环境要求

* node 18+
* vue 3.5+
* typescript 5.5+
* vite 5.0+

以上是虚假的要求，真实的要求：无

## 组件的基本结构

```vue

<template>
  <div ref="dragContainer" class="my-drag">
    <!-- 控制面板 -->
    <div class="drag-controls">
      <button>重置视图</button>
      <div class="drag-info">提示信息</div>
    </div>
    <!-- 内容容器 -->
    <div ref="contentWrapper" class="content-wrapper">
      <slot></slot>
    </div>
  </div>
</template>
```

组件由两个主要部分组成：

1. 控制面板：包含重置按钮和提示信息
2. 内容容器：通过插槽接收用户传入的内容

## 状态管理

使用 reactive 管理组件的状态：

```ts
const state = reactive({
  isSpacePressed: false,  // 空格键是否按下
  isDragging: false,      // 是否正在拖动
  startX: 0,              // 拖动起始点X坐标
  startY: 0,              // 拖动起始点Y坐标
  translateX: 0,          // X轴平移量
  translateY: 0,          // Y轴平移量
  scale: 1,               // 缩放比例
  minScale: 0.5,          // 最小缩放比例
  maxScale: 3,            // 最大缩放比例
  animationFrameId: 0,    // 动画帧ID
  currentX: 0,            // 当前鼠标X坐标
  currentY: 0,            // 当前鼠标Y坐标
})
```

## 键盘事件处理

实现空格键按下和释放的事件处理：

```ts
// 监听键盘事件
function handleKeyDown(e: KeyboardEvent) {
  if (e.code === 'Space' && !state.isSpacePressed && isMouseInContainer.value) {
    e.preventDefault()
    state.isSpacePressed = true
    if (dragContainer.value) {
      dragContainer.value.style.cursor = 'grab'
    }
  }
}

function handleKeyUp(e: KeyboardEvent) {
  if (e.code === 'Space') {
    e.preventDefault()
    state.isSpacePressed = false
    if (dragContainer.value) {
      dragContainer.value.style.cursor = 'default'
    }
    if (state.isDragging) {
      state.isDragging = false
      // 取消动画帧请求
      if (state.animationFrameId) {
        cancelAnimationFrame(state.animationFrameId)
        state.animationFrameId = 0
      }
    }
  }
}
```

### 具体实现原理

1. 空格键按下时，设置 isSpacePressed 为 true，并设置拖动容器的 cursor 为 grab，表示可以拖动。
2. 当空格键被按下时，组件会给内容容器添加一个特殊的 CSS 类 no-events
3. pointer-events: none 的作用：
    - 使元素及其子元素不再接收任何鼠标事件（点击、悬停、拖拽等）
    - 鼠标事件会"穿透"这个元素，被下层元素捕获
    - 元素视觉上仍然可见，但无法与之交互
4. 当空格键按下且用户点击鼠标时，由于内部元素已经不接收鼠标事件，事件会被外层容器捕获

## 鼠标事件处理

### 拖拽实现

```ts
// 当鼠标按下时
function handleMouseDown(e: MouseEvent) {
  if (state.isSpacePressed) {
    e.preventDefault()
    state.isDragging = true
    state.currentX = e.clientX
    state.currentY = e.clientY
    state.startX = e.clientX - state.translateX
    state.startY = e.clientY - state.translateY
    if (dragContainer.value) {
      dragContainer.value.style.cursor = 'grabbing'
    }
    
    // 开始动画循环
    if (state.animationFrameId) {
      cancelAnimationFrame(state.animationFrameId)
    }
    state.animationFrameId = requestAnimationFrame(updateDragAnimation)
  }
}

// 当鼠标意动时
function handleMouseMove(e: MouseEvent) {
  if (state.isDragging && state.isSpacePressed) {
    e.preventDefault()
    // 只更新当前鼠标位置，实际变换在动画帧中处理
    state.currentX = e.clientX
    state.currentY = e.clientY
  }
}

// 当松开鼠标按键时
function handleMouseUp() {
  if (state.isDragging) {
    state.isDragging = false
    if (dragContainer.value) {
      dragContainer.value.style.cursor = state.isSpacePressed ? 'grab' : 'default'
    }
    
    // 取消动画帧请求
    if (state.animationFrameId) {
      cancelAnimationFrame(state.animationFrameId)
      state.animationFrameId = 0
    }
  }
}
```

### 动画更新

使用 requestAnimationFrame 实现平滑动画：

```ts
// 动画帧更新函数
function updateDragAnimation() {
  if (state.isDragging) {
    // 添加防抖动逻辑
    const newX = state.currentX - state.startX
    const newY = state.currentY - state.startY
    
    // 只有当位置变化超过阈值时才更新
    if (Math.abs(newX - state.translateX) > 0.5 || Math.abs(newY - state.translateY) > 0.5) {
      state.translateX = newX
      state.translateY = newY
      updateTransform()
    }
    
    state.animationFrameId = requestAnimationFrame(updateDragAnimation)
  }
}
```

## 滚轮缩放实现

```ts
// 处理滚轮缩放
function handleWheel(e: WheelEvent) {
  if (state.isSpacePressed) {
    e.preventDefault()
    const delta = e.deltaY > 0 ? -0.1 : 0.1
    const newScale = Math.max(state.minScale, Math.min(state.maxScale, state.scale + delta))
    
    // 计算鼠标位置相对于容器的偏移
    const rect = dragContainer.value?.getBoundingClientRect()
    if (!rect) return
    
    const mouseX = e.clientX - rect.left
    const mouseY = e.clientY - rect.top
    
    // 调整缩放中心点为鼠标位置
    const scaleRatio = newScale / state.scale
    const dx = mouseX - mouseX * scaleRatio
    const dy = mouseY - mouseY * scaleRatio
    
    state.translateX = state.translateX * scaleRatio + dx
    state.translateY = state.translateY * scaleRatio + dy
    state.scale = newScale
    
    // 使用 requestAnimationFrame 更新变换
    requestAnimationFrame(() => {
      updateTransform()
    })
  }
}
```

## 重置与更新变换

```ts
// 更新变换
function updateTransform() {
  if (contentWrapper.value) {
    contentWrapper.value.style.transform = `translate3d(${state.translateX}px, ${state.translateY}px, 0) scale(${state.scale})`
  }
}

// 重置变换
function resetTransform() {
  if (contentWrapper.value) {
    contentWrapper.value.style.transition = 'transform 0.3s ease-out'
    
    requestAnimationFrame(() => {
      state.translateX = 0
      state.translateY = 0
      state.scale = 1
      updateTransform()
      
      setTimeout(() => {
        if (contentWrapper.value) {
          contentWrapper.value.style.transition = ''
        }
      }, 300)
    })
  }
}
```

## 注册和移除事件

```ts
onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('keyup', handleKeyUp)
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  
  // 初始化时应用硬件加速并确保初始位置正确
  if (contentWrapper.value) {
    contentWrapper.value.style.willChange = 'transform'
    contentWrapper.value.style.transform = 'translate3d(0, 0, 0) scale(1)'
  }
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('keyup', handleKeyUp)
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  
  if (state.animationFrameId) {
    cancelAnimationFrame(state.animationFrameId)
  }
})
```

## 完整代码

最后将整合到一起，再加入一些可以自定义的插槽，补充一些样式。得到完整的代码：

```vue

<template>
  <div
      ref="dragContainer"
      class="my-drag"
      @mousedown="handleMouseDown"
      @wheel="handleWheel"
      @mouseenter="handleMouseEnter"
      @mouseleave="handleMouseLeave"
  >
    <div class="drag-controls" v-if="isTip">
      <slot name="tip">
        <button @click="resetTransform" class="reset-btn">重置视图</button>
        <div class="drag-info">按住空格键 + 鼠标拖动查看，滚轮缩放</div>
      </slot>
    </div>
    <div ref="contentWrapper" class="content-wrapper" :class="{ 'no-events': state.isSpacePressed }">
      <slot></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
  import {ref, onMounted, onUnmounted, reactive} from 'vue'

  const dragContainer = ref<HTMLElement | null>(null)
  const contentWrapper = ref<HTMLElement | null>(null)
  const isMouseInContainer = ref(false) // 新增：跟踪鼠标是否在容器内

  const {isTip} = defineProps({
    isTip: {
      type: Boolean,
      default: true,
    },
  })

  const state = reactive({
    isSpacePressed: false,
    isDragging: false,
    startX: 0,
    startY: 0,
    translateX: 0,
    translateY: 0,
    scale: 1,
    minScale: 0.5,
    maxScale: 3,
    // 新增：动画帧请求ID
    animationFrameId: 0,
    // 新增：当前鼠标位置
    currentX: 0,
    currentY: 0,
  })

  // 新增：鼠标进入容器事件
  function handleMouseEnter() {
    isMouseInContainer.value = true
  }

  // 新增：鼠标离开容器事件
  function handleMouseLeave() {
    isMouseInContainer.value = false
    // 如果鼠标离开容器，确保重置状态
    if (state.isSpacePressed) {
      state.isSpacePressed = false
      state.isDragging = false
      if (dragContainer.value) {
        dragContainer.value.style.cursor = 'default'
      }
    }
  }

  // 监听键盘事件
  function handleKeyDown(e: KeyboardEvent) {
    if (e.code === 'Space' && !state.isSpacePressed && isMouseInContainer.value) {
      e.preventDefault()
      state.isSpacePressed = true
      if (dragContainer.value) {
        dragContainer.value.style.cursor = 'grab'
      }
    }
  }

  function handleKeyUp(e: KeyboardEvent) {
    if (e.code === 'Space') {
      e.preventDefault()
      state.isSpacePressed = false
      if (dragContainer.value) {
        dragContainer.value.style.cursor = 'default'
      }
      if (state.isDragging) {
        state.isDragging = false
        // 取消动画帧请求
        if (state.animationFrameId) {
          cancelAnimationFrame(state.animationFrameId)
          state.animationFrameId = 0
        }
      }
    }
  }

  // 处理鼠标事件
  function handleMouseDown(e: MouseEvent) {
    if (state.isSpacePressed) {
      e.preventDefault()
      state.isDragging = true
      state.currentX = e.clientX // 先设置当前位置
      state.currentY = e.clientY
      state.startX = e.clientX - state.translateX
      state.startY = e.clientY - state.translateY
      if (dragContainer.value) {
        dragContainer.value.style.cursor = 'grabbing'
      }

      // 开始动画循环
      if (state.animationFrameId) {
        cancelAnimationFrame(state.animationFrameId)
      }
      state.animationFrameId = requestAnimationFrame(updateDragAnimation)
    }
  }

  function handleMouseMove(e: MouseEvent) {
    if (state.isDragging && state.isSpacePressed) {
      e.preventDefault()
      state.currentX = e.clientX
      state.currentY = e.clientY
    }
  }

  // 新增：动画帧更新函数
  function updateDragAnimation() {
    if (state.isDragging) {
      // 添加防抖动逻辑
      const newX = state.currentX - state.startX
      const newY = state.currentY - state.startY

      // 只有当位置变化超过阈值时才更新
      if (Math.abs(newX - state.translateX) > 0.5 || Math.abs(newY - state.translateY) > 0.5) {
        state.translateX = newX
        state.translateY = newY
        updateTransform()
      }

      state.animationFrameId = requestAnimationFrame(updateDragAnimation)
    }
  }

  function handleMouseUp() {
    if (state.isDragging) {
      state.isDragging = false
      if (dragContainer.value) {
        dragContainer.value.style.cursor = state.isSpacePressed ? 'grab' : 'default'
      }

      // 取消动画帧请求
      if (state.animationFrameId) {
        cancelAnimationFrame(state.animationFrameId)
        state.animationFrameId = 0
      }
    }
  }

  // 处理滚轮缩放
  function handleWheel(e: WheelEvent) {
    if (state.isSpacePressed) {
      e.preventDefault()
      const delta = e.deltaY > 0 ? -0.1 : 0.1
      const newScale = Math.max(state.minScale, Math.min(state.maxScale, state.scale + delta))

      // 计算鼠标位置相对于容器的偏移
      const rect = dragContainer.value?.getBoundingClientRect()
      if (!rect) return

      const mouseX = e.clientX - rect.left
      const mouseY = e.clientY - rect.top

      // 调整缩放中心点为鼠标位置
      const scaleRatio = newScale / state.scale
      const dx = mouseX - mouseX * scaleRatio
      const dy = mouseY - mouseY * scaleRatio

      state.translateX = state.translateX * scaleRatio + dx
      state.translateY = state.translateY * scaleRatio + dy
      state.scale = newScale

      // 使用 requestAnimationFrame 更新变换
      requestAnimationFrame(() => {
        updateTransform()
      })
    }
  }

  // 更新变换
  function updateTransform() {
    if (contentWrapper.value) {
      // 使用 translate3d 启用硬件加速
      contentWrapper.value.style.transform = `translate3d(${state.translateX}px, ${state.translateY}px, 0) scale(${state.scale})`
    }
  }

  // 重置变换
  function resetTransform() {
    // 添加平滑过渡效果
    if (contentWrapper.value) {
      contentWrapper.value.style.transition = 'transform 0.3s ease-out'

      // 在下一帧应用变换
      requestAnimationFrame(() => {
        state.translateX = 0
        state.translateY = 0
        state.scale = 1
        updateTransform()

        // 重置完成后移除过渡效果
        setTimeout(() => {
          if (contentWrapper.value) {
            contentWrapper.value.style.transition = ''
          }
        }, 300)
      })
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', handleKeyDown)
    window.addEventListener('keyup', handleKeyUp)
    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)

    // 初始化时应用硬件加速并确保初始位置正确
    if (contentWrapper.value) {
      contentWrapper.value.style.willChange = 'transform'
      // 确保初始状态下内容在可见区域
      contentWrapper.value.style.transform = 'translate3d(0, 0, 0) scale(1)'
    }
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', handleKeyDown)
    window.removeEventListener('keyup', handleKeyUp)
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)

    // 取消可能存在的动画帧请求
    if (state.animationFrameId) {
      cancelAnimationFrame(state.animationFrameId)
    }
  })
</script>

<style scoped>
  .my-drag {
    position: relative;
    overflow: hidden;
    border: 1px solid #e0e0e0;
    border-radius: 4px;
    background-color: #f9f9f9;
    touch-action: none; /* 防止触摸设备上的默认行为 */
  }

  .drag-controls {
    position: absolute;
    bottom: 10px;
    left: 10px;
    z-index: 10;
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .reset-btn {
    padding: 4px 8px;
    background-color: #fff;
    border: 1px solid #ddd;
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
  }

  .reset-btn:hover {
    background-color: #f0f0f0;
  }

  .drag-info {
    font-size: 12px;
    color: #666;
    background-color: rgba(255, 255, 255, 0.8);
    padding: 4px 8px;
    border-radius: 4px;
  }

  .content-wrapper {
    transform-origin: 0 0;
    /* 移除过渡效果，完全由 requestAnimationFrame 控制动画 */
    /* transition: transform 0.05s ease-out; */
    position: relative;
    width: 100%;
    height: 100%;
    /* 添加硬件加速 */
    backface-visibility: hidden;
    -webkit-backface-visibility: hidden;
  }

  .no-events {
    pointer-events: none;
  }
</style>

```

## 使用用法

```vue

<MyDrag class="drag-box">
  <MyLoading @click="loading=!loading"></MyLoading>
  <button @click="globalLoadingFun">展示全局loading</button>
  <iframe src="https://downmaple.github.io/code-note/" style="width: 100%;height: 100%"></iframe>
</MyDrag>
```

## 总结

本文介绍了如何使用 Vue 3 和 TypeScript 实现一个功能完善的可拖拽缩放组件。通过合理使用
requestAnimationFrame、硬件加速等技术，我们实现了平滑的拖拽和缩放效果。同时，通过具名插槽和 props 配置，使组件具有良好的扩展性和可定制性。

这种组件在图片预览、大型数据可视化、地图应用等场景中非常有用，可以提供更好的用户交互体验。

