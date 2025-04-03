本篇简单介绍了 AbortController API 的基本概念，以及一些简单的用法。

祖传开篇：作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## AbortController 简介

[MDN文档](https://developer.mozilla.org/zh-CN/docs/Web/API/AbortController)

AbortController 是一个允许你根据需要中止一个或多个 Web 请求或异步操作的控制器对象。其核心概念在于：

* **控制器（AbortController 实例）**：用于发出中止信号。
* **信号（AbortSignal 对象）**：通过控制器的 signal 属性获取，用于与异步操作通信。当调用控制器的 abort()
  方法时，所有绑定了该信号的操作都会被中止。

`AbortController` 的出现解决了在处理网络请求、定时任务或长时间运行的操作时，如何优雅地取消不再需要的任务的问题。

不止如此，`AbortController` 还可以用于中止自定义的异步任务、中止 `WebSocket`、中止 Nodejs 的文件读取操作、中止 setTimeout
、中止 `Worker` 线程、中止 `Promise` 的链式操作等。

## 基本用法

这是一个简单的demo，在服务端未响应之前，点击中止按钮，可以中止请求。同时在浏览器中也会看到请求被中止的信息。

但是这个 demo 是一次性的，即只能中止一次请求，如果再次发送请求，还是会显示请求被中止的信息。

这是因为 发起请求用的都是同一个 AbortController 实例，所以无法中止第二次请求。

```vue

<template>
  <div class="box">
    <button @click="send">发送请求</button>
    <button @click="abort">中止请求</button>
  </div>
</template>
<script setup>
  const controller = new AbortController()
  const signal = controller.signal

  function abort() {
    controller.abort()
  }

  function send() {
    fetch('http://localhost:3000/test', {signal, method: 'GET'})
        .then((res) => {
          console.log(res)
        })
        .catch((err) => {
          console.log(err)
        })
  }
</script>
<style scoped></style>

```

## 完整案例

下面来写一些比较完整的案例

### 可以多次发起并终止

封装 fetch 请求，返回请求和一个中止方法

```js
/**
 * 可取消的fetch请求
 * @param {string} url
 * @param {object} config
 * @returns
 */
export default function myFetch(url, config = {}) {
  const controller = new AbortController()
  
  const mergeConfig = {
    signal: controller.signal,
    ...config
  }
  
  const request = fetch(url, mergeConfig)
  
  return {
    request,
    cancel: () => controller.abort()
  }
}
```

在vue页面中使用

```vue

<script setup>
  import myFetch from './utils/myFetch'

  let cancelFetch = null

  function abort() {
    cancelFetch()
  }

  function send() {
    const {request, cancel} = myFetch('http://localhost:3000/test', {method: 'GET'})
    cancelFetch = cancel
    request
        .then((res) => {
          console.log(res)
        })
        .catch((err) => {
          console.log(err)
        })
  }
</script>
```

### 取消多个请求

多个请求公用一个 signal

```js
const controller = new AbortController();

// 同时发起多个请求
Promise.all([
  fetch('/api/orders', { signal: controller.signal }),
  fetch('/api/users', { signal: controller.signal })
])
  .then(/* ... */)
  .catch(/* 处理中止错误 */);

// 一次性取消所有请求
controller.abort();
```

独立控制多个请求

```js
const controllers = {
  orders: new AbortController(),
  users: new AbortController()
};

function cancelRequest(type) {
  controllers[type]?.abort();
}

// 分别控制不同请求
fetch('/api/orders', { signal: controllers.orders.signal });
fetch('/api/users', { signal: controllers.users.signal });
```

### 中止计时器

```js
/**
 * 创建可中止的计时器
 * @param {Function} callback - 计时器回调函数
 * @param {number} delay - 延迟时间(毫秒)
 * @param {boolean} [isInterval=false] - 是否为循环计时器
 * @returns { object } 包含 abort 方法的对象
 */
export default function createAbortableTimer(callback, delay, isInterval = false) {
	let timerId = null
	const abortController = new AbortController()

	// 包装回调函数，支持中止信号
	const wrappedCallback = () => {
		if (!abortController.signal.aborted) {
			callback()
		}
	}

	// 启动计时器
	if (isInterval) {
		timerId = setInterval(wrappedCallback, delay)
	} else {
		timerId = setTimeout(wrappedCallback, delay)
	}

	// 中止方法
	const abort = () => {
		if (timerId) {
			isInterval ? clearInterval(timerId) : clearTimeout(timerId)
			abortController.abort()
			timerId = null
		}
	}

	return { abort }
}


// 使用示例 - setTimeout
const timer1 = createAbortableTimer(
        () => console.log('单次计时器触发'),
        3000
);

// 在需要时中止
// timer1.abort();

// 使用示例 - setInterval
let counter = 0;
const timer2 = createAbortableTimer(
        () => {
          console.log(`循环计时器触发 ${++counter} 次`);
          if (counter >= 5) timer2.abort();
        },
        1000,
        true
);

```


### 中止其他操作

```js
async function fetchWithTimeout(url, timeout = 5000) {
  // 创建一个 AbortController 实例
  const controller = new AbortController();
  const signal = controller.signal;

  // 设置超时定时器，超时后自动调用 controller.abort()
  const timeoutId = setTimeout(() => {
    controller.abort();
  }, timeout);

  try {
    const response = await fetch(url, { signal });
    clearTimeout(timeoutId); // 请求成功，清除超时定时器
    const data = await response.json();
    console.log('请求成功：', data);
    return data;
  } catch (error) {
    if (error.name === 'AbortError') {
      console.error('请求被中止');
    } else {
      console.error('请求失败：', error);
    }
  }
}

// 调用示例
fetchWithTimeout('https://jsonplaceholder.typicode.com/todos/1', 3000);

```

```js
// 示例：中断 Web Worker
const controller = new AbortController();
const worker = new Worker('worker.js');

controller.signal.addEventListener('abort', () => {
  worker.terminate();
});

// 触发中止
controller.abort();

// 示例：取消 requestAnimationFrame
const controller = new AbortController();
let animationFrameId;

controller.signal.addEventListener('abort', () => {
  cancelAnimationFrame(animationFrameId);
});

function animate() {
  animationFrameId = requestAnimationFrame(animate);
  // 动画逻辑...
}

animate();
// 需要停止时调用：
// controller.abort();
```

## 注意事项

1. **错误处理：** 中止操作会导致相关 `Promise` 被拒绝，并抛出 `AbortError`。在编写代码时应对这种错误进行专门捕获与处理，以区分中止操作与其他网络错误。
2. **不可重用：** 调用 `abort()` 后，`AbortController` 实例将永久处于中止状态，再次调用不会有额外效果。因此，若需要多次控制操作，建议创建新的 `AbortController` 实例。
3. **浏览器兼容性：** 大多数现代浏览器（Chrome 66+、Firefox 57+、Safari 11.1+、Edge 16+）都已支持 `AbortController`；但在老旧浏览器或某些特定环境下可能需要 `polyfill`。
4. **竞态条件：** 在某些情况下，中止操作可能无法立即生效，因为某些操作可能已经完成或处于等待状态。因此，在处理中止操作时，需要考虑这种可能。
5. **资源清理：** 中止操作后，要确保及时清理资源，如清除定时器、取消未完成的任务等，防止内存泄漏。
