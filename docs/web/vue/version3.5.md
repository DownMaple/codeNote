# 在 2024年9月1日 Vue 3.5 发布

### 原文地址：https://blog.vuejs.org/posts/vue-3-5

## 响应式系统优化（以下是原文翻译）

在 3.5 中，Vue 的响应性系统经历了另一次重大重构，实现了更好的性能并显着提高了内存使用率 （-56%），而行为没有变化。此重构还解决了在
SSR 期间因挂起计算而导致的过时计算值和内存问题。

此外，3.5 还优化了大型深度响应式数组的响应性跟踪，在某些情况下使此类操作的速度提高了 10 倍。

详细信息：[PR#10397](https://github.com/vuejs/core/pull/10397)、[PR#9511](https://github.com/vuejs/core/pull/9511)

## Reactive Props 解构

在之前的版本中，我们想要在js中访问prop必须要这样写：props.name，否则name将会丢失响应式，下面是3.5之前的写法：

```vue

<template>
  <div>
    <h3>props的值 :{{ props.msg }}</h3>
  </div>
</template>

<script setup lang="ts">
  import {watchEffect, watch} from "vue";

  const props = defineProps({
    msg: String
  })

  watchEffect(() => {
    console.log('watchEffect', props.msg)
  })
  // or
  watch(
      () => props.msg,
      (newValue, oldValue) => {
        console.log('watch', newValue, oldValue)
      }
  )
</script>

<style scoped>

</style>
```

而在新版本中，我们可以直接 结构 defineProps

```vue

<template>
  <div>
    <h3>props的值(新版本) :{{ msg }}</h3>
  </div>
</template>

<script setup lang="ts">

  import {watch, watchEffect} from "vue";

  const {msg} = defineProps({
    msg: String
  })


  // 还能在这里直接 设置一个默认值
  // const { msg = 'hello1232'} = defineProps({
  //   msg: String
  // })
  // 还有ts类型写法
  // const { msg = 'hello' } = defineProps<{ msg?: string }>()


  watchEffect(() => {
    console.log('watchEffect', msg)
  })
  // or
  watch(
      () => msg,
      (newValue, oldValue) => {
        console.log('watch', newValue, oldValue)
      })
</script>

<style scoped>

</style>
```

[官方文档](https://vuejs.org/guide/components/props.html#reactive-props-destructure)

## userId() API

可用于生成每个应用程序的唯一 ID，这些 ID 保证在服务器和客户端渲染中保持稳定。
它们可用于生成表单元素和辅助功能属性的 ID，并且可以在 SSR 应用程序中使用，而不会导致激活不匹配

::: code-group

```vue

<template>
  <div>
    <TestOld :msg="appMsg" :id="idOld"></TestOld>
    <input class="input" type="text" v-model="appMsg">
    <p>新的版本</p>
    <TestNew :msg="appMsg" :id="idNew"></TestNew>
  </div>
</template>
<script setup lang="ts">
  import {ref, useId} from "vue";
  import TestOld from "@/components/TestOld.vue";
  import TestNew from "@/components/TestNew.vue";

  const appMsg = ref('hello vue');

  const idOld = useId()
  const idNew = useId()
</script>
```

```html

<body>
<div id="app" data-v-app="">
	<div data-v-7a7a37b1="">
		<div data-v-7a7a37b1="" id="v:0"><h3>props的值 :hello vue</h3></div>
		<input data-v-7a7a37b1="" class="input" type="text">
		<p data-v-7a7a37b1="">新的版本</p>
		<div data-v-7a7a37b1="" id="v:1"><h3>props的值(新版本) :hello vue</h3></div>
	</div>
</div>
</body>
```

```vue [官方demo]

<script setup>
  import {useId} from 'vue'

  const id = useId()
</script>

<template>
  <form>
    <label :for="id">Name:</label>
    <input :id="id" type="text"/>
  </form>
</template>
```

:::

## useTemplateRef()

先看历史版本是什么功能:

```vue

<template>
  <div>
    <input type="text" ref="inputRef">
  </div>
</template>
<script setup lang="ts">
  import {onMounted, ref} from "vue";

  const inputRef = ref(null);
  onMounted(() => {
    console.log(inputRef.value)   // <input type="text">
  })
</script>
```

没错，就是用来获取 DOM 或 组件实例的
现在有了 useTemplateRef() ：

```vue

<template>
  <div>
    <input type="text" ref="inputRef">
  </div>
</template>
<script setup lang="ts">
  import {onMounted, ref, useTemplateRef} from "vue";

  const customizeRef = useTemplateRef('inputRef')   // 自定义变量
  onMounted(() => {
    console.log(customizeRef.value) // <input type="text">
  })
</script>
```

## Teleport组件新增defer延迟属性

[Teleport文档地址](https://cn.vuejs.org/guide/built-ins/teleport.html#teleport)

`<Teleport>` 是一个内置组件，它可以将一个组件内部的一部分模板“传送”到该组件的 DOM 结构外层的位置去。

```vue

<template>
  <div>
    <Teleport to="#testTarget">被传送的内容</Teleport>
    <div id="testTarget"></div>
  </div>
</template>
```

如果我这样使用 Teleport，那么被传送的内容将不会生效，并且 Vue 会警告我们：
> [!WARNING]
> Failed to locate Teleport target with selector "#testTarget". Note the target element must exist before the component
> is mounted - i.e. the target cannot be rendered by the component itself, and ideally should be outside of the entire
> Vue
> component tree.

> [!WARNING]
> Invalid Teleport target on mount: null

这时候，我们使用 defer 属性，可以延迟传送，直到组件挂载后才传送，如下：

```vue

<template>
  <div>
    <Teleport to="#testTarget" defer>被传送的内容</Teleport>
    // [!code focus]
    <div id="testTarget"></div>
  </div>
</template>
```

## onWatcherCleanup函数

在组件卸载之前或者下一次watch回调执行之前会自动调用onWatcherCleanup函数， 有了这个函数后你就不需要在组件的beforeUnmount钩子函数去统一清理一些timer了。
::: code-group

```vue [父组件]

<template>
  <div>
    <input class="input" type="text" v-model="appMsg">
    <watch-test v-if="watchState" :msg="appMsg"></watch-test>
    <button @click="watchState = false">关闭</button>
  </div>
</template>
<script setup lang="ts">
  import {ref} from "vue";
  import WatchTest from "@/components/watchTest.vue";

  const appMsg = ref('hello vue');
  const watchState = ref(true)
</script>
```

```vue [子组件]

<template>
  <div>
    {{ msg }}
  </div>
</template>
<script setup lang="ts">
  import {onWatcherCleanup, watch, watchEffect} from "vue";

  const {msg = 'hello'} = defineProps({
    msg: String
  })
  watch(
      () => msg,
      (newValue) => {
        if (newValue === 'hello1') {
          const timer = setInterval(() => {
            console.log(msg)
          }, 200)
          onWatcherCleanup(() => {
            clearInterval(timer)
          })
        }
      },
  )
</script>
```
:::
当我们 点击关闭按钮后 或者 重新出发 watch回调 后，子组件在销毁之前定时器会被清除，不会造成内存泄露。

## 新增pause和resume方法
有的场景中我们可能想在“一段时间中暂停一下”，不去执行watch或者watchEffect中的回调。等业务条件满足后再去恢复执行watch或者watchEffect中的回调
```vue
<template>
  <div>{{ msg }}</div>
  <button @click="watchEvent.pause">暂停</button>
  <button @click="watchEvent.resume">回复</button>
</template>

<script setup lang="ts">
import {watch} from "vue";

const {msg = 'hello'} = defineProps({
  msg: String
})

const watchEvent = watch(
    () => msg,
    (newValue) => {
      console.log(newValue)
    },
)
</script>
```

## watch的deep选项支持传入数字
在以前deep选项的值要么是false，要么是true，表明是否深度监听一个对象。在3.5中deep选项支持传入数字了，表明监控对象的深度。
```vue
<template>
  <div>
    <button @click="updateG">修改g的值</button>
    <button @click="updateF">修改f的值</button>
  </div>
</template>
<script setup lang="ts">
import {reactive, watch} from "vue";
const a = reactive({
  b: {
    g: 1,
    c: {
      e: 1,
      d: {
        f: 1
      }
    }
  }
})
watch(a,
    (newValue) => {
      console.log(newValue)
    },
    {
      deep: 2
    }
)
// 会触发
function updateG() {
  a.b.g = 2
}
// 不会触发 watch 侦听
function updateF() {
  a.b.c.d.f = 2
}
</script>
```

<hr/>

# SSR 相关优化

## Lazy Hydration (不懂，以下是原文内容)

异步组件现在可以通过在 API 选项中指定策略来控制何时应该进行水合。
例如，要仅在组件变得可见时水合组件： `hydrate defineAsyncComponent()`

```js
import {defineAsyncComponent, hydrateOnVisible} from 'vue';

const AsyncComp = defineAsyncComponent({
	loader: () => import('./Comp.vue'),
	hydrate: hydrateOnVisible()
})
```

核心 API 有意降低级别，Nuxt 团队已经在此功能之上构建了更高级别的语法糖

详细信息：[PR#11458](https://github.com/vuejs/core/pull/11458)

## data-allow-mismatch

如果客户端值不可避免地与其服务器对应值（例如日期）不同
我们现在可以使用属性来抑制由此产生的水合不匹配警告： `data-allow-mismatch`

```vue
<span data-allow-mismatch>{{ data.toLocaleString() }}</span>
```

您还可以通过为属性提供值来限制允许的不匹配类型, 其中可能的值为 `text children class style attribute`

## 自定义元素改进 (官方原文)

3.5 修复了许多与 API 相关的长期问题，并添加了许多使用 Vue 编写自定义元素的新功能：`defineCustomElement()`

* 通过选项支持自定义元素的应用程序配置。`configureApp`
* 添加 、 和 API 用于访问自定义元素的 host 元素和 shadow 根。`useHost() useShadowRoot() this.$host`
* 支持在没有 Shadow DOM 的情况下通过传递 `.shadowRoot: false`
* 支持提供一个选项，该选项将附加到自定义元素注入的标签上。`nonce<style>`

这些新的仅限自定义元素的选项可以通过第二个参数传递给：`defineCustomElement`

```js
import MyElement from './MyElement.ce.vue'

defineCustomElements(MyElement, {
	shadowRoot: false,
	nonce: 'xxx',
	configureApp(app) {
		app.config.errorHandler = ...
	}
})
```

[文档地址](https://cn.vuejs.org/api/general.html#definecustomelement)