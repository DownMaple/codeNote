---
prev: false
next: false
---

# Vue3中使用TSX

[Vue3](https://cn.vuejs.org/) 除了提供模板语法外，还支持使用JSX/TSX来编写组件。TSX结合了TypeScript的类型检查和JSX的表达能力，为Vue开发提供了更灵活的选择。

## 环境配置

要在Vue3项目中使用TSX，需要根据不同的构建工具进行配置。以下分别介绍Vite和Webpack环境下的配置方法。

### Vite项目配置 (Vite 6.0+)

Vite是Vue团队推荐的现代构建工具，对TSX的支持非常友好。

1. 安装必要的依赖：

```bash
# 适用于Vite 6.0+
npm install @vitejs/plugin-vue-jsx -D
# 或
yarn add @vitejs/plugin-vue-jsx -D
# 或
pnpm add @vitejs/plugin-vue-jsx -D
```

2. 在`vite.config.ts`中配置：

```ts
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'

export default defineConfig({
  plugins: [
    vue(),
    vueJsx({
      // 可选配置项
      include: /\.[jt]sx$/,  // 默认处理的文件类型
      // 传递给@vue/babel-plugin-jsx的选项
      transformOn: true,     // 启用 onClick 转换
      enableObjectSlots: true, // 启用对象插槽语法
      // 可以指定自定义组件定义函数名
      defineComponentName: ['defineComponent']
    })
  ]
})
```

3. 在`tsconfig.json`中确保JSX支持：

```json
{
  "compilerOptions": {
    "jsx": "preserve",
    "jsxFactory": "h",
    "jsxFragmentFactory": "Fragment"
  }
}
```

### Webpack项目配置 (Webpack 5+)

对于使用Webpack 5的项目（包括Vue CLI创建的项目），配置方式如下：

1. 安装必要的依赖：

```bash
# 对于Webpack 5+项目
npm install @vue/babel-plugin-jsx -D
npm install @babel/preset-typescript -D
# 或
yarn add @vue/babel-plugin-jsx -D
yarn add @babel/preset-typescript -D
# 或
pnpm add @vue/babel-plugin-jsx -D
pnpm add @babel/preset-typescript -D
```

2. 在`babel.config.js`中配置：

```js
module.exports = {
  presets: [
    '@vue/cli-plugin-babel/preset',
    ['@babel/preset-typescript', { isTSX: true, allExtensions: true }]
  ],
  plugins: [
    [
      '@vue/babel-plugin-jsx',
      {
        // 启用转换Vue 3 Composition API
        enableObjectSlots: true,
        // 支持v-model简写
        transformOn: true,
        // 可选：优化渲染性能
        optimize: false
      }
    ]
  ]
}
```

3. 如果是纯Webpack项目（非Vue CLI），需要在`webpack.config.js`中添加：

```js
module.exports = {
  // ...其他配置
  module: {
    rules: [
      {
        test: /\.(jsx|tsx)$/,
        exclude: /node_modules/,
        use: [
          {
            loader: 'babel-loader',
            options: {
              plugins: [
                [
                  '@vue/babel-plugin-jsx',
                  {
                    enableObjectSlots: true,
                    transformOn: true
                  }
                ]
              ],
              presets: [
                ['@babel/preset-typescript', { isTSX: true, allExtensions: true }]
              ]
            }
          }
        ]
      },
      // ...其他规则
    ]
  },
  resolve: {
    extensions: ['.js', '.jsx', '.ts', '.tsx', '.vue']
  }
}
```

### 使用unplugin-vue-jsx (多构建工具支持)

如果你需要同时支持Vue 2和Vue 3，或者需要在多种构建工具中保持一致的配置，可以使用`unplugin-vue-jsx`：

```bash
npm install unplugin-vue-jsx -D
# 或
yarn add unplugin-vue-jsx -D
# 或
pnpm add unplugin-vue-jsx -D
```

然后根据你的构建工具进行配置：

**Vite配置**：
```js
// vite.config.ts
import VueJsx from 'unplugin-vue-jsx/vite'

export default defineConfig({
  plugins: [
    VueJsx({
      // 自动检测Vue版本
      version: 'auto',
      // 其他选项
      sourceMap: true
    })
  ]
})
```

**Webpack配置**：
```js
// webpack.config.js
module.exports = {
  plugins: [
    require('unplugin-vue-jsx/webpack')({
      version: 'auto',
      sourceMap: true
    })
  ]
}
```

## TSX基础用法

### 基本语法

在Vue3中使用TSX时，需要通过`defineComponent`函数来定义组件：

```tsx
import { defineComponent, ref } from 'vue'

export default defineComponent({
  name: 'HelloWorld',
  props: {
    msg: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const count = ref(0)
    
    const increment = () => {
      count.value++
    }
    
    return () => (
      <div>
        <h1>{props.msg}</h1>
        <p>Count: {count.value}</p>
        <button onClick={increment}>Increment</button>
      </div>
    )
  }
})
```

### 事件处理

在TSX中，事件处理使用驼峰命名法，并直接传递函数：

```tsx
// 错误写法（Vue模板写法）
<button v-on:click={handleClick}>点击</button>

// 正确写法
<button onClick={handleClick}>点击</button>
```

### 条件渲染和列表渲染

TSX中使用JavaScript原生语法进行条件渲染和列表渲染：

```tsx
// 条件渲染
{isShow.value ? <div>显示内容</div> : null}

// 列表渲染
<ul>
  {items.value.map(item => (
    <li key={item.id}>{item.text}</li>
  ))}
</ul>
```

### 插槽使用

在TSX中使用插槽需要通过特殊语法：

```tsx
// 默认插槽
<MyComponent>
  {() => <div>默认插槽内容</div>}
</MyComponent>

// 具名插槽
<MyComponent v-slots={{
  header: () => <header>头部内容</header>,
  footer: () => <footer>底部内容</footer>
}}>
  {() => <div>默认插槽内容</div>}
</MyComponent>
```

## setup语法与TSX结合

Vue3.2引入了`<script setup>`语法糖，在TSX中我们可以通过不同方式实现类似的效果。

### 函数式组件

最简单的方式是使用函数式组件：

```tsx
import { defineComponent, ref } from 'vue'

// 函数式组件
const SimpleCounter = () => {
  const count = ref(0)
  
  return (
    <div>
      <p>Count: {count.value}</p>
      <button onClick={() => count.value++}>Increment</button>
    </div>
  )
}

// 导出组件
export default SimpleCounter
```

### 使用setup函数

结合`defineComponent`和`setup`函数：

```tsx
import { defineComponent, ref } from 'vue'

export default defineComponent({
  props: {
    initial: {
      type: Number,
      default: 0
    }
  },
  setup(props, { slots, emit }) {
    const count = ref(props.initial)
    
    const increment = () => {
      count.value++
      emit('update', count.value)
    }
    
    return () => (
      <div>
        <p>Count: {count.value}</p>
        <button onClick={increment}>Increment</button>
        {slots.default?.()}
      </div>
    )
  }
})
```

### 组合式API与TSX

可以将逻辑提取到组合式函数中，然后在TSX中使用：

```tsx
// useCounter.ts
import { ref } from 'vue'

export function useCounter(initial = 0) {
  const count = ref(initial)
  
  const increment = () => {
    count.value++
  }
  
  const decrement = () => {
    count.value--
  }
  
  return {
    count,
    increment,
    decrement
  }
}

// Counter.tsx
import { defineComponent } from 'vue'
import { useCounter } from './useCounter'

export default defineComponent({
  props: {
    initial: {
      type: Number,
      default: 0
    }
  },
  setup(props) {
    const { count, increment, decrement } = useCounter(props.initial)
    
    return () => (
      <div class="counter">
        <button onClick={decrement}>-</button>
        <span>{count.value}</span>
        <button onClick={increment}>+</button>
      </div>
    )
  }
})
```

## .setup.tsx 文件格式

Vue3提供了一种特殊的文件格式 `.setup.tsx`，它允许你在单文件组件中结合使用Vue模板语法和TSX。这种方式特别适合需要灵活渲染逻辑的场景。

### 基本用法

创建一个 `.setup.tsx` 文件，例如 `Counter.setup.tsx`：

```tsx
import { ref, defineComponent } from 'vue'

// 使用 <script setup lang="tsx"> 语法
export default defineComponent({
  name: 'Counter',
  props: {
    initial: {
      type: Number,
      default: 0
    }
  },
  setup(props) {
    const count = ref(props.initial)
    
    const increment = () => {
      count.value++
    }
    
    return () => (
      <div>
        <p>Count: {count.value}</p>
        <button onClick={increment}>Increment</button>
      </div>
    )
  }
})
```

### 在Vue SFC中使用TSX

你也可以在常规的Vue单文件组件中使用TSX，只需将`<script>`标签的`lang`属性设置为`tsx`：

```vue
<script lang="tsx">
import { defineComponent, ref } from 'vue'

export default defineComponent({
  name: 'MyComponent',
  setup() {
    const count = ref(0)
    
    return () => (
      <div>
        <p>Count: {count.value}</p>
        <button onClick={() => count.value++}>Increment</button>
      </div>
    )
  }
})
</script>

<style scoped>
/* 样式部分 */
</style>
```

### 在script setup中使用TSX

Vue 3.2+支持在`<script setup>`中直接使用TSX，这是一种更简洁的方式：

```vue
<script setup lang="tsx">
import { ref } from 'vue'

// 定义状态
const count = ref(0)
const increment = () => count.value++

// 定义TSX渲染函数
const renderCounter = () => (
  <div class="counter">
    <p>Count: {count.value}</p>
    <button onClick={increment}>Increment</button>
  </div>
)
</script>

<template>
  <!-- 在模板中使用TSX渲染函数 -->
  <div>
    <h2>Counter Component</h2>
    <component :is="renderCounter()" />
  </div>
</template>

<style scoped>
.counter {
  padding: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
}
</style>
```

在上面的例子中，我们在`<script setup lang="tsx">`中定义了一个TSX渲染函数`renderCounter`，然后在Vue模板中通过`:is`动态组件语法使用它。这种方式结合了Vue模板的简洁性和TSX的灵活性。

### 混合使用的优势

在Vue模板中使用TSX渲染函数有以下优势：

1. **灵活处理复杂逻辑**：对于包含复杂条件渲染或动态组件的部分，可以使用TSX
2. **类型安全**：TSX部分享有完整的TypeScript类型检查
3. **代码复用**：可以将TSX渲染逻辑封装为函数，在多个地方复用
4. **保留Vue优势**：仍然可以使用Vue的指令、过渡效果等特性

### 实际示例

下面是一个更复杂的例子，展示如何在Vue模板中混合使用TSX：

```vue
<script setup lang="tsx">
import { ref, computed } from 'vue'

interface TableColumn {
  key: string
  title: string
  render?: (row: any) => JSX.Element
}

interface DataRow {
  id: number
  name: string
  age: number
  status: 'active' | 'inactive'
}

// 数据
const data = ref<DataRow[]>([
  { id: 1, name: '张三', age: 28, status: 'active' },
  { id: 2, name: '李四', age: 32, status: 'inactive' },
  { id: 3, name: '王五', age: 45, status: 'active' }
])

// 列定义
const columns: TableColumn[] = [
  { key: 'name', title: '姓名' },
  { key: 'age', title: '年龄' },
  { 
    key: 'status', 
    title: '状态',
    render: (row) => (
      <span class={row.status === 'active' ? 'status-active' : 'status-inactive'}>
        {row.status === 'active' ? '活跃' : '非活跃'}
      </span>
    )
  },
  {
    key: 'actions',
    title: '操作',
    render: (row) => (
      <div class="actions">
        <button onClick={() => handleEdit(row)}>编辑</button>
        <button onClick={() => handleDelete(row.id)}>删除</button>
      </div>
    )
  }
]

// 处理函数
const handleEdit = (row: DataRow) => {
  console.log('编辑:', row)
}

const handleDelete = (id: number) => {
  data.value = data.value.filter(row => row.id !== id)
}

// 复杂表格渲染函数
const renderTable = () => (
  <table class="data-table">
    <thead>
      <tr>
        {columns.map(column => (
          <th key={column.key}>{column.title}</th>
        ))}
      </tr>
    </thead>
    <tbody>
      {data.value.map(row => (
        <tr key={row.id}>
          {columns.map(column => (
            <td key={`${row.id}-${column.key}`}>
              {column.render 
                ? column.render(row)
                : row[column.key as keyof DataRow]}
            </td>
          ))}
        </tr>
      ))}
    </tbody>
  </table>
)
</script>

<template>
  <div class="table-container">
    <h2>用户数据表格</h2>
    <!-- 使用TSX渲染的表格 -->
    <component :is="renderTable()" />
  </div>
</template>

<style scoped>
.data-table {
  width: 100%;
  border-collapse: collapse;
}
.data-table th, .data-table td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}
.status-active {
  color: green;
  font-weight: bold;
}
.status-inactive {
  color: gray;
}
.actions {
  display: flex;
  gap: 8px;
}
.actions button {
  padding: 4px 8px;
  cursor: pointer;
}
</style>
```

在这个例子中，我们使用TSX来渲染一个复杂的表格，包含自定义单元格渲染和事件处理，同时保留了Vue单文件组件的结构和样式隔离的优势。

## 实际案例：TodoList组件

下面是一个完整的TodoList组件示例，展示了如何在Vue3中使用TSX：

```tsx
import { defineComponent, ref, computed } from 'vue'
import './TodoList.css'

interface Todo {
  id: number
  text: string
  completed: boolean
}

export default defineComponent({
  name: 'TodoList',
  setup() {
    const newTodo = ref('')
    const todos = ref<Todo[]>([
      { id: 1, text: '学习Vue3', completed: false },
      { id: 2, text: '学习TSX', completed: false },
      { id: 3, text: '构建项目', completed: false }
    ])
    
    const completedCount = computed(() => 
      todos.value.filter(todo => todo.completed).length
    )
    
    const remainingCount = computed(() => 
      todos.value.filter(todo => !todo.completed).length
    )
    
    const addTodo = () => {
      if (newTodo.value.trim()) {
        todos.value.push({
          id: Date.now(),
          text: newTodo.value,
          completed: false
        })
        newTodo.value = ''
      }
    }
    
    const toggleTodo = (id: number) => {
      const todo = todos.value.find(todo => todo.id === id)
      if (todo) {
        todo.completed = !todo.completed
      }
    }
    
    const removeTodo = (id: number) => {
      todos.value = todos.value.filter(todo => todo.id !== id)
    }
    
    return () => (
      <div class="todo-list">
        <h1>Todo List</h1>
        
        <div class="add-todo">
          <input
            type="text"
            value={newTodo.value}
            onInput={(e) => { newTodo.value = (e.target as HTMLInputElement).value }}
            placeholder="添加新任务"
          />
          <button onClick={addTodo}>添加</button>
        </div>
        
        <ul class="todos">
          {todos.value.map(todo => (
            <li key={todo.id} class={{ completed: todo.completed }}>
              <input
                type="checkbox"
                checked={todo.completed}
                onChange={() => toggleTodo(todo.id)}
              />
              <span>{todo.text}</span>
              <button onClick={() => removeTodo(todo.id)}>删除</button>
            </li>
          ))}
        </ul>
        
        <div class="todo-stats">
          <span>完成: {completedCount.value}</span>
          <span>剩余: {remainingCount.value}</span>
        </div>
      </div>
    )
  }
})
```

对应的CSS样式：

```css
.todo-list {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
}

.add-todo {
  display: flex;
  margin-bottom: 20px;
}

.add-todo input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px 0 0 4px;
}

.add-todo button {
  padding: 8px 16px;
  background-color: #4caf50;
  color: white;
  border: none;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
}

.todos {
  list-style: none;
  padding: 0;
}

.todos li {
  display: flex;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.todos li.completed span {
  text-decoration: line-through;
  color: #888;
}

.todos li span {
  flex: 1;
  margin: 0 10px;
}

.todos li button {
  padding: 4px 8px;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.todo-stats {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  color: #666;
}
```

## 总结

使用TSX编写Vue3组件有以下优势：

1. **类型安全**：结合TypeScript提供更强的类型检查
2. **灵活性**：可以充分利用JavaScript表达能力
3. **动态渲染**：更容易处理动态组件和条件渲染
4. **IDE支持**：更好的代码补全和错误提示

当然，TSX也有一些缺点，如语法较为复杂、学习曲线陡峭等。在选择是否使用TSX时，应根据项目需求和团队熟悉度进行权衡。

Vue3提供了多种方式使用TSX：
- 纯TSX组件（.tsx文件）
- 在Vue SFC中使用TSX（`<script lang="tsx">`）
- 在script setup中使用TSX（`<script setup lang="tsx">`）
- 混合使用Vue模板和TSX渲染函数

这些灵活的选择让开发者可以根据具体场景选择最合适的开发方式。
