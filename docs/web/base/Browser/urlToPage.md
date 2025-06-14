本文主要介绍，在浏览器地址栏输入 url 后到 页面 渲染完成的全过程。

作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## 1、URL 解析

### URL的定义

URL（统一资源定位符，Uniform Resource Locator）是一种用于指定互联网上资源位置的字符串。它告诉浏览器如何定位和获取资源。
组成结构：

* **协议：** 如 http（超文本传输协议：它是一个基于请求/响应模式的无状态协议。支持除文本外的富媒体资源，如图片，视频等）,
  https（https就是在http下加了SSL层从而来保护交换数据的隐私和完整。一般来说它可以通过证书等相关信息确认网站的真实性，建立加密的信息通道，保证数据内容的完整。一般用于支付类网站如：https://www.alipay.com/ ）,
  ftp（文件上传下载协议：一般用于客户端和服务器端文件的直接传输） 等，表示访问资源所用的协议。
* **主机名：** 如 www.example.com，表示资源所在的服务器地址。
* **端口号（可选）：** 如 :8080，指定服务器上的特定端口，默认情况下HTTP是80，HTTPS是443。
* **路径：** 如 /path/to/resource，表示服务器上的资源路径。
* **查询参数（可选）：** 如 ?key1=value1&key2=value2，用于向服务器传递额外的信息。
* **片段标识符（可选）：** 如 #section1，用于指向页面内的特定部分。

用户在地址栏输入一个 url 后，浏览器会解析这个 url，将各个部分拆解为独立的变量，并保存到浏览器的地址栏中。

### JS的编码和解码方式：

```encodeURI()/decodeRUI()``` 这两个函数把字符串作为URI进行编码/解码，实际上encodeURI()
函数只把参数中的空格编码为%20，汉字进行编码，其余特殊字符不会转换。

```HTML

<script>
    let str = "www.baidu.com/davina /微信";
    console.log(encodeURI(str)); //www.baidu.com/davina%20/%E5%BE%AE%E4%BF%A1

    let str1 = "www.baidu.com/davina /微信/ac1";
    console.log(encodeURI(str1)); //www.baidu.com/davina%20/%E5%BE%AE%E4%BF%A1/ac1
</script>
```

```encodeURIComponent()/decodeURIComponent()``` 这两个函数可把字符串作为URI组件进行编码/解码。由于这个方法对:
/都进行了编码，所以不能用它来对网址进行编码，而适合对URI中的参数进行编码/解码。

```html

<script>
    let uri = "https://www.davina/com/from=http://wwws.baidu.com";
    console.log(encodeURIComponent(uri)); //https%3A%2F%2Fwww.davina%2Fcom%2Ffrom%3Dhttp%3A%2F%2Fwwws.baidu.com

    //所以一般我们的用法是：
    let newUri = `https://www.davina/com/from=${encodeURIComponent(
            "https://www.baidu.com"
    )}`;
    console.log(newUri); //https://www.davina/com/from=https%3A%2F%2Fwww.baidu.com
</script>
```

```escape()/unescape()``` 函数对字符串进行编码/解码，将字符的unicode编码转化为16进制序列。它也是可以解码中文的。用于服务端与服务端传输多。它不对/进行编码。

```html

<script>
    let uri = "https://www.davina/com/from=http://wwws.baidu.com";
    console.log(escape(uri)); //https%3A//www.davina/com/from%3Dhttp%3A//wwws.baidu.com
</script>
```

## DNS 解析

DNS（Domain Name System，域名系统）是互联网上用于将域名（如www.example.com）转换为IP地址（如192.168.1.1）的协议。

1. **浏览器检查本地缓存：** 浏览器首先会检查是否有可用的DNS缓存记录。如果有且未过期，则直接使用该记录。如果没有，则
   查看c盘hosts文件，如果有浏览器会首先使用这个ip地址。如果本地hosts文件里没有，则请求本地DNS解析器
2. **操作系统检查本地缓存：** 如果浏览器缓存中没有找到对应的IP地址，浏览器会请求操作系统的DNS缓存。操作系统也会维护自己的DNS缓存。
3. **查询路由器缓存：** 如果操作系统也没有缓存记录，接下来会查询路由器的缓存。许多家用路由器也保存了最近访问过的域名和IP地址映射。
4. **查询ISP的DNS服务器：** 如果上述步骤都没有找到匹配项，浏览器会向互联网服务提供商（ISP）的DNS服务器发起查询请求。
5. **递归查询：** ISP的DNS服务器如果也不知晓该域名的IP地址，它将进行递归查询： 根域名服务器：首先询问根域名服务器（如
   .），获取顶级域名服务器（如 .com, .org 等）的信息。顶级域名服务器：然后向顶级域名服务器询问具体的权威域名服务器信息。权威域名服务器：最后从权威域名服务器处获得确切的IP地址。
6. **或迭代查询：** DNS服务器另外一种查询方式为迭代查询，当客户机发送查询请求时，DNS服务器并不直接回复查询结果，而是告诉客户机另一台DNS服务器地址，客户机再向这台DNS服务器提交请求，依次循环直到返回查询的结果为止
   总的来说：客户机的请求需要自己挨个去查询才能得到结果，服务器没有结果时只会给你提供其它服务器的地址，而不会帮你去请求查询，这与递归查询截然相反
7. **返回结果并缓存：** 一旦获取到IP地址，这个信息会被逐层返回给最初发出请求的设备，并且沿途的每个节点都会缓存这条记录以备后续使用。

## 建立 TCP 链接

浏览器通过DNS获取到web服务器真的IP地址后，便向服务器发起TCP连接请求，通过TCP的三次握手建立好连接后，浏览器便可以将http请求数据通过发送给服务器了。

`补充：TCP：用于对传输准确性要求特别高的场景。如文件传输、发送和接收邮件、远程登录等；UDP：一般用于即时通信。如语音、视频、直播等`

### TCP、UDP的区别

| TCP                 | UDP           |
|---------------------|---------------|
| 面相链接                | 无连接           |
| 提供可靠服务              | 不保证可靠交互       |
| 有状态                 | 无状态           |
| 面向字节流               | 面向报文          |
| 传输效率较慢              | 传输效率较快        |
| 有拥塞控制               | 没有拥塞控制        |
| 每一条TCP链接只能是点对点（一对一） | 支持一对一、一对多、多对多 |
| 首部开销20字节            | 首部开销8字节       |

### 三次握手

* **第一次握手（无任何状态）：**
  客户端向服务端发起建立连接请求，客户端会随机生成一个起始序列号x，客户端向服务端发送的字段包含标志位SYN=1，序列号seq=x。第一次握手后客户端的状态为SYN-SENT。此时服务端的状态为LISTEN
* **第二次握手（保证：客户端的发送能力、服务器的接收能力没问题）：**
  服务端在收到客户端发来的报文后，会随机生成一个服务端的起始序列号y，然后给客户端回复一段报文，标志位SYN=1，序列号seq=y，ACK=1，确认号ack=x+1。第二次握手后服务端的状态为SYN-RCVD（SYN=1表示要和客户端建立一个连接，ACK=1表示确认序号有效）
* **第三次握手（保证：客户端的接收能力、服务器的发送能力没问题）：**
  客户端收到服务端发来的报文后，会再向服务端发送报文。ACK=1，序列号seq=x+1，确认号ack=y+1。客户、服务端状态变为ESTABLISTED。此时连接建立完成

### TLS 握手

TLS（传输层安全，Transport Layer
Security）握手是建立安全通信连接的关键步骤。它确保了客户端和服务器之间的通信是加密的、完整的，并且可以验证双方的身份。TLS握手过程涉及密钥交换、身份验证和会话密钥协商等步骤。
（以下取自 [通义灵码](https://tongyi.aliyun.com/) ）

1. **客户端问候（Client Hello）** 客户端向服务器发送一个Client Hello消息，其中包含：
    * 支持的TLS版本。
    * 随机数（用于后续生成主密钥）。
    * 支持的加密套件（Cipher Suites），即支持的加密算法组合。
    * 支持的压缩方法（可选）。
    * 扩展字段（如支持的椭圆曲线、签名算法等）。
2. **服务器问候（Server Hello）** 服务器选择并返回一个Server Hello消息，其中包含：
    * 确定的TLS版本。
    * 服务器随机数（用于后续生成主密钥）。
    * 选定的加密套件。
    * 选定的压缩方法（如果支持）。
    * 其他扩展信息。
3. **服务器证书（Server Certificate）** 服务器发送其数字证书，通常是一个X.509格式的证书，包含公钥和其他相关信息。这个证书由受信任的证书颁发机构（CA）签发。
4. **服务器密钥交换（Server Key Exchange，可选）** 如果所选的加密套件需要额外的密钥交换参数（如Diffie-Hellman参数），服务器会发送这些参数。
5. **服务器完成（Server Hello Done）** 服务器发送Server Hello Done消息，表示服务器的初始化阶段完成。
6. **客户端密钥交换（Client Key Exchange）**
    * 客户端使用服务器提供的公钥或密钥交换参数生成预主密钥（Pre-Master Secret），并将其加密后发送给服务器。
    * 如果使用的是RSA加密，则直接用服务器的公钥加密预主密钥；如果是Diffie-Hellman，则通过DH算法协商预主密钥。
7. **变更密码规范（Change Cipher Spec）** 客户端和服务器各自发送Change Cipher Spec消息，表示从现在开始使用协商好的加密算法和密钥进行通信。
8. **完成消息（Finished）** 双方发送Finished消息，内容为使用新密钥加密的校验值（基于之前的握手消息）。这一步用于确认双方都正确地完成了握手过程，并且可以开始加密通信

## 浏览器发送 HTTP 请求

当浏览器通过DNS解析获取到目标服务器的IP地址，并且建立了TCP连接后，浏览器会发送HTTP请求以获取所需的资源。

### HTTP请求的基本结构

1. **请求行（Request Line）：**
    * 请求方法（Method）：如 GET, POST, PUT, DELETE 等。
    * 请求URI（Uniform Resource Identifier）：指定请求的资源路径。
    * HTTP版本（HTTP Version）：如 HTTP/1.1 或 HTTP/2。
2. **请求头（Request Headers）：**
   包含各种元数据，如用户代理（User-Agent）、接受的内容类型（Accept）、内容类型（Content-Type）、主机名（Host）、授权信息（Authorization）等。
3. **空行：** 请求头和请求体之间用一个空行分隔。
4. **请求体（Request Body，可选）：** 仅在某些请求方法（如 POST 或 PUT）中包含，用于发送数据到服务器。

## 服务器响应 HTTP 请求

当浏览器发送HTTP请求后，服务器会处理该请求并返回一个HTTP响应。HTTP响应包含状态信息、元数据和可能的实体内容（如HTML页面、JSON数据等）。

### HTTP 响应的基本结构

1. **状态行（Status Line）：**
    * HTTP版本（HTTP Version）：如 HTTP/1.1 或 HTTP/2。
    * 状态码（Status Code）：如 200 OK、404 Not Found 等。
    * 状态描述（Reason Phrase）：与状态码对应的描述性短语。
2. **响应头（Response Headers）：** 包含各种元数据，如服务器名称（Server）、内容类型（Content-Type）、内容长度（Content-Length）等。
3. **空行：** 响应头和响应体之间用一个空行分隔。
4. **响应体（Response Body）：** 服务器返回的实体内容，如HTML页面、JSON数据等。

### HTTP 响应的状态码分类

1. **1xx：** 信息性状态码，用于通知客户端请求正在处理中。
2. **2xx：** 成功状态码，表示请求已成功处理。
3. **3xx：** 重定向状态码，表示请求需要重定向到另一个URL。
4. **4xx：** 客户端错误状态码，表示请求错误，客户端需要修改请求。
5. **5xx：** 服务器错误状态码，表示服务器处理请求时发生错误。

### 处理响应的过程

1. **解析响应行：** 服务器返回的响应行包含状态码、状态描述和HTTP版本。
2. **解析响应头：** 服务器返回的响应头包含各种元数据，如服务器名称、内容类型、内容长度等。
3. **解析响应体：** 服务器返回的响应体包含实体内容，如HTML页面、JSON数据等。
4. **处理响应：** 根据响应的状态码和响应头，客户端可以决定如何处理响应。例如，如果状态码为200，则表示请求成功，客户端可以解析响应体并显示在浏览器中。如果状态码为404，则表示请求的资源不存在

## TCP 链接释放 （四次挥手）

TCP的四次挥手（也称为四次波）是用于终止TCP连接的过程。与三次握手建立连接不同，关闭连接需要四次交互，以确保双方都能正确地结束会话并释放资源
（以下取自 [通义灵码](https://tongyi.aliyun.com/) ）

1. **第一次挥手（客户端发送FIN报文）：**
    * 客户端准备关闭连接，向服务端发送一个FIN（Finish）报文段，表示客户端已经没有数据要发送了。
    * 报文中包含标志位FIN=1和序列号seq=u（u为当前序列号）。
    * 客户端进入FIN-WAIT-1状态。
2. **第二次挥手（服务端确认收到FIN报文）：**
    * 服务端接收到客户端的FIN报文后，发送一个ACK（Acknowledgment）报文段作为确认。
    * 报文中包含标志位ACK=1、确认号ack=u+1（即客户端FIN报文的序列号加1），以及服务端自己的序列号seq=v。
    * 此时，客户端进入FIN-WAIT-2状态，等待服务端的进一步操作；服务端进入CLOSE-WAIT状态，等待应用程序关闭连接。
3. **第三次挥手（服务端发送FIN报文）：**
    * 服务端的应用程序在完成所有数据传输后，也会向客户端发送一个FIN报文段，表示服务端也没有数据要发送了。
    * 报文中包含标志位FIN=1、序列号seq=w（w为当前序列号），以及确认号ack=u+1。
    * 服务端进入LAST-ACK状态，等待客户端的最终确认。
4. **第四次挥手（客户端确认收到FIN报文）：**
    * 客户端接收到服务端的FIN报文后，发送一个ACK报文段作为确认。
    * 报文中包含标志位ACK=1、确认号ack=w+1（即服务端FIN报文的序列号加1），以及客户端自己的序列号seq=u+1。
    * 客户端进入TIME-WAIT状态，等待一段时间（通常是2个最大报文段生命周期MSL，Maximum Segment
      Lifetime）以确保服务端能够收到最终的ACK报文。之后，客户端关闭连接，进入CLOSED状态。
    * 服务端收到ACK报文后，立即关闭连接，进入CLOSED状态。

简单的说就是：

1. 浏览器所在主机向服务器发出连接释放报文，然后停止发送数据；
2. 服务器接收到释放报文后发出确认报文，然后将服务器上未传送完的数据发送完；
3. 服务器数据传输完毕后，向客户机发送连接释放报文；
4. 客户机接收到报文后，发出确认，然后等待一段时间后，释放TCP连接

## 客户端渲染

在浏览器地址栏输入URL并完成HTTP请求和响应过程后，浏览器需要将接收到的资源（通常是HTML文档）解析并渲染成用户可见的页面。

### 解析HTML文档（Parsing HTML）

**构建DOM树（Document Object Model）：**

* 浏览器从服务器接收到HTML文档后，首先会解析HTML内容并构建DOM树。DOM树是一个树状结构，每个节点代表HTML文档中的一个元素（如标签、文本节点等）。
* 浏览器使用HTML解析器（如WebKit的HTMLParser或Blink的HTMLParser）来解析HTML文档。

### 构建CSSOM树（CSS Object Model）

**解析CSS样式：**

* 浏览器会解析HTML文档中引用的CSS样式表，并构建CSSOM树。CSSOM树是一个树状结构，每个节点代表一个CSS规则。
* 浏览器使用CSS解析器（如WebKit的CSSParser或Blink的CSSParser）来解析CSS样式。

### 构建渲染树（Rendering Tree）

**合并DOM和CSSOM：**

* 浏览器将DOM树和CSSOM树合并，生成渲染树。渲染树中的每个节点包含一个DOM节点及其对应的样式信息。
* 渲染树中的节点称为渲染对象（Render Objects），每个渲染对象包含布局信息（如位置、大小）和绘制信息（如颜色、字体）。

### 布局（Layout）

**计算布局信息：**

* 浏览器根据渲染树中的样式信息计算每个渲染对象的布局信息（如位置、大小）。这个过程称为布局或重排（Reflow）。
* 布局引擎（如WebKit的RenderFlow或Blink的LayoutNG）负责计算布局信息。

### 绘制（Painting）

**绘制渲染对象：**

* 浏览器根据渲染树中的布局信息绘制每个渲染对象。这个过程称为绘制或重绘（Repaint）。
* 绘制引擎（如WebKit的GraphicsLayer或Blink的Paint）负责绘制渲染对象。

### 合成（Compositing）

**组合图层：**

* 浏览器将多个图层（如背景图层、内容图层、滚动图层等）组合在一起，生成最终的显示图像。
* 合成引擎（如WebKit的CompositingLayer或Blink的Compositor）负责组合图层。

### 优化渲染性能

* **增量更新：** 仅重新计算和绘制发生变化的部分，而不是整个页面。
* **缓存：** 缓存解析后的DOM树、CSSOM树和渲染树，减少重复计算。
* **异步解析：** 使用异步解析技术，避免阻塞主线程。
* **硬件加速：** 利用GPU加速绘制和合成过程，提高渲染速度。
