祖传开篇：本文简单介绍JavaScript的事件循环（消息循环）机制。作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## 为什么会有事件循环？

在寻找为什么会有事件循环机制之前，我们先从浏览器开始慢慢探索。

### 浏览器的进程和线程

> [!TIP]
> 进程（Process）：是操作系统分配资源的最小单位，每个进程拥有独立的内存空间、文件描述符等资源。进程之间的资源是隔离的，一个进程崩溃不会直接影响其他进程。

> [!TIP]
> 线程（Thread）：是进程内的执行单元，是操作系统调度的最小单位。同一进程内的线程共享该进程的资源（如内存、文件描述符等），但每个线程有自己的栈和寄存器状态。

浏览器是及其复杂的的软件，它运行在操作系统之上，它由进程和线程组成。进程是操作系统分配资源的最小单位，而线程是进程的子单位，它负责完成具体的任务。

浏览器的进程有很多，下面罗列一些比较常用的：

1. **浏览器进程：**
    * 功能：负责管理用户界面（如标签页、书签栏、地址栏等）、网络请求、存储和数据库操作等。
    * 特点：这是浏览器的主进程，控制其他所有进程，并处理与操作系统之间的交互。
2. **渲染进程：**
    * 功能：每个标签页对应一个独立的渲染进程，负责解析 HTML、CSS 和 JavaScript，构建 DOM 树、样式树，执行布局和绘制页面内容。
    * 特点：渲染进程是沙盒化的，以提高安全性，防止恶意代码影响整个浏览器或其他标签页。
3. **网络进程：**
    * 功能：负责处理所有的网络请求，包括 HTTP/HTTPS 请求、WebSocket 连接等。
    * 特点：集中管理网络资源，确保网络操作的安全性和效率。
4. **GPU 进程：**
    * 功能：负责处理图形相关的任务，如加速图形渲染、视频解码等。
    * 特点：通过 GPU 硬件加速，提升图形和动画的性能，减少 CPU 负担。

还有些进程如插件进程、扩展进程、Service Worker 进程等，它们也负责特定的任务，但与浏览器进程不同的是，它们不是浏览器的主进程，而是由浏览器进程创建的。

### 渲染线程的主要工作

渲染进程（Renderer Process）是浏览器中负责处理网页内容的核心组件之一。每个标签页通常对应一个独立的渲染进程，以确保不同页面之间的隔离性和安全性。渲染进程的主要工作包括以下几个方面：

1. 解析HTML （渲染DOM树、解析外部资源）
2. 解析和应用CSS （构建样式树、样式计算）
3. 布局 （确定几何位置、优化布局）
4. 绘制 （生成绘制指令、绘制内容）
5. 合成 （图层管理、合成最终图层）
6. 执行JavaScript （解释和执行脚本、处理事件）
7. 处理异步任务
8. 安全和沙盒机制
9. ···

### 为什么是单线程

在上面介绍了进程和线程，一个进程可以有多个线程，而且渲染进程那么繁忙，那么为什么不能有多个线程执行js呢？

**原因：**

1. **浏览器环境的限制：**
    * UI 渲染与脚本执行不能同时进行：在浏览器中，JavaScript 主要用于操作 DOM 和处理用户交互。如果允许多线程执行
      JavaScript，可能会导致多个线程同时修改 DOM，从而引发竞争条件（race condition），使得页面渲染变得不可预测。
    * 避免复杂性：浏览器的设计者希望保持 JavaScript 的简单性和易用性。引入多线程编程会增加开发者的复杂度，尤其是对于并发控制、锁机制等高级概念的要求。
2. **简化内存管理：**
    * 垃圾回收机制：单线程环境下，垃圾回收器可以更高效地管理和回收内存。多线程环境下的内存管理更加复杂，需要考虑线程间的同步和协调问题。
3. **历史原因：**
    * 早期设计决策：JavaScript 最初设计时，主要是为了在网页中添加简单的交互功能，当时的浏览器环境并不支持复杂的多线程编程。随着
      Web 技术的发展，虽然 JavaScript 的应用场景越来越广泛，但其单线程特性已经被广泛接受，并且通过事件循环机制很好地解决了异步任务的处理问题。

### 怎么解决线程阻塞问题

1. **事件驱动模型与事件循环**
    * 事件循环（Event Loop）：JavaScript 使用事件循环机制来处理异步任务。事件循环不断检查任务队列中的任务，并按顺序执行它们。通过这种方式，即使有长时间运行的任务，也不会阻塞后续的任务执行。
    * 宏任务与微任务：事件循环将任务分为宏任务（如 `setTimeout`、`setInterval`、I/O 操作等）和微任务（如 `Promise`、
      `process.nextTick`
      等）。每次执行完一个宏任务后，会立即执行所有微任务，然后再进行下一轮的宏任务处理。这种设计确保了异步操作能够及时响应，而不会被阻塞。
2. **异步编程**
    * 回调函数（Callback）：早期的异步编程主要依赖回调函数。当异步操作完成时，通过回调函数返回结果。虽然这种方式简单直接，但嵌套过多的回调函数会导致“回调地狱”，降低代码可读性。
    * Promise：`Promise` 是一种更优雅的异步编程方式。它提供了链式调用的能力，可以更好地管理异步操作的结果和错误处理。
      `Promise` 的
      `.then() `和 `.catch() `方法使得代码更加简洁和易读。
    * async/await：这是 ES2017 引入的语法糖，进一步简化了异步编程。通过 `async` 函数和 `await`
      关键字，可以像同步代码一样编写异步代码，提高了代码的可读性和维护性。
3. **Web Workers**
    * 后台线程：Web Workers 允许在浏览器环境中创建后台线程来执行耗时的任务，而不阻塞主线程。适合处理复杂的计算任务、数据处理等。Web
      Workers 不能直接访问 DOM，但可以通过消息传递机制与主线程通信。
4. **非阻塞 I/O 操作**
    * 网络请求：JavaScript 提供了多种非阻塞的网络请求方式，如 `fetch` 和 `XMLHttpRequest`。这些 API 不会阻塞主线程，而是将请求交给浏览器的底层
      C++ 引擎处理，完成后通过回调函数或 `Promise` 返回结果。
    * 定时器：`setTimeout` 和 `setInterval` 是常见的定时器函数，用于延迟执行代码或定期执行代码。这些函数不会阻塞主线程，而是在指定的时间间隔后将任务放入事件队列等待执行。
5. **优化同步代码**
    * 分解大任务：对于一些不可避免的同步任务，可以通过将其分解为多个小任务来减少阻塞时间。例如，使用 `requestAnimationFrame`
      或
      `setTimeout` 将任务分片执行。
6. **使用现代浏览器特性**
    * Service Workers：`Service Workers` 是一种特殊的 `Web Worker`，可以在后台处理网络请求、推送通知等任务，而不影响主线程。它们还可以用于实现离线支持和缓存管理。

下面我们主要介绍**事件循环**

## 事件循环

最开始的时候，渲染主线程会进入一个无限的[循环状态](#怎样的循环)。

每一次循环，渲染线程会检查[消息队列](#消息队列)是否有任务需要处理，如果有，就处理。如果没有，就进入休眠状态。

其他线程或进程，可以随时的向消息队列添加任务，新的任务会插入到对应的消息队列的末尾，如果在添加时消息循环在休眠状态，则会唤醒。

这样的循环机制，就叫事件循环。

### 怎样的循环

直接上浏览器源码： [源码地址GitHub](https://github.com/chromium/chromium/blob/main/base/message_loop/message_pump_default.cc)

```c++
// Copyright 2006-2008 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "base/message_loop/message_pump_default.h" // [!code warning]

#include "base/auto_reset.h"
#include "base/logging.h"
#include "base/synchronization/waitable_event.h"
#include "build/build_config.h"

#if BUILDFLAG(IS_APPLE)
#include <mach/thread_policy.h>

#include "base/apple/mach_logging.h"
#include "base/apple/scoped_mach_port.h"
#include "base/apple/scoped_nsautorelease_pool.h"
#include "base/threading/threading_features.h"
#endif

namespace base {

MessagePumpDefault::MessagePumpDefault()
    : keep_running_(true),
      event_(WaitableEvent::ResetPolicy::AUTOMATIC,
             WaitableEvent::InitialState::NOT_SIGNALED) {
  event_.declare_only_used_while_idle();
}

MessagePumpDefault::~MessagePumpDefault() = default;

void MessagePumpDefault::Run(Delegate* delegate) {
  AutoReset<bool> auto_reset_keep_running(&keep_running_, true);

  for (;;) {  // [!code warning]
#if BUILDFLAG(IS_APPLE)
    apple::ScopedNSAutoreleasePool autorelease_pool;
#endif

    Delegate::NextWorkInfo next_work_info = delegate->DoWork();
    bool has_more_immediate_work = next_work_info.is_immediate();
    if (!keep_running_) {
      break;
    }

    if (has_more_immediate_work) {
      continue;
    }

    delegate->DoIdleWork();
    if (!keep_running_) {
      break;
    }

    if (next_work_info.delayed_run_time.is_max()) {
      event_.Wait();
    } else {
      event_.TimedWait(next_work_info.remaining_delay());
    }
    // Since event_ is auto-reset, we don't need to do anything special here
    // other than service each delegate method.
  }
}

void MessagePumpDefault::Quit() {
  keep_running_ = false;
}

void MessagePumpDefault::ScheduleWork() {
  // Since this can be called on any thread, we need to ensure that our Run
  // loop wakes up.
  event_.Signal();
}

void MessagePumpDefault::ScheduleDelayedWork(
    const Delegate::NextWorkInfo& next_work_info) {
  // Since this is always called from the same thread as Run(), there is nothing
  // to do as the loop is already running. It will wait in Run() with the
  // correct timeout when it's out of immediate tasks.
  // TODO(gab): Consider removing ScheduleDelayedWork() when all pumps function
  // this way (bit.ly/merge-message-pump-do-work).
}

}  // namespace base
```

可以看到，在浏览器的源码中，message_loop 文件夹下，就是实现了事件循环机制。 这是一个无限循环，当有任务需要处理时，会调用
Delegate 的 DoWork 方法，当没有任务需要处理时，会调用 ScheduleWork 方法，让事件循环机制等待。

### 消息队列

消息队列（Message Queue） 是一种用于存储和管理任务的数据结构，通常是一个先进先出（FIFO）的队列。在 JavaScript
的事件循环机制中，消息队列用于存放待执行的任务（如回调函数、宏任务和微任务）。当主线程空闲时，会从消息队列中取出任务并执行。

作用：

* **任务调度：** 消息队列确保了任务能够按顺序执行，避免了多个任务同时竞争 CPU 资源的问题。
* **异步处理：** 通过将异步操作的结果放入消息队列，JavaScript 可以在不阻塞主线程的情况下处理这些结果，从而保持页面的流畅性和响应性。
* **事件驱动：** 消息队列是事件驱动模型的核心组成部分，它使得 JavaScript 能够高效地处理各种事件（如用户输入、网络请求等）。

任务是没有优先级的，但是任务有不同的类型，同一类型的任务会进入同一个队列，不同类型任务可以分属不同的队列。（w3c没有明确规定不同类型的任务进入不同的队列，但是不能在两个队列中同时存在相同类型的任务）

chromium开源文档中：[任务类型](https://github.com/chromium/chromium/blob/main/third_party/blink/public/platform/TaskTypes.md)
和 [代码实现](https://github.com/chromium/chromium/blob/main/third_party/blink/public/platform/task_type.h)

不同的消息队列是有优先级的，w3c
文档中说明，浏览器必须准备一个微任务队列，并且微任务队列中的任务优先于其他任务执行。 [w3c文档](https://html.spec.whatwg.org/multipage/webappapis.html#perform-a-microtask-checkpoint)

在一次事件循环中，浏览器可以根据实际情况在不同的队列中取出任务执行。

> [!TIP]
> 根据 W3C 规范，事件循环的工作流程如下：
> 1. 执行同步代码：首先执行当前的同步代码，即从任务队列中取出一个任务并执行。
> 2. 处理微任务：每当一个任务执行完毕后，会立即处理所有微任务，直到微任务队列为空。
> 3. 渲染：在所有微任务执行完毕后，浏览器会进行一次渲染更新。
> 4. 下一个任务：从任务队列中取出下一个任务，重复上述过程。

chrome浏览器的实现，至少包含了以下几种队列：

* **延时队列：** 用于存放计时器完成后的回调任务，优先级-中
* **交互队列：** 用于存放用户操作后产生的事件处理任务，优先级-高
* **微任务队列：** 用于存放需要最快执行的微任务，优先级-最高
* **等···：** 还有很多其他队列

示例代码：

```javascript
console.log('Script start');  // 主线程输出 1

setTimeout(() => {
  console.log('setTimeout'); // 计时结束后进入延时队列 4
}, 0);

Promise.resolve().then(() => { // 快速生成一个微任务的方法
  console.log('Promise 1');  // 异步任务进入 微任务队列 3 
});

console.log('Script end');  // 主线程输出 2 

```

```bash
Script start
Script end
Promise 1
setTimeout
```

## 问题：JS的计时器能精准计时吗？

答案是：**不能**

1. 没有原子钟
2. JS 的计时器是依赖操作系统的，操作系统的计时器不是精确的，而是根据CPU的时钟频率来计算，所以 JS 的计时器也是不精确的
3. W3C标准，浏览器嵌套计时器如果超过5层，则会带有4毫秒的最少时间
4. 最后受事件循环的影响，计时器的回调任务只能在主线程的空闲时间执行
