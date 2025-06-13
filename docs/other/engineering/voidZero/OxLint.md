---
prev: false
next: false
---

# OxLint 使用指南

[OxLint](https://oxc.rs/) 是一个基于 Rust 编写的 JavaScript/TypeScript 代码检查工具，相比传统的 ESLint，其性能提升了 50-100 倍。OxLint 是 Oxc (The Oxidation Compiler) 项目的一部分，该项目旨在为 JavaScript 和 TypeScript 提供高性能的工具链，包括解析器、检查器、格式化器、转译器等。

## 为什么选择 OxLint？

### 核心优势

1. **极致性能**：比 ESLint 快 50-100 倍，并且可以根据 CPU 核心数量进行扩展
2. **内存安全**：基于 Rust 的内存安全保证消除了某些类别的错误
3. **兼容性**：支持 `.eslintignore` 和 ESLint 注释禁用语法
4. **零配置**：开箱即用，无需复杂配置
5. **增强诊断**：提供更明确的错误信息，帮助开发者快速定位问题

### 实际效果

- Shopify 报告：原本需要 75 分钟的 ESLint 检查，使用 OxLint 只需 10 秒
- Vue 创建者尤雨溪体验：在 Vue 3 代码库上运行，约 200 条规则 + 约 590 个文件，首次运行仅需 50ms，再次运行仅需 30ms

## 安装方法

### 临时使用

可以直接在项目根目录运行：

```bash
# 使用 npm
npx oxlint@latest

# 使用 pnpm
pnpm dlx oxlint@latest

# 使用 yarn
yarn dlx oxlint@latest

# 使用 bun
bunx oxlint@latest

# 使用 deno
deno run npm:oxlint@latest
```

### 安装到项目中

```bash
# 使用 npm
npm add -D oxlint

# 使用 pnpm
pnpm add -D oxlint

# 使用 yarn
yarn add -D oxlint

# 使用 bun
bun add -D oxlint
```

## 基本使用

### 命令行使用

```bash
# 基本用法
oxlint

# 启用所有规则（包括警告）
oxlint --all

# 检查特定文件或目录
oxlint src/

# 修复可自动修复的问题
oxlint --fix

# 输出为 JSON 格式
oxlint --json
```

### VSCode 扩展

OxLint 提供了官方 VSCode 扩展，可以在 VSCode 扩展市场搜索 "oxc" 安装。

## 配置文件

OxLint 支持通过 `.oxlintrc.json` 文件进行配置。文件可以放置在项目根目录，或者在命令行中使用 `--config` 参数指定配置文件路径。

### 基本配置示例

```json
{
  "excludeFiles": ["dist/*", "node_modules/*"],
  "categories": {
    "correctness": "error",
    "suspicious": "warn",
    "style": "off"
  },
  "rules": {
    "no-console": "warn",
    "no-debugger": "error",
    "eqeqeq": ["error", "always"]
  }
}
```

## 配置项详解

### 排除文件

使用 `excludeFiles` 字段定义要排除的文件或目录：

```json
{
  "excludeFiles": [
    "dist/*",
    "node_modules/*",
    "coverage/*",
    "build/*",
    "**/*.test.js",
    "**/*.spec.js"
  ]
}
```

### 规则分类

OxLint 将规则分为几个主要类别，可以通过 `categories` 字段设置整个类别的规则级别：

```json
{
  "categories": {
    "correctness": "error", // 正确性问题，默认为 "error"
    "suspicious": "warn",   // 可疑代码，默认为 "off"
    "style": "off",         // 代码风格，默认为 "off"
    "pedantic": "off",      // 吹毛求疵的规则，默认为 "off"
    "perf": "warn"          // 性能相关，默认为 "off"
  }
}
```

### 规则配置

使用 `rules` 字段可以单独配置每条规则：

```json
{
  "rules": {
    // 简单配置，指定级别
    "no-console": "warn",
    
    // 带选项的配置
    "eqeqeq": ["error", "always"],
    
    // 禁用规则
    "no-empty": "off"
  }
}
```

规则级别可以是：
- `"error"` 或 `2`：将问题视为错误（退出代码为 1）
- `"warn"` 或 `1`：将问题视为警告（不影响退出代码）
- `"off"` 或 `0`：禁用规则

### 嵌套配置

OxLint 支持嵌套配置，可以为不同目录或文件设置不同的规则：

```json
{
  "categories": {
    "correctness": "error"
  },
  "nested": {
    "src/legacy/": {
      "categories": {
        "correctness": "warn"
      },
      "rules": {
        "no-var": "off"
      }
    }
  }
}
```

## 常用规则列表

OxLint 支持 500+ 条规则，包括来自 ESLint、TypeScript、React、Jest、Unicorn、JSX-a11y 和 Import 等的规则。以下是一些常用规则：

### 核心规则

| 规则名 | 类别 | 说明 |
|-------|------|------|
| `no-console` | `style` | 禁止使用 console |
| `eqeqeq` | `correctness` | 要求使用 === 和 !== |
| `no-unused-vars` | `correctness` | 禁止未使用的变量 |
| `no-undef` | `correctness` | 禁止未声明的变量 |
| `no-empty` | `suspicious` | 禁止空块语句 |
| `no-duplicate-case` | `correctness` | 禁止重复的 case 标签 |
| `no-dupe-keys` | `correctness` | 禁止对象字面量中的重复键 |

### TypeScript 规则

| 规则名 | 类别 | 说明 |
|-------|------|------|
| `ts/no-explicit-any` | `suspicious` | 禁止使用 any 类型 |
| `ts/no-unnecessary-type-assertion` | `suspicious` | 禁止不必要的类型断言 |
| `ts/no-non-null-assertion` | `suspicious` | 禁止使用非空断言 |

### React 规则

| 规则名 | 类别 | 说明 |
|-------|------|------|
| `react/no-danger` | `suspicious` | 禁止使用危险的 JSX 属性 |
| `react/jsx-key` | `correctness` | 要求 JSX 中的数组元素有 key |
| `react/no-children-prop` | `correctness` | 禁止使用 children 作为 prop |

## 禁用规则

### 在文件中禁用

可以使用注释在文件中禁用规则：

```javascript
// 禁用整个文件的规则检查
/* eslint-disable */

// 禁用特定规则
/* eslint-disable no-console, no-unused-vars */

// 禁用下一行的规则
// eslint-disable-next-line no-console
console.log('Hello');

// 禁用当前行的规则
console.log('World'); // eslint-disable-line no-console
```

## 与 ESLint 集成

如果你的项目中已经使用了 ESLint，但想利用 OxLint 的性能优势，可以：

1. 安装 `eslint-plugin-oxlint`：
   ```bash
   npm add -D eslint-plugin-oxlint
   ```

2. 在 ESLint 配置中使用：

   **对于 ESLint v9.0 及以上（Flat Config）**：
   ```js
   // eslint.config.js
   import oxlint from 'eslint-plugin-oxlint';
   
   export default [
     // 其他配置...
     ...oxlint.configs['flat/recommended'], // oxlint 应该是最后一个
   ];
   ```

   **对于 ESLint v9.0 以下（传统配置）**：
   ```js
   // .eslintrc.js
   module.exports = {
     // 其他配置...
     extends: [
       // 其他预设...
       "plugin:oxlint/recommended",
     ],
   }
   ```

3. 在 CI/CD 或 lint-staged 中先运行 OxLint，再运行 ESLint，这样大部分问题会被 OxLint 快速检测出来。

## GitHub Actions 集成

可以在 GitHub Actions 工作流中添加 OxLint 检查：

```yaml
jobs:
  oxlint:
    name: Lint JS/TS
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npx --yes oxlint@latest --deny-warnings # 使用最新版本
```

## 与 lint-staged 集成

在 `package.json` 中配置 lint-staged：

```json
{
  "lint-staged": {
    "**/*.{js,mjs,cjs,jsx,ts,mts,cts,tsx,vue,astro,svelte}": "oxlint"
  }
}
```

## 总结

OxLint 提供了一种高性能的 JavaScript/TypeScript 代码检查解决方案，特别适合大型项目或对性能要求高的场景。虽然 OxLint 目前生态系统不如 ESLint 成熟，但其性能优势和不断完善的规则使其成为一个很有前景的选择。

官方建议在工作流中先运行 OxLint 再运行 ESLint，这样可以发挥 OxLint 的速度优势，同时保留 ESLint 的丰富功能。随着 OxLint 的不断发展，未来可能会有更多项目选择完全迁移到 OxLint。

## 参考资料

- [OxLint 官方文档](https://oxc.rs/docs/guide/usage/linter.html)
- [OxLint GitHub 仓库](https://github.com/oxc-project/oxc)
- [eslint-plugin-oxlint](https://github.com/oxc-project/eslint-plugin-oxlint)
