本篇简单介绍了 Web Component 的基本概念，以及一些简单的用法。

祖传开篇：作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## Web Component 简介

如果需要精准分类，Web Component 是要归属于 HTML5 的，而不是 JavaScript。

在学习到 Vue和 React 之后，就会接触到 “组件” 这个概念。组件有着：容易使用、代码高服用、开发和定制方便、样式隔离等优点。

Web Component 就是浏览器原生的组件规范。

Web Component 是由一系列 Web API 组成的规范，包括：

- **Custom Elements （自定义元素） :**  一组 JavaScript API，允许你定义 custom elements 及其行为，然后可以在你的用户界面中按照需要使用它们。
- **Shadow DOM （影子 DOM） :** 一组 JavaScript API，用于将封装的“影子”DOM 树附加到元素（与主文档 DOM
  分开呈现）并控制其关联的功能。通过这种方式，你可以保持元素的功能私有，这样它们就可以被脚本化和样式化，而不用担心与文档的其他部分发生冲突。
- **HTML Template （HTML 模板） :**  `<template>` 和 `<slot>` 元素使你可以编写不在呈现页面中显示的标记模板。然后它们可以作为自定义元素结构的基础被多次重用。

参考自[Web Component](https://developer.mozilla.org/zh-CN/docs/Web/Web_Components)

## Custom Elements

HTML 标准定义的网页元素，有时并不符合我们的需要，这时浏览器允许用户自定义网页元素，这就叫做 Custom Element。

简单说，它就是用户自定义的网页元素，是 Web components 技术的核心。

> [!TIP]
> 注意，自定义网页元素的标签名必须含有连字符-，一个或多个连字符都可以。

自定义元素的类型有两种：

* **自定义内置元素（Customized built-in element）** 继承自标准的 HTML 元素，例如 `HTMLImageElement` 或
  `HTMLParagraphElement`。它们的实现定义了标准元素的行为。
* **独立自定义元素（Autonomous custom element）** 继承自 HTML 元素基类 `HTMLElement`。你必须从头开始实现它们的行为。

下面我们直接 自定义一个 `my-card` 组件。

### 基本用法

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Web Components</title>
  <style>
    body {
      padding: 0;
      margin: 0;
    }
    
    .my-card__title {
      color: blue;
    }
  </style>
  <script src="my-card.js" defer></script>
</head>
<body>
<my-card on-remove="handleRemove" card-id="card1">
</my-card>
<my-card on-remove="handleRemove" card-id="card2">
</my-card>
</body>
</html>

```

my-card.js

```js
class MyCard extends HTMLElement {
  constructor() {
    super();
    this.attachShadow({mode: 'open'});
    this.shadowRoot.innerHTML = `
       <style>
      .my-card {
          width: calc(100% - 30px);
          height: auto;
          margin: 15px 0 0 15px;
          border: 1px solid #f2f2f2;
          border-radius: 10px;
          box-shadow: 0 0 10px 3px rgba(0, 0, 0, 0.08);
      }

      .my-card__title {
          display: flex;
          flex-direction: row;
          align-items: flex-start;
          justify-content: space-between;
          padding: 15px;
          border-bottom: 1px solid #f2f2f2;
      }

      .my-card__title--close {
          font-size: 12px;
          color: #999;
          margin-top: 4px;
          flex-shrink: 0;
          margin-left: 15px;
          cursor: pointer;
      }

      .my-card__title--close:hover {
          color: red;
      }

      .my-card__content {
          padding: 15px;
          min-height: 100px;
      }
  </style>
  <div class="my-card">
    <div class="my-card__title">
    </div>
    <div class="my-card__content">

    </div>
  </div>  
    `
  }
  
  static get observedAttributes() {
    // 这里返回一个数组，数组中的元素就是需要监听的属性。
  }
  
  // 同上
  // static observedAttributes = ["color", "size"];
  
  attributeChangedCallback(name, oldValue, newValue) {
    // 在属性发生变化时，会触发这个回调函数。name 就是属性名，oldValue 是旧值，newValue 是新值。
  }
  
  connectedCallback() {
    // 在自定义元素被添加到 DOM 时，会触发这个回调函数。这里就可以访问 DOM 元素。
  }
  
  disconnectedCallback() {
    // 在自定义元素从 DOM 中移除时，会触发这个回调函数。
  }
  
  adoptedCallback() {
    // 当自定义元素被移动到新文档时被调用。
  }
}

customElements.define('my-card', MyCard)

```

- 上面自定义元素的基类是 `HTMLElement`，也可以根据需要，修改为 `HTMLImageElement` 或 `HTMLButtonElement` 等 `HTMLElement`
  的子类。
- 在构造函数中定义了 `shadowRoot`，所谓Shadow DOM指的是，这部分的 HTML 代码和样式，不直接暴露给用户。（也可以不使用
  `shadowRoot`，直接在 `constructor` 中定义 HTML 代码和样式）
- 这个类可以定义生命周期方法。

### 自定义元素的生命周期方法

一旦你的自定义元素被注册，当页面中的代码以特定方式与你的自定义元素交互时，浏览器将调用你的类的某些方法。通过提供这些方法的实现，规范称之为生命周期回调，你可以运行代码来响应这些事件。

* `connectedCallback()`：每当元素添加到文档中时调用。规范建议开发人员尽可能在此回调中实现自定义元素的设定，而不是在构造函数中实现。
* `disconnectedCallback()`：每当元素从文档中移除时调用。
* `adoptedCallback()`：每当元素被移动到新文档中时调用。
* `attributeChangedCallback()`：在属性更改、添加、移除或替换时调用,该方法接受三个参数：属性名、旧值、新值。

### 注册自定义元素

最后，使用 `window.customElements.define()` 方法;如果没有登记就使用 Custom Element，浏览器会认为这是一个不认识的元素，会当做空的
div 元素处理。

该方法接受三个参数：

* name: 元素的名称。必须以小写字母开头，包含一个连字符，并符合规范中有效名称的定义中列出的一些其他规则。
* constructor: 自定义元素的构造函数。
* options: 仅对于自定义内置元素，这是一个包含单个属性 extends 的对象，该属性是一个字符串，命名了要扩展的内置元素。

例如： `customElements.define("word-count", WordCount, { extends: "p" });`

## Shadow DOM

所谓 Shadow DOM 指的是，浏览器将模板、样式表、属性、JavaScript 码等，封装成一个独立的 DOM
元素。外部的设置无法影响到其内部，而内部的设置也不会影响到外部，与浏览器处理原生网页元素（比如`<video>`元素）的方式很像。

Shadow DOM 最大的好处有两个，一是可以向用户隐藏细节，直接提供组件，二是可以封装内部样式表，不会影响到外部。 在上面的代码我们可以看到，在页面的
style 样式中，我们定义了一个 `.my-card__title {color: blue;}` 样式，卡片标题却没有受影响，因为它被封装在 Shadow DOM 中。

以下来自 MDN ：

影子 DOM 允许将隐藏的 DOM 树附加到常规 DOM 树中的元素上——这个影子 DOM 始于一个影子根，在其之下你可以用与普通 DOM
相同的方式附加任何元素。

![An image](https://developer.mozilla.org/zh-CN/docs/Web/API/Web_components/Using_shadow_DOM/shadowdom.svg)

* **影子宿主（Shadow host）:** 影子 DOM 附加到的常规 DOM 节点。
* **影子树（Shadow tree）:** 影子 DOM 内部的 DOM 树。
* **影子边界（Shadow boundary）:** 影子 DOM 终止，常规 DOM 开始的地方。
* **影子根（Shadow root）:** 影子树的根节点。

`const shadowDOM = this.attachShadow({mode: 'open'});` 这里的 mode 属性决定了 Shadow DOM 的封装模式，它有两个可能的值：

* open：允许外部访问 Shadow DOM 的 API。
* closed：不允许外部访问 Shadow DOM 的 API。

当使用 open 模式创建 Shadow DOM 时，外部脚本可以通过 Element.shadowRoot 属性访问 Shadow DOM 的根节点。从外部查询、修改
Shadow DOM 内部的元素和样式

```html
</template>
<my-card card-id="card1">
  <span slot="my-card-title">这里是卡片的标题！！！</span>
  <div slot="my-card-content" style="color: #999">通过插槽实现的卡片内容·····</div>
</my-card>
<button id="btn">修改卡片1的标题</button>
<script>
  document.getElementById('btn').onclick = function () {
    const card = document.querySelector('my-card')
    card.shadowRoot.querySelector('.my-card__title').innerHTML = '修改了'
  }
</script>
```

当使用 closed 模式创建 Shadow DOM 时，外部脚本无法通过 Element.shadowRoot 属性访问 Shadow DOM 的根节点。

```js
const card = document.querySelector('my-card')  // 得到的是 null
```

## HTML Template

`<template>`标签表示组件的 HTML 代码模板。内部就是正常的 HTML 代码，浏览器不会将这些代码加入 DOM。

```html

<template>
  <h1>This won't display!</h1>
  <script>alert("this won't alert!");</script>
</template>
```

我们就可以用 JavaScript 动态的实例化模板

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Web Components</title>
  <style>
    body {
      padding: 0;
      margin: 0;
    }
    
    .my-card__title {
      color: blue;
    }
  </style>
  <script src="my-card.js" defer></script>

</head>
<body>
<template id="my-card-template">
  <style>
    .my-card {
      width: calc(100% - 30px);
      height: auto;
      margin: 15px 0 0 15px;
      border: 1px solid #f2f2f2;
      border-radius: 10px;
      box-shadow: 0 0 10px 3px rgba(0, 0, 0, 0.08);
    }
    
    .my-card__title {
      display: flex;
      flex-direction: row;
      align-items: flex-start;
      justify-content: space-between;
      padding: 15px;
      border-bottom: 1px solid #f2f2f2;
    }
    
    .my-card__title--close {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
      flex-shrink: 0;
      margin-left: 15px;
      cursor: pointer;
    }
    
    .my-card__title--close:hover {
      color: red;
    }
    
    .my-card__content {
      padding: 15px;
      min-height: 100px;
    }
  </style>
  <div class="my-card">
    <div class="my-card__title">
      <span>卡片标题</span>
      <span class="my-card__title--close">关闭</span>
    </div>
    <div class="my-card__content">
    </div>
  </div>
</template>
<my-card card-id="card1">
</my-card>
<my-card card-id="card2">
</my-card>
</body>
</html>

```

`<script src="my-card.js" defer></script>` defer 表示在页面加载完成后再执行。

```js
class MyCard extends HTMLElement {
  constructor() {
    super();
    const shadowDOM = this.attachShadow({mode: 'open'});
    const templateDOM = document.getElementById("my-card-template").content
    shadowDOM.appendChild(templateDOM.cloneNode(true))
  }
}

customElements.define('my-card', MyCard)

```

`cloneNode()`方法的参数 `true` 表示复制包含所有子节点。

### Slots

Slots 是一种特殊类型的元素，它允许你将内容从组件的一个部分传递到另一个部分，增加了组件的灵活性。它使得 Web Components
自定义元素，更加的灵活。

这样我们就可以在 my-card 中显示不同的内容。

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Web Components</title>
  <style>
    body {
      padding: 0;
      margin: 0;
    }
    
    .my-card__title {
      color: blue;
    }
  </style>
  <script src="my-card.js" defer></script>

</head>
<body>
<template id="my-card-template">
  <style>
    .my-card {
      width: calc(100% - 30px);
      height: auto;
      margin: 15px 0 0 15px;
      border: 1px solid #f2f2f2;
      border-radius: 10px;
      box-shadow: 0 0 10px 3px rgba(0, 0, 0, 0.08);
    }
    
    .my-card__title {
      display: flex;
      flex-direction: row;
      align-items: flex-start;
      justify-content: space-between;
      padding: 15px;
      border-bottom: 1px solid #f2f2f2;
    }
    
    .my-card__title--close {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
      flex-shrink: 0;
      margin-left: 15px;
      cursor: pointer;
    }
    
    .my-card__title--close:hover {
      color: red;
    }
    
    .my-card__content {
      padding: 15px;
      min-height: 100px;
    }
  </style>
  <div class="my-card">
    <div class="my-card__title">
      <slot name="my-card-title">卡片标题</slot>
      <span class="my-card__title--close">关闭</span>
    </div>
    <div class="my-card__content">
      <slot name="my-card-content">卡片内容</slot>
    </div>
  </div>
</template>
<my-card card-id="card1">
  <span slot="my-card-title">这里是卡片的标题！！！</span>
  <div slot="my-card-content" style="color: #999">通过插槽实现的卡片内容·····</div>
</my-card>
<my-card card-id="card2">
  <span slot="my-card-title">这里是卡片2！！！</span>
  <div slot="my-card-content" style="color: #999">通过插槽实现的卡片内容·····</div>
</my-card>
</body>
</html>

```

### 自定义属性以及事件

我们可以在 my-card 中定义一些自定义属性，然后在 内部做处理，也可以通过 自定义属性，传递一个全局的事件，来处理。

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Web Components</title>
  <style>
    body {
      padding: 0;
      margin: 0;
    }
    
    .my-card__title {
      color: blue;
    }
  </style>
  <script src="my-card.js" defer></script>

</head>
<body>
<template id="my-card-template">
  <style>
    .my-card {
      width: calc(100% - 30px);
      height: auto;
      margin: 15px 0 0 15px;
      border: 1px solid #f2f2f2;
      border-radius: 10px;
      box-shadow: 0 0 10px 3px rgba(0, 0, 0, 0.08);
    }
    
    .my-card__title {
      display: flex;
      flex-direction: row;
      align-items: flex-start;
      justify-content: space-between;
      padding: 15px;
      border-bottom: 1px solid #f2f2f2;
    }
    
    .my-card__title--close {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
      flex-shrink: 0;
      margin-left: 15px;
      cursor: pointer;
    }
    
    .my-card__title--close:hover {
      color: red;
    }
    
    .my-card__content {
      padding: 15px;
      min-height: 100px;
    }
  </style>
  <div class="my-card">
    <div class="my-card__title">
      <slot name="my-card-title">卡片标题</slot>
      <span class="my-card__title--close">关闭</span>
    </div>
    <div class="my-card__content">
      <slot name="my-card-content">卡片内容</slot>
    </div>
  </div>
</template>
<my-card on-remove="handleRemove" card-id="card1">
  <span slot="my-card-title">这里是卡片的标题！！！</span>
  <div slot="my-card-content" style="color: #999">通过插槽实现的卡片内容·····</div>
</my-card>
<my-card on-remove="handleRemove" card-id="card2">
  <span slot="my-card-title">这里是卡片2！！！</span>
  <div slot="my-card-content" style="color: #999">通过插槽实现的卡片内容·····</div>
</my-card>
<button id="btn">修改卡片1的标题</button>
<script>
  function handleRemove(e) {
    console.log(`删除了id为：${e} 的卡片`)
  }
  
  document.getElementById('btn').onclick = function () {
    const card = document.querySelector('my-card')
    card.shadowRoot.querySelector('.my-card__title').innerHTML = '修改了'
  }
</script>
</body>
</html>
```

```js
class MyCard extends HTMLElement {
  constructor() {
    super();
    const shadowDOM = this.attachShadow({mode: 'open'});
    const templateDOM = document.getElementById("my-card-template").content
    shadowDOM.appendChild(templateDOM.cloneNode(true))
  
  }
  
  // 监听属性变化
  static get observedAttributes() {
    return ['card-id', 'on-remove'];
  }
  
  connectedCallback() {
    console.log("自定义元素添加至页面。");
    this.closeButton = this.shadowRoot.querySelector('.my-card__title--close')
    this.closeButton.addEventListener('click', () => {
      this.removeCard()
    })
  }
  
  disconnectedCallback() {
    console.log("自定义元素从页面中移除。");
  }
  
  adoptedCallback() {
    console.log("自定义元素移动至新页面。");
  }
  
  attributeChangedCallback(name, oldValue, newValue) {
    console.log(`属性 ${name} 已变更。`);
    if (name === 'on-remove') {
      // 作为全局函数引用
      this.onRemove = window[newValue];
    } else if (name === 'card-id') {
      this.cardId = newValue;
    }
  }
  
  removeCard() {
    if (this.onRemove && typeof this.onRemove === 'function') {
      // 调用回调函数
      this.onRemove(this.cardId);
    }
    this.remove();
  }
}

customElements.define('my-card', MyCard)

```

## 最后

Web Components 是 W3C 推动的标准化技术，它通过自定义元素的方式，允许开发者在浏览器中直接使用。 最后有两个 Web Component
相关的问题

### Polyfills

[浏览器兼容性查询](https://caniuse.com/)

对于旧版浏览器不支持 Web Component 兼容性情况，可以考虑使用 polyfill 来实现兼容性。Polyfills 是一种代码注入技术，使得浏览器可以支持新的标准
API。

[Polyfill WebComponents 文档](https://www.webcomponents.org/polyfills)

### 实际应用

* **Vue3：** Vue3 引入了对 Web Components
  的原生支持 [相关文档](https://cn.vuejs.org/guide/extras/web-components.html#vue-and-web-components)
* **MicroApp:** 基于 Web Components
  的一款简约、高效、功能强大的微前端框架。 [MicroApp 文档](https://jd-opensource.github.io/micro-app/docs.html#/)
* **微软：** 微软使用基于 Web Components 技术开发的组件库 FAST 重构了 MSN 网站。另外，基于 ChatGPT 的 New Bing 也是基于
  FAST 构建的。 [FAST 文档](https://fast.design/docs/introduction/#what-are-web-components)
* **谷歌：** Google 开源了许多 Web Components，包括地图、Drive、日历等。也包括 Google 系产品 Youtube
  的播放器组件。 [GoogleWebComponents GitHub](https://github.com/GoogleWebComponents)
* **Twitter：** Twitter 2016 年开始将自己的嵌入式推文从 iframe 切换成 Web Components 中的 Shadow DOM
  技术。从而使浏览器内存占用率大幅降低，渲染时间大幅缩短，推文显示速度更快，页面滚动更流畅。[推文](https://devcommunity.x.com/t/upcoming-change-to-embedded-tweet-display-on-web/66215)
