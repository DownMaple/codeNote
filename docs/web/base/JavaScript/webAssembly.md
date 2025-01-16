本篇简单介绍了 WebAssembly 的基本概念和 怎么用 Rust 构建一个wasm供 Web 端使用。

祖传开篇：作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## WebAssembly 简介

WebAssembly（简称 WASM）是一种可以在现代 Web 浏览器中运行的二进制指令格式，它是一种低级的编程语言，可以与 JavaScript
一起运行。WebAssembly 的设计目标是高性能和跨平台，通过一种紧凑的字节码表示，允许开发者用多种高级语言（如 C、C++、Rust
等）编写代码并将其编译为 WebAssembly。

WebAssembly 的出现填补了 JavaScript 在性能密集型任务上的不足，比如游戏开发、视频编辑器、科学计算等。

[WebAssembly 规范](https://webassembly.github.io/spec/core/index.html)

### WebAssembly 的特点

1. **高性能：** Wasm被打包为二进制文件，在浏览器中运行时，它将直接映射到底层硬件，从而实现接近原生的性能。 WebAssembly
   的设计目标之一是提供接近原生的性能。
2. **安全性：** WebAssembly 运行在沙箱环境中，与现有的 Web 安全模型集成，确保在运行时的安全性。
3. **跨平台：** WebAssembly 可以在多种平台（如 Windows、macOS、Linux 等）上运行，并且可以与现有的 JavaScript 运行时集成。
4. **语言中立：** 开发者可以使用支持 WebAssembly 的任何语言编写代码，例如 C、C++、Rust、Go 等
5. **与 JavaScript 的互操作性：** Wasm 可以与 JavaScript 无缝交互，充分利用现有的 Web 生态。

## WebAssembly 的工作流程

WebAssembly 的 工作流程大概可以分为一下几个阶段

### 编译阶段

WebAssembly 的程序通常用高级编程语言（如 C、C++、Rust）编写。开发者通过特定的编译工具链（如 Emscripten 或 Rust 的
wasm-pack）将代码转换为 .wasm 文件。这些工具会将高级语言代码编译为 WebAssembly 字节码，并生成能够与 JavaScript 交互的接口代码。

例如编写一个方法，接收前端一个字符串，然后返回拼接后的字符串：

```rust
#[wasm_bindgen]
pub fn show_name(name: String) -> String {
    format!("Hello, {}!", name)
}
```

然后 通过 wasm-pack 生成 .wasm 文件：

```bash
wasm-pack build --target web
```

这样就会在项目根目录 pkg 目录下生成一个 .wasm 文件,还包含了其他文件，如下：

```
pkg/
├── wasm-rust.d.ts
├── wasm-rust.js
├── wasm-rust-bg.wasm
├── wasm-rust-bg.wasm.ts
└── package.json
```

到这里就可以直接发布到 npm 上，然后通过 npm 安装到前端项目。也可以直接将 pkg 中的文件拷贝到前端项目，然后通过 import 引入即可。

### WebAssembly 模块的结构

以下内容来自于 [WebAssembly 模块结构](https://webassembly.github.io/spec/core/text/modules.html)
还有 [掘金 小鱼儿i99 的 WebAssembly 原理](https://juejin.cn/post/7032856060415180814)

Wasm 模块的二进制数据是以 Section 的形式被安排和存放的。对于 Section，可以直接把它想象成一个个具有特定功能的一簇二进制数据。

* **Type Section （类型段）：** 定义函数的签名（参数类型和返回值类型）。
* **Import Section （导入段）：** 定义模块依赖的外部函数和变量（如 JavaScript 函数）。
* **Function Section （函数段）：** 定义模块中的函数，并引用类型段中的函数签名。
* **Table Section （表段）：** 定义模块中的表，如函数指针表。
* **Memory Section （内存段）：** 定义模块使用的线性内存。WebAssembly 的内存是一个连续的字节数组，可以通过索引访问。
* **Global Section （全局段）：** 定义模块中的全局变量。
* **Export Section （导出段）：** 定义模块导出的函数和变量，供外部（如 JavaScript）调用。
* **Code Section （代码段）：** 包含函数的实际字节码指令。
* **Data Section （数据段）：** 定义模块中的数据，如字符串、数组等。

### 加载和验证

浏览器加载 .wasm 文件时，会先对其进行验证。这一步是为了确保 WebAssembly 模块的结构和内容是合法的，避免恶意代码的执行。

验证的内容：

* 检查模块的结构是否符合 WebAssembly 标准。
* 确保字节码中的操作不会违反沙盒安全规则。

验证后的 WebAssembly 模块会被编译为浏览器特定的机器代码，以便快速执行。

### 分配内存

如果模块定义了内存段，浏览器会为其分配线性内存。

线性内存（Linear Memory）是指一种连续的、一维的内存地址空间。

特点：

* **连续地址空间：** 线性内存中的每个地址都是连续的，没有间隔或跳跃。
* **单一维度：** 线性内存是一个一维空间，没有二维或三维等维度。
* **简化寻址：** 由于地址是连续的，可以通过简单的算术运算来计算偏移量，从而访问特定的内存位置
* **可扩展性：** 线性内存可以动态扩展，以适应动态变化的数据需求。

在浏览器中，线性内存被映射到 JavaScript 的内存中，以供 JavaScript 代码使用。

后面可以在项目中使用 wasm 了。

## 完整案例

简单的 js 和 wasm 交互

### 环境要求

* rustup
* rustc
* cargo
* node
* npm

### 新建 Rust 项目

先安装 wasm-pack

```bash
cargo install wasm-pack
```

也可以用 npm

```bash
npm install -g wasm-pack
```

然后创建一个 rust 项目：

```bash
cargo new --lib wasm-study-rust
```

### 编写 Rust 代码

首先修改 cargo.toml 文件：

wasm-bindgen = "0.2" 和 crate-type = ["cdylib"]  用于 生成 .wasm 文件，是必要的

serde_json 和 serde 用于序列化与反序列化 json 数据（可选）

```
[package]
name = "wasm_study_rust"
version = "0.1.0"
edition = "2021"

[lib]
crate-type = ["cdylib"]

[dependencies]
wasm-bindgen = "0.2"
serde_json = "1"   
serde = { version = "1.0.217", features = ["derive"] }

```

然后在 src/lib.rs 中编写 Rust 代码：

```rust
use wasm_bindgen::prelude::wasm_bindgen;
use serde::{Deserialize, Serialize};

#[wasm_bindgen]
pub fn add(left: u64, right: u64) -> u64 {
    left + right
}

#[wasm_bindgen]
#[derive(Serialize, Deserialize)]
pub struct User{
    name: String,
    age: u8,
    email:String
}
#[wasm_bindgen]
impl User {
    #[wasm_bindgen(constructor)]
    pub fn new(name: String, age: u8, email: String) -> User {
        User { name, age, email }
    }

    #[wasm_bindgen(getter)]
    pub fn name(&self) -> String {
        self.name.clone()
    }

    #[wasm_bindgen(getter)]
    pub fn age(&self) -> u8 {
        self.age
    }

    #[wasm_bindgen(getter)]
    pub fn email(&self) -> String {
        self.email.clone()
    }
}

#[wasm_bindgen]
pub fn show_name(name: String) -> String {
    let user = User{
        name: name.clone(),
        age: 18,
        email: "123@qq.com".to_string()
    };
    let user_json = serde_json::to_string(&user).unwrap();
    jsUserFn(user);
    jsUserJsonFn(user_json);
    format!("Hello, {}!", name)
}

// 方式一
// #[wasm_bindgen]
// extern {
//     fn jsUserFn(user: User);
//     fn jsUserJsonFn(user: String);
// }

// 方式二
#[wasm_bindgen(js_namespace = myNamespace)]
extern "C" {
    fn jsUserFn(user: User);
    fn jsUserJsonFn(user: String);
}

```

我们这里主要显示了接收js传递的参数，以及通过方法向js返回数据。

在 show_name() 方法中，我们主动调用了 js 的方法 ` jsUserFn(user)` 和 `jsUserJsonFn(user_json)` ，并传递了参数。
传递参数用了两种方式，一种直接传递对象，另一种是直接传递 json 字符串。

直接传递对象，需要在 rust 中，手动实现 结构体 的 getter 和 setter 方法，就是上面的 `impl User {}` 中的代码

我们使用了两种方式 来收集 js 的方法，一种是全局调用，另一种是通过命名空间调用。
在后面我们可以在 vue 中来区分这两种方式

### 打包 wasm

```bash
wasm-pack build --target web
```

执行后，就会在项目根目录生成pkg文件夹，里面有wasm文件，以及js文件。

### 在 Vue 中使用 WebAssembly

首先，在 vite.config.ts 中，修改配置，添加对 .wasm 文件的支持：

```ts
export default defineConfig({
  plugins: [vue(), vueJsx(), vueDevTools()],
  assetsInclude: ['**/*.wasm'], // 添加对 .wasm 文件的支持
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
})

```

创建一个 wasm 文件夹，将打包好的 pkg 文件夹内的文件放入其中：

```
src/
├── wasm/
├───── wasm-rust.d.ts
├───── wasm-rust.js
├───── wasm-rust-bg.wasm
├───── wasm-rust-bg.wasm.ts
├───── wasm-rust.d.ts
└───── wasmLoader.ts // 新建一个 ts 文件，用于加载 wasm 文件

```

在 wasmLoader.ts 中，添加以下代码：

```ts
// 方式一
// function jsUserFn(user) {
//   console.log(user)
// }
//
// function jsUserJsonFn(user) {
//   console.log(user)
// }
//
// window['jsUserFn'] = jsUserFn
// window['jsUserJsonFn'] = jsUserJsonFn

// 方法二 命名空间
import {myNamespace} from './module.js'

window['myNamespace'] = myNamespace

import __wbg_init, {add, show_name} from './wasm_study_rust'

export async function loadWasm() {
  // 加载 wasm 文件
  await __wbg_init()
  // 返回 add 和 show_name 方法 
  return {add, show_name}
}

```

这里两种方式来声明方法，供 Rust 收集。 一种是直接挂载 window 对象，另一种是通过命名空间来声明。

这样我们就可以在 Vue 中使用 WebAssembly 中的方法了。

```
<script setup lang="ts">
  import {loadWasm} from '@/wasm/wasmLoader.ts'

  async function triggerWasm() {
    const wasm = await loadWasm()
    console.log(wasm.add(BigInt(1), BigInt(2)))
  }

  async function showNameClick() {
    const wasm = await loadWasm()
    console.log(wasm.show_name('youYuXi'))
  }
</script>

<template>
  <button @click="triggerWasm">触发 wasm</button>
  <button @click="showNameClick">触发 wasm 2</button>
</template>
```

### 其他

关于方法传参和返回值类型的问题，还有就是Rust收集可用方法，可以使用 [`js-sys`](https://crates.io/crates/js-sys) 和 [
`web-sys`](https://crates.io/crates/web-sys) 手动调用 JavaScript 函数。
这个感兴趣的可以自行查阅

## 局限性

* **开发门槛高**：WebAssembly 需要使用其他语言（如 Rust 或 C++）进行开发，并且需要使用特定的工具链来编译和打包。
* **调试复杂**：由于 WebAssembly 是一种字节码格式，因此调试起来相对复杂，需要使用特定的调试工具来查看和调试 WebAssembly 代码。
* **功能限制**：WebAssembly 目前还不支持所有 JavaScript 功能，例如 DOM 操作、定时器、网络请求等。
* **生态正在发展**：WebAssembly 的生态正在快速发展。
