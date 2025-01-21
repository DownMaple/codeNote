本篇笔记主要是，怎么在 vue 中自己实现一个简单的 ElementPlus 的 loading 组件和 v-loading指令。

祖传开篇：作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

环境要求：有个vue3就行。

## 实现loading组件

首先先来实现一个简单的 loading 组件，主要就是一些 css 动画还有自定义的展示文字：

```vue

<template>
  <div class="my-loading">
    <div class="my-loading-box">
      <div class="my-loading-box--item"></div>
      <div class="my-loading-box--item">></div>
      <div class="my-loading-box--item">></div>
      <div class="my-loading-box--item">></div>
    </div>
    <p class="my-loading__txt">{{ text }}</p>
  </div>
</template>

<script setup lang="ts">

  const {text, color} = defineProps({
    text: {
      type: String,
      default: '加载中···'
    },
    color: {
      type: String,
      default: '#409eff'
    }
  })

</script>
<style lang="scss" scoped>

  .my-loading {
    width: 100%;
    height: auto;
    position: relative;
    z-index: 9;
    flex: 1;
    background: transparent;
    display: flex;
    justify-content: center;
    flex-direction: column;
    align-items: center;

    &-box {
      width: 42px;
      height: 32px;
      position: relative;
      box-sizing: border-box;
      display: block;
      font-size: 0;
      color: v-bind(color);
      margin-bottom: 5px;


      &--item {
        display: inline-block;
        float: none;
        background-color: currentColor;
        border: 0 solid currentColor;


        &:nth-child(1) {
          position: absolute;
          bottom: 32%;
          left: 18%;
          width: 14px;
          height: 14px;
          border-radius: 100%;
          transform-origin: center bottom;
          animation: ball-climbing-dot-jump 0.6s ease-in-out infinite;
        }

        &:not(:nth-child(1)) {
          position: absolute;
          top: 0;
          right: 0;
          width: 14px;
          height: 2px;
          border-radius: 0;
          transform: translate(60%, 0);
          animation: ball-climbing-dot-steps 1.8s linear infinite;

        }

        &:not(:nth-child(1)):nth-child(2) {
          animation-delay: 0ms;
        }

        &:not(:nth-child(1)):nth-child(3) {
          animation-delay: -600ms;
        }

        &:not(:nth-child(1)):nth-child(4) {
          animation-delay: -1200ms;
        }

        @keyframes ball-climbing-dot-jump {
          0% {
            transform: scale(1, 0.7);
          }

          20% {
            transform: scale(0.7, 1.2);
          }

          40% {
            transform: scale(1, 1);
          }

          50% {
            bottom: 125%;
          }

          46% {
            transform: scale(1, 1);
          }

          80% {
            transform: scale(0.7, 1.2);
          }

          90% {
            transform: scale(0.7, 1.2);
          }

          100% {
            transform: scale(1, 0.7);
          }
        }

        @keyframes ball-climbing-dot-steps {
          0% {
            top: 0;
            right: 0;
            opacity: 0;
          }

          50% {
            opacity: 1;
          }

          100% {
            top: 100%;
            right: 100%;
            opacity: 0;
          }
        }

      }

    }

    &__txt {
      font-size: 14px;
      color: v-bind(color);
      margin: 0;
    }
  }
</style>

```

挺简单的吧，更多 css loading 可以参考 [https://css-loaders.com/](https://css-loaders.com/)

## 实现 v-loading 指令

[官方文档入口](https://cn.vuejs.org/guide/reusability/custom-directives.html#introduction)

除了 Vue 内置的一系列指令 (比如 v-model 或 v-show) 之外，Vue 还允许你注册自定义的指令 (Custom Directives)。

一个自定义指令由一个包含类似组件生命周期钩子的对象来定义。钩子函数会接收到指令所绑定元素作为其参数。

在 `<script setup>` 中，任何以 v 开头的驼峰式命名的变量都可以当作自定义指令使用。 例如：

```vue

<script setup>
  // 在模板中启用 v-highlight
  const vHighlight = {
    mounted: (el) => {
      el.classList.add('is-highlight')
    }
  }
</script>

<template>
  <p v-highlight>This sentence is important!</p>
</template>
```

在不使用 `<script setup>` 的情况下，自定义指令需要通过 `directives` 选项注册

```js
export default {
  setup() {
    /*...*/
  },
  directives: {
    // 在模板中启用 v-highlight
    highlight: {
      /* ... */
    }
  }
}
```

此外，还可以 使用 `app.directive()` 将一个自定义指令全局注册到应用层级

### 指令钩子函数

```js
const myDirective = {
  // 在绑定元素的 attribute 前
  // 或事件监听器应用前调用
  created(el, binding, vnode) {
    // 下面会介绍各个参数的细节
  },
  // 在元素被插入到 DOM 前调用
  beforeMount(el, binding, vnode) {
  },
  // 在绑定元素的父组件
  // 及他自己的所有子节点都挂载完成后调用
  mounted(el, binding, vnode) {
  },
  // 绑定元素的父组件更新前调用
  beforeUpdate(el, binding, vnode, prevVnode) {
  },
  // 在绑定元素的父组件
  // 及他自己的所有子节点都更新后调用
  updated(el, binding, vnode, prevVnode) {
  },
  // 绑定元素的父组件卸载前调用
  beforeUnmount(el, binding, vnode) {
  },
  // 绑定元素的父组件卸载后调用
  unmounted(el, binding, vnode) {
  }
}
```

### 实现 v-my-loading

```ts
import myLoading from './index.ts';
import {createApp, h} from 'vue';

let appInstance: any = null
const addLoading = (el: HTMLElement) => {
  // 获取元素的样式
  const computedStyle = getComputedStyle(el);
  let position = computedStyle.position
  // 判断 元素 的 position 属性，如果是 static 或者空，则设置为 relative
  if (position === 'static' || position === '') {
    el.style.position = 'relative'
  }
  // 创建 loading 组件的容器
  const loadingBox = document.createElement('div')
  // 设置 loading 组件的样式
  loadingBox.style.cssText = `
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.5);
        display: flex;
        z-index:998
        justify-content: center;
        align-items: center;
      `
  // 将 loading 组件的容器添加到元素中
  el.appendChild(loadingBox)
  
  // 通过 createApp 创建一个 Vue 应用程序，并渲染 loading 组件， 并将其挂载到 loading 组件的容器上
  createApp({
    render() {
      return h(myLoading, {color: '#fff'})
    }
  }).mount(loadingBox)
}

// 移除 loading 组件
const removeLoading = (el: HTMLElement) => {
  if (appInstance) {
    appInstance.unmount()
  }
  // 获取 loading 组件的容器
  const loadingBox = el.querySelector('div')
  if (loadingBox) {
    el.removeChild(loadingBox)
  }
}


export const VMyLoading = {
  
  // 挂载时，如果值为 true，则添加 loading 组件
  mounted(el: HTMLElement, binding: any) {
    if (binding.value) {
      addLoading(el);
    }
  },
  
  // 更新时，如果值为 true，则添加 loading 组件
  updated(el: HTMLElement, binding: any) {
    if (binding.value) {
      addLoading(el);
    } else {
      removeLoading(el);
    }
  },
  
  // 卸载时，移除 loading 组件
  unmounted(el: HTMLElement) {
    removeLoading(el);
  }
}

```

如果是在组件库内，我们可以在组件的入口文件 index.ts 中添加导出项，使指令更容易使用：

```ts
import withInstall from '../utils/withInstall'
import myLoading from './myLoading.vue'
// 引入指令并且重命名
import {VMyLoading as vMyLoading} from './VMyLoading.ts';

// 这是已经安装了的组件
const MyLoading = withInstall(myLoading)

// 导出自定义指令
export {vMyLoading}

export default MyLoading

```

这样我们就可以在模板中使用 `v-my-loading` 指令了

```vue

<script setup lang="ts">

  import {ref} from 'vue';

  // 引入自定义指令和组件
  import MyLoading, {vMyLoading} from './myLoading';

  const loading = ref(true)

</script>

<template>
  <div>
    <!--    自定义指令-->
    <div v-my-loading="loading" class="test-box">测试内容</div>
    <!--    自定义组件-->
    <MyLoading @click="loading=!loading"></MyLoading>
  </div>
</template>

<style scoped>
  .test-box {
    width: 100px;
    height: 100px;
    color: #409eff;
    background-color: #dcdfe5;
  }
</style>

```

## 制作全局 v-loading

通过Vue创建一个遮罩层，并在其中渲染自定义的加载组件。最后提供show和hide方法来控制加载指示器的显示和隐藏。

这里就和自定义指令一样，只是将loading组件挂载到全局，而不是在组件中使用。

```ts
import {createApp, h} from 'vue';
import myLoading from './index.ts';

const loadingRoot = document.createElement('div')

loadingRoot.style.cssText = `
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index:9999;
  display: flex;
  justify-content: center;
  align-items: center;
  
  `

const loadingVue = createApp({
  render() {
    return h(myLoading, {color: '#fff'})
  }
})

loadingVue.mount(loadingRoot)

export const globalLoading = {
  show() {
    document.body.appendChild(loadingRoot)
  },
  hide() {
    document.body.removeChild(loadingRoot)
  }
}

```

这里需要注意的是，全局加载组件的挂载位置，一般建议在body标签内，这样可以保证全局加载组件的层级最高，避免被其他组件覆盖。

使用方法：

```vue

<script setup lang="ts">

  import {globalLoading} from './myLoading/globalLoading.ts';

  function globalLoadingFun() {
    globalLoading.show()
    setTimeout(() => {
      globalLoading.hide()
    }, 2000)
  }

</script>

<template>
  <div>
    <button @click="globalLoadingFun">展示全局loading</button>
  </div>
</template>


```

