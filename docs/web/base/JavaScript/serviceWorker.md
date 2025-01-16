本篇简单介绍了 Service Worker 的基本概念，以及一些简单的使用场景。

祖传开篇：作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## 什么是 Service Worker ？

Service Worker （[MDN文档](https://developer.mozilla.org/zh-CN/docs/Web/API/Service_Worker_API)） 是一个注册在指定源和路径下的
`事件驱动` [worker](./webWorker) （[MDN文档](https://developer.mozilla.org/zh-CN/docs/Web/API/Service_Worker_API)）采用
JavaScript 文件的形式，控制关联的页面或者网站，拦截并修改访问和资源请求，细粒度地缓存资源。

### 特性

* 不能直接操作DOM
* 需要时会唤醒，不需要时会休眠，不会随着网页的关闭或浏览器的关闭而自动关闭。
* 缓存的内容开发者可以控制
* 安装后永远存活，除非手动卸载
* 必须由 HTTPS 承载
* 在 Service Worker 中，大量使用了 Promise
* 有自己的独立的生命周期
* 可以使用 indexDB 和 Cache API， 但是不能访问 localStorage

## 注册 Service Worker

使用 `ServiceWorkerContainer.register(scriptURL, options)` 方法首次注册 service worker。如果注册成功，service worker
就会被下载到客户端并尝试安装或激活（见下文），这将作用于整个域内用户可访问的 URL，或者其特定子集。

第一个参数 `scriptURL` 是一个 URL，它指向一个 Service Worker 脚本。

第二个参数 `options` 是一个可选对象，它包含以下属性： `scope` 表示定义 service worker 注册范围的 URL；service worker 可以控制的
URL 范围。通常是相对 URL。

```js
  if ('serviceWorker' in navigator) {
  try {
    const pageCache = await navigator.serviceWorker.register("/sw.js", {scope: '/'})
    if (pageCache.installing) {
      console.log("正在安装 Service worker");
    } else if (pageCache.waiting) {
      console.log("已安装 Service worker installed");
    } else if (pageCache.active) {
      console.log("激活 Service worker");
    }
  } catch (e) {
    console.error('serviceWorker 注册失败: ' + e)
  }
  
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    console.log('Service worker 更新了')
  })
} else {
  console.warn("当前浏览器不支持 serviceWorker")
}
```

## 生命周期

service worker 将遵守以下生命周期

### 下载

用户首次访问 service worker 控制的网站或页面时，service worker 会立刻被下载。

### 安装

service worker 下载完成后，会进入安装阶段，此时 service worker 会触发 `install` 事件，此时可以进行一些初始化操作，如缓存资源等。

### 激活

安装完成后，service worker 会进入激活阶段，此时 service worker 会触发 `activate` 事件，此时可以进行一些清理操作，如删除过期的缓存等。

## 通讯

在 Service Worker 安装完成后，它就可以与网页进行通讯了。

为了确保所有受控客户端都能接收到 Service Worker
激活的通知，通常会选择向每个客户端发送消息。如果你只需要通知特定的客户端，可以在逻辑上进行筛选，但默认情况下，通知所有客户端是一个更安全和全面的做法。

```js
self.addEventListener('activate', (event) => {
  event.waitUntil(
      // 获取所有当前service worker控制的客户端（例如，打开的页面）
      self.clients.matchAll().then((client) => {
        // 向每个客户端发送消息，通知它们service worker已被激活并准备处理fetch请求
        client.postMessage({
          msg: 'Hey, from service worker! I\'m listening to your fetch requests.',
          source: 'service-worker'
        })
      })
  )
})
```

客户端需要部署消息监听代码：

```js
navigator.serviceWorker.addEventListener('message', (event) => {
  console.log('来自 Service Worker 的通讯:', event.data.message);
})
```

## 示例

简单的演示用法

### 拦截 fetch 请求

```js 
const CHANG_ROUTER = ['upload/page']          // 修改请求信息的请求
const Prohibited_Requests = ['upload/pageAll']  // 直接拦截的请求

self.addEventListener('fetch', event => {
  // 如果请求url包含__api/，则修改请求信息，并返回fetch请求
  if (event.request.url.includes('__api/')) {
    // 简单获取一下请求路由
    let urlRouter = event.request.url.split('__api/')[1];
    // 如果请求路由包含在CHANG_ROUTER中，则修改请求信息，并返回fetch请求
    if (CHANG_ROUTER.includes(urlRouter)) {
      // 去掉 __api/ 保持原有信息继续请求
      event.respondWith(fetch(event.request.url.replace('__api/', ''), {
        method: event.request.method, body: event.request.body, headers: event.request.headers
      }).then(response => {
        return response;
      }));
    }
    // 如果请求路由包含在Prohibited_Requests中，则拦截请求，并返回错误信息
    else if (Prohibited_Requests.includes(urlRouter)) {
      // 向所有客户端发送消息，通知它们请求被拦截
      self.clients.matchAll().then(clients => {
        clients.forEach(client => {
          client.postMessage({type: 'Interception', data: urlRouter, message: '该请求已被拦截'});
        })
      })
      event.respondWith(new Response(JSON.stringify('请求被拦截'), {
        status: 403,
        statusText: 'Request Intercepted',
        headers: {'Content-Type': 'application/json'}
      }));
    } else {
      fetch(event.request).catch(() => {
        // 当请求失败时返回离线页面
        return caches.match(event.request).then(response => {
          return response || caches.match(OFFLINE_URL);
        });
      })
    }
  }
})

```

### 缓存指定内容

在 Service Worker 中，缓存的机制是使用 `caches` 对象提供的方法来操作的。

在 public 文件夹下创建一个 offline.html 文件，作为离线页面。

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>离线模式</title>
</head>
<body>
<h1>您已处于离线状态</h1>
<p>请检查您的网络连接。</p>
</body>
</html>
```

```js

const CACHE_NAME = 'my-vue-cache-v1'
const OFFLINE_URL = '/offline.html'
// 缓存的静态资源
const CACHE_ASSETS = [
  OFFLINE_URL
];
self.addEventListener('install', event => {
  event.waitUntil(
      caches.open(CACHE_NAME).then(cache => {
        return cache.addAll(CACHE_ASSETS);
      }).catch(err => console.error('缓存失败:', err))
  )
  // 跳过等待，立即激活
  self.skipWaiting();
})

self.addEventListener('activate', event => {
  event.waitUntil(
      caches.keys().then(cacheNames =>
          Promise.all(
              cacheNames.map(cacheName => {
                if (cacheName !== CACHE_NAME) {
                  console.log(`[Service Worker] 删除旧缓存: ${cacheName}`);
                  return caches.delete(cacheName);
                }
              })
          )
      )
  )
  self.clients.claim(); // 控制所有页面
})

self.addEventListener('fetch', event => {
  event.respondWith(
      fetch(event.request).catch(() => {
        if (event.request.mode === 'navigate') {
          // 如果是导航请求，返回离线页面
          return caches.match(OFFLINE_URL);
        } else {
          // 非导航请求返回缓存内容（若存在）
          return caches.match(event.request);
        }
      })
  )
})

```

完成一次安装后，就可以在 浏览器的开发者控制面板中，打开应用程序 ，找到 缓存存储 就可以看到 以 my-vue-cache-v1 命名的缓存了。

然后离线状态下访问页面，就会显示 offline.html 的内容了

## 其他 API

相关 API 有很多，作者也是就了解一些，详细的 API
文档大家可以移步 [MDN](https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API)。

下面介绍一个比较有意思的 API

### notificationclick

在主线程上或在非 Service Worker 的工作线程中创建的通知 使用 Notification（） 构造函数将 而是在 Notification 对象上接收
Click 事件 本身。 例如：

```js
function showNotification(title, body) {
  if (Notification.permission === 'granted') {
    new Notification(title, {body});
  } else {
    console.log('未授予通知权限');
  }
}

onMounted(() => {
  document.getElementById('request-permission-btn').addEventListener('click', () => {
    // 请求通知权限
    Notification.requestPermission().then(permission => {
      if (permission === 'granted') {
        console.log('用户已授予通知权限');
        showNotification('通知已启用', '您将收到重要通知。');
      } else if (permission === 'denied') {
        console.log('用户拒绝了通知权限');
      } else {
        console.log('用户未决定');
      }
    });
  });

})
```

在 Service Worker 中触发 `ServiceWorkerGlobalScope` 接口的 `notificationclick` 事件，生成的系统通知。（同样也需要用户授予权限）

```js
navigator.serviceWorker.addEventListener('message', (event) => {
  console.log('来自 Service Worker 的通讯:', event.data.message);
  navigator.serviceWorker.ready.then(registration => {
    registration.showNotification('来自 Service Worker 的消息', {
      body: event.data.message
    })
  })
})
```

在 Service Worker 中，监听 `notificationclick` 事件。

```js
self.addEventListener('notificationclick', event => {
  console.log('通知被点击:', event.notification.tag);
  event.notification.close(); // 关闭通知
});
// 这里也可以使用 self.onnotificationclick = (event) => {}) 
```

### Push API

这个API 是一个 Web API，它允许 Web 应用程序向用户发送推送消息。即使用户没有打开应用程序，也可以通过 Push API 发送推送消息。

在 Service Worker 中，监听 `push` 事件。

```js
self.addEventListener('push', event => {
  const data = event.data.json(); // 解析推送消息的内容
  const title = data.title || '新消息';
  const options = {
    body: data.body || '你有一条新消息',
    icon: '/icon.png',
    badge: '/badge.png'
  };
  
  event.waitUntil(
      self.registration.showNotification(title, options)
  );
});
```

这个就简单演示一下吧，具体的使用大家可以参考 [MDN](https://developer.mozilla.org/en-US/docs/Web/API/Push_API)。

使用时需要准备太多内容，大概如下：

1. **Service Worker：** 用于接收推送消息。
2. **VAPID 密钥：** 用于验证推送消息的来源。
3. **客户端订阅：** 使用 PushManager.subscribe() 订阅推送服务。
4. **服务器配置：** 使用 web-push 库发送推送消息。
5. **HTTPS 环境：** 确保网站在 HTTPS 下运行。
6. **浏览器兼容性：** 检查目标浏览器的支持情况。

