## 简介 ##
本文只是介绍关于 pnpm 的 workspace 和 git Submodule 的基本用法，作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。
[官方入口](https://pnpm.io/zh/workspaces)
主要作用：
1. 统一管理多包依赖：通过一个根 pnpm-workspace.yaml 文件定义所有子包，pnpm 可以在工作空间内共享和优化依赖。
   不同包中相同的依赖只安装一次，避免重复安装，提高空间和安装速度的效率
2. 本地包的互相依赖：在工作空间内，子包可以直接通过 workspace: 引用彼此，而不需要发布到 npm。
3. 支持批量操作：作空间可以一次性对多个子包执行相同的命令，如安装依赖、运行脚本等
4. 跨包依赖版本一致性：通过根 package.json 定义通用依赖，所有子包可共享这部分配置，确保依赖版本一致。


## 创建一个 pnpm 的 Workspace ##

不多扯了，直接动手！！！

### 1、创建一个文件夹，并且初始化 pnpm ###

workspace 初始化

```bash
mkdir workspace
cd workspace
pnpm init
```
### 2、创建 pnpm-workspace.yaml 文件 ###
在workspace根目录创建，此文件会告诉 PNPM，哪些子目录应被视为工作空间的一部分
```yaml
packages:
  - 'packages/*'  // 这里可以单独指定子包，也可以使用通配符，如 packages/*
```

### 3、创建子包(这里演示用，一个公共方法，一个公共组件) ###
首先创建一个公共方法项目：
1. 在packages下新建一个文件夹：public-method
2. 然后在 public-method 下执行 pnpm init 
3. 在 public-method 下新建一个 src 文件夹，然后创建一个 index.ts 文件，内容如下：
   ```typescript
    export const randomStr = (str : string) : string => {
        return str + Math.random()
    }
    ```
4. 修改 public-method 下的 package.json 文件，主要设置 name 和 main 两个字段 如下：
    ```json
    {
      "name": "@public/method",
      "version": "1.0.0",
      "description": "",
      "main": "src/index.ts",
      "scripts": {
        "test": "echo \"Error: no test specified\" && exit 1"
      },
      "keywords": [],
      "author": "",
      "license": "ISC"
    }
    ```
目录展示：
```
workspace/
├── package.json         # 根 package.json
├── pnpm-workspace.yaml  # 工作空间配置
├── packages/
│   ├──  public-method/
│   │   ├── package.json
│   │   └── src/
│   │       └── index.ts

```

公共组件包创建步骤同上，但是内容略有不同, 目录如下：
```
workspace/
├── package.json         # 根 package.json
├── pnpm-workspace.yaml  # 工作空间配置
├── packages/
│   ├──  public-method/
│   │   ├── package.json
│   │   └── src/
│   │       └── index.ts
│   ├──  public-ui/
│   │   ├── package.json
│   │   └── src/
│   │       └── components
│   │           └── index.ts
│   │           └── input
│   │               └── index.ts
│   │               └── input.vue
│   │       └── style
│   │           └── components
│   │               └── input.scss
│   │               └── index.scss
│   │           └── index.scss
│   │       └── index.ts
```
一些主要文件：
src/index.ts
```typescript
import './style/index.scss'

export * from './components'
import * as components from './components';

const MyUIObj = {
  ...components
}

export const install = function(app) {
  Object.keys(MyUIObj).forEach(key => {
    app.component(key, MyUIObj[key]);
    // todo i-tag
  });
}
const MyUI = { install, ...components }
export default MyUI
```
src/style/index.scss
```
@use './components/index.scss';
```
src/style/components/index.scss
```
@use "./input.scss";
```
src/style/components/input.scss
```
.my-input {
  border: 1px solid blue;
}
```
src/components/index.ts
```
export { default as Input } from './input'
```
src/components/input/index.ts
```
import Input from './input.vue'
export default Input
```
src/components/input/input.vue
```
<template>
  <input class="my-input" type="text">
</template>
<script lang="ts" setup>
</script>
```

### 4、安装依赖（公共依赖、单独依赖） ###
下面先安装一下所有的包都有或者大部分的包都需要用到的依赖项, 修改 workspace的 package.json ，增加如下内容：
```json
  "dependencies": {
    "pinia": "^2.2.6",
    "sass": "^1.81.0",
    "vue": "^3.5.12",
    "vue-router": "^4.4.5"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.1.4",
    "@vitejs/plugin-vue-jsx": "^4.0.1",
    "vite": "^5.4.10",
    "vite-plugin-vue-devtools": "^7.5.4"
  }
```
直接在 workspace 目录下执行 <span style="background-color: #ffff99;padding:5px;border-radius: 5px;">pnpm install</span>
现在在 workspace 目录下，所有子包都安装好了，并且依赖版本也一致了。
接下来引入我们刚刚创建的两个公共包,依次在 workspace 目录下 执行命令 ：
```
pnpm install @public/method --workspace -w
pnpm install @public/ui --workspace -w
```
就可以看到 package.json 中增加了两个依赖：
```json
 "dependencies": {
    "@public/method": "workspace:^", 
    "@public/ui": "workspace:^",
    "pinia": "^2.2.6",
    "sass": "^1.81.0",
    "vue": "^3.5.12",
    "vue-router": "^4.4.5"
  },
```
这样我们基本框架搭建起来了，下一步就是，创建一个简单的项目，来测试一下是否能够成功引用到公共包。
### 5、创建两个简单的项目，来测试一下是否能够成功引用到公共包 ###
诶嘿，变成两个了！！
直接在 packages 目录 运行 命令：
``` pnpm create vite ``` 
搞到 名为：<span style="background-color: #ffff99;padding:5px;border-radius: 5px;">vue-one</span> 的一个普普通通的vue项目,然后再同样搞一个名为：<span style="background-color: #ffff99;padding:5px;border-radius: 5px;">vue-two</span>的项目
这样我们就有了两个 vue 项目，分别在 packages/vue-one 和 packages/vue-two 中。
修改一下两个项目的 package.json 文件，如下内容：
```json
{
  "name": "vue-two",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview"
  }
}
```
这里注意一下 pnpm-workspace.yaml 文件的 packages 字段下是否添加了 vue-one 和 vue-two。
然后我们在 workspace 目录下执行命令：``` pnpm install ```
pnpm 会自动安装所有子包的依赖，并在需要时通过符号链接关联本地包。

现在，可以不管三七二十一，直接 在将 vue-one 和 vue-two 
正常启动，没啥问题（诶嘿，我正常启动了，其他的我不到啊:smile: ）。

下一步，我们在vue-one中调用我们刚刚（谁知道是多久前的）创建的公共包
在 vue-one 的 app.vue 中添加如下代码：
```vue
<script setup lang="ts">
import HelloWorld from './components/HelloWorld.vue'
import {onMounted, ref} from "vue";
import { randomStr } from "@public/method"
import { Input as MyInput } from '@public/ui'
const str = ref('hello')
onMounted(() => {
  let s = randomStr(str.value)
  console.log(s);
})

</script>

<template>
  <my-input></my-input>
  <div>
    <a href="https://vite.dev" target="_blank">
      <img src="/vite.svg" class="logo" alt="Vite logo" />
    </a>
    <a href="https://vuejs.org/" target="_blank">
      <img src="./assets/vue.svg" class="logo vue" alt="Vue logo" />
    </a>
  </div>
  <HelloWorld msg="Vite + Vue" />
</template>
```
可能现在已经不记得 我们公共包定义的方法和组件是什么了，randomStr 方法 是一个参数是一个字符串，返回的是一个字符串拼接随机数；
MyInput 是一个输入框组件，有醒目的蓝色边框。

:clap::clap::clap::clap::clap::clap::clap::clap::clap::clap::clap::clap::clap::clap::clap::clap:

vue-two 随便装点依赖，然后测试一下单独引入依赖项是否正常。比如装一个鲁大师，然后 log 一下看看否能成功引入。

### 6、怎么打包 ###
将就直接在 vue-one 下执行 打包命令，如果vue-tsc 报错，就把打包命令中的 vue-tsc -b && 删除，从根本上解决问题（其实就是没装依赖）
至于打包后能不能用，我先去试一试，稍等两行文字
测试了，可以用

## workspace 小结
这就是 pnpm 的 workspace 的基本使用方法，还有很多不同的用法，比如如何使用 pnpm link 链接本地包，如何使用 pnpm patch-package 解决依赖包版本冲突等问题，这里就不一一介绍了，感兴趣的可以自行探索。

[[demo](https://gitee.com/MR-lfc/platform)
]([demo](https://gitee.com/MR-lfc/platform)
)
