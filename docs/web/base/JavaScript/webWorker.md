祖传开篇：本文简单介绍 什么是`Web Worker`
以及基础用法；详细案例请前往：[封装elementPlus上传组件为大文件切片上传](../../vue/components/chunkUpload)
。作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## Web Worker

Web Worker 是 HTML5 引入的一个规范，允许 JavaScript 在后台线程中执行任务，而不会阻塞主页面的执行。这对于执行耗时操作（如密集计算、文件上传下载等）非常有用，能够显著提升
web 应用的性能和用户体验。

在 JavaScript 世界中，多线程编程曾经是一个遥不可及的梦想。然而，随着 Web Worker 的出现，这一梦想成为了现实。

## 创建一个 Web Worker (Dedicated Worker )

要创建一个 Web Worker，首先需要准备一个独立的 JavaScript 文件，这个文件将运行在后台线程中。

### 主线程代码

```js
// 主页面代码
const worker = new Worker('worker.js'); 
```

Worker 的参数支持网络路径 或者 new URL() 路径， 并不支持本地路径，所以要使用 本地路径时可以如下：

```js
const worker = new Worker(new URL('./generateFile.worker.js', import.meta.url));

// console.log(new URL('./generateFile.worker.js', import.meta.url))  如下：
// hash:""
// host:"localhost:5173"
// hostname:"localhost"
// href:"http://localhost:5173/src/uploadSharding/utils/generateFile.worker.js"
// origin:"http://localhost:5173"
// password:""
// pathname:"/src/uploadSharding/utils/generateFile.worker.js"
// port:"5173"
```

* **import.meta.url：**  是 ES 模块（ESM）中的一个内置对象，它返回当前模块的 URL，通常是当前文件在浏览器或 Node.js 环境中的位置
    * 在浏览器环境中，它会返回当前模块的绝对
      URL（比如 `h ttp://localhost:5173/src/uploadSharding/uploadSharding.vue?t=1736324013520` ）
    * 在 Node.js 环境中，它会返回当前模块的文件路径（比如 file:///path/to/your/module.js）
* **new URL()：**   是 JavaScript 中的一个内置函数，用于创建一个 URL 对象，该对象封装了 URL
  的各个部分，包括协议、主机名、端口号、路径、查询字符串和哈希值等。
    * 第一个参数是相对路径（'./generateFile.worker.js'），它是一个相对 URL。
    * 第二个参数是当前模块的 URL（import.meta.url）,它指定了该相对 URL 解析时的基准 URL。

### Web Worker 线程通讯

在 Web Workers 中，self 代表当前 worker 的全局上下文。

* **访问 Worker 上下文：** 通过 self 可以访问 Worker 的 `postMessage`、`onmessage`等方法。
* **控制 Worker 生命周期：** self 也可以用于结束 worker，例如调用 `self.close()` 来关闭当前 worker。

在 worker.js 中，可以通过 `self.onmessage`  来监听主线程发送过来的消息，也可以用 `self.postMessage` 来向主线程发送消息。

```js
// worker.js
self.onmessage = function (event) {
  console.log(event.data); // 输出：Hello Worker!
  // 处理接收到的数据
  self.postMessage('Hello Main!');
};

```

在主线程中，可以通过 `worker.postMessage` 来发送消息给 worker。

```js
const worker = new Worker('worker.js');
worker.postMessage('Hello Worker!');
```

我们还可以使用 `worker.onmessage` 来监听 worker 发送的消息。

```js
const worker = new Worker('worker.js');
worker.onmessage = function (event) {
}
```

### importScripts()

在 Service Worker 中，importScripts()
也用于导入脚本文件，常用于服务工作线程中注册事件监听器、缓存文件等任务。importScripts() 也是同步的。

importScripts() 不支持 ES6 模块导入 (import/export)，它只能导入传统的 JavaScript 脚本。在 Worker 中使用 ES6 模块时，可以通过
Worker 的 type: 'module' 选项来支持模块化。

```js
// service-worker.js
importScripts('/cache-utils.js');
```

* **同步加载：** importScripts() 会阻塞当前 Worker 的执行，直到脚本文件加载完成。因此，它不适合用于需要并行执行任务的场景。
* **同源策略：** 如果导入的脚本文件是远程文件（例如通过 URL 引入），则必须遵循同源策略。也就是说，Worker
  中的脚本只能从同一个源加载，除非服务器启用了 CORS（跨源资源共享）。
* **无法使用动态导入：** importScripts() 不支持像 import() 或 require() 这样的动态导入，它只能加载静态文件。

## 常用API

* `Worker()`：创建一个 Worker 对象，用于执行指定的脚本。
* `postMessage()`：向 另一个线程 发送消息。
* `onmessage`：当另一个线程发送消息时触发。
* `onerror`：当另一个线程发生错误时触发。
* `close()`：关闭当前线程。
* `terminate()`：终止当前线程。
* `importScripts()`:在 Worker 线程中加载外部 JavaScript 文件。

## web Worker 的限制

* 不能直接操作 DOM
* 不能访问 window、document 对象
* 不能使用 setTimeout() 和 setInterval()，需要使用 Worker 的专用定时器 API

## 示例代码

```js
// 主页面代码
const worker = new Worker('calculator.js');

worker.postMessage({operation: 'sum', numbers: [1, 2, 3, 4, 5]});

worker.onmessage = function (event) {
  console.log('Sum result:', event.data.result); // 输出：15
};

// calculator.js
self.onmessage = function (event) {
  const {operation, numbers} = event.data;
  let result;
  
  switch (operation) {
    case 'sum':
      result = numbers.reduce((acc, num) => acc + num, 0);
      break;
      // 其他操作
    default:
      result = 0;
  }
  
  self.postMessage({result});
};
```

## 其他 Worker

除了常见的 Web Worker，在浏览器环境中还有其他类型的 Worker，它们分别适用于不同的用途和场景。

### Service Worker

`Service Worker` 是一个独立于浏览器页面运行的 JavaScript 线程，可以在后台处理任务，例如缓存管理、离线支持、推送通知等。它的最大特点是能够在用户不打开页面的情况下运行。

* 拦截和缓存网络请求：Service Worker 可以拦截从浏览器发出的请求，决定是否从缓存中加载资源，或者将请求转发到网络上。
* 离线工作：Service Worker 可以让你的应用在没有网络连接的情况下仍然工作，依靠缓存和后台同步。
* 推送通知：可以接收来自服务器的推送通知，并展示给用户。

### Shared Worker

`Shared Worker` 允许多个浏览器标签页、窗口或 iframe 共享同一个 Worker。它与 Web Worker 相似，但可以在多个上下文之间共享。

* 跨多个浏览器上下文共享：可以在多个标签页、窗口或 iframe 之间共享同一个 Worker 实例。
* 消息传递：通过 port 对象在不同的窗口或标签页之间传递消息。

### Audio Worklet

`Audio Worklet` 是用于创建高效音频处理节点的 API，适用于音频应用程序（如浏览器音频处理、音频合成等）。它在 Web Audio API
中提供了一个低延迟、高性能的音频处理机制。

* 高性能：适合音频实时处理，减少延迟。
* 音频节点：可以创建自定义的音频节点进行音频数据的处理。

### WebSocket Workers

`WebSocket` 是一种双向通信协议，允许客户端和服务器之间通过一个持久化连接进行实时通讯。尽管 WebSocket 不是一个独立的 Worker
类型，它与 Workers 一起使用时，可以提供非常高效的实时通信。

* 实时双向通信：与服务器保持一个持久的连接，适用于聊天、实时游戏等应用。
* 与 Web Worker 结合：可以将 WebSocket 与 Worker 结合，避免主线程阻塞。
