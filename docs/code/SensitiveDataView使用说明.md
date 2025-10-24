# SensitiveDataView 使用说明

## 概述

`SensitiveDataView` 是一个自定义的Android组件，用于安全地显示敏感数据（如手机号、身份证号、邮箱等），支持数据脱敏和可视性切换功能。

## 功能特性

- 🔒 **数据脱敏**: 支持多种数据类型的自动脱敏显示
- 👁️ **可视性切换**: 点击眼睛图标可切换显示/隐藏状态
- 🔐 **AES解密**: 自动检测并解密AES加密的数据
- 🎨 **自定义样式**: 支持自定义脱敏字符、位置等
- 🚫 **禁用功能**: 可禁用切换功能，仅显示脱敏数据

## 支持的数据类型

| 类型 | 说明 | 脱敏规则 | 示例 |
|------|------|----------|------|
| `phone` | 手机号 | 保留前3位和后4位 | 138****5678 |
| `idCard` | 身份证号 | 保留前6位和后4位 | 110101********1234 |
| `email` | 邮箱地址 | 保留@前2位和@后全部 | ex****@email.com |
| `custom` | 自定义 | 根据maskStart和maskEnd设置 | 可自定义 |

## XML属性

```xml
<declare-styleable name="SensitiveDataView">
    <!-- 数据类型 -->
    <attr name="dataType" format="enum">
        <enum name="phone" value="0" />
        <enum name="idCard" value="1" />
        <enum name="email" value="2" />
        <enum name="custom" value="3" />
    </attr>
    
    <!-- 默认是否可见 -->
    <attr name="defaultVisible" format="boolean" />
    
    <!-- 是否禁用切换功能 -->
    <attr name="disabled" format="boolean" />
    
    <!-- 自定义脱敏起始位置 -->
    <attr name="maskStart" format="integer" />
    
    <!-- 自定义脱敏结束位置 -->
    <attr name="maskEnd" format="integer" />
    
    <!-- 自定义脱敏字符 -->
    <attr name="maskChar" format="string" />
</declare-styleable>
```

## 使用方法

### 1. 在XML布局中使用

```xml
<!-- 手机号脱敏展示 -->
<com.jnrl.home.views.SensitiveDataView
    android:id="@+id/sensitive_phone"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:dataType="phone"
    app:defaultVisible="false" />

<!-- 身份证脱敏展示 -->
<com.jnrl.home.views.SensitiveDataView
    android:id="@+id/sensitive_id_card"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:dataType="idCard"
    app:defaultVisible="false" />

<!-- 自定义脱敏规则 -->
<com.jnrl.home.views.SensitiveDataView
    android:id="@+id/sensitive_custom"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:dataType="custom"
    app:defaultVisible="false"
    app:maskStart="2"
    app:maskEnd="3"
    app:maskChar="#" />

<!-- 禁用切换功能 -->
<com.jnrl.home.views.SensitiveDataView
    android:id="@+id/sensitive_disabled"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:dataType="phone"
    app:defaultVisible="true"
    app:disabled="true" />
```

### 2. 在代码中使用

```kotlin
// 获取组件实例
val sensitiveDataView = findViewById<SensitiveDataView>(R.id.sensitive_phone)

// 设置数据（支持加密和未加密数据）
sensitiveDataView.setValue("13812345678")

// 设置加密数据
val encryptedPhone = SecurityUtils.aesEncrypt("13812345678")
sensitiveDataView.setValue(encryptedPhone)

// 设置切换监听器
sensitiveDataView.onToggleListener = { isVisible ->
    println("数据显示状态: ${if (isVisible) "显示" else "隐藏"}")
}

// 程序控制显示/隐藏
sensitiveDataView.setVisible(true)  // 显示原始数据
sensitiveDataView.setVisible(false) // 显示脱敏数据

// 检查当前状态
val isVisible = sensitiveDataView.isVisible()
```

### 3. 动态创建

```kotlin
val sensitiveDataView = SensitiveDataView(context).apply {
    dataType = SensitiveDataView.DataType.PHONE
    defaultVisible = false
    setValue("13812345678")
    onToggleListener = { isVisible ->
        // 处理状态变化
    }
}

// 添加到布局
parentLayout.addView(sensitiveDataView)
```

## 在CreateOrEditWorkOrderViewModel中的集成

该组件已集成到工单系统中，实现了以下功能：

### 1. 创建工单时的加密

```kotlin
// 在createOrUpdateWorkOrder方法中
val submitBean = CreateOrEditWorkOrderBean().apply {
    // ... 其他属性
    contactMobile = encryptContactMobile(createOrEditWorkOrderBean.contactMobile)
}
```

### 2. 编辑工单时的解密

```kotlin
// 在getWorkOrderDetailData方法中
createOrEditWorkOrderBean.contactMobile = decryptContactMobile(workOrderDetailDataBean.contactMobile)
```

### 3. 加密解密方法

```kotlin
/**
 * 加密联系人手机号
 */
private fun encryptContactMobile(contactMobile: String?): String? {
    return contactMobile?.takeIf { it.isNotBlank() }?.let { mobile ->
        try {
            // 检查是否已经是加密数据
            if (SecurityUtils.isAESEncryptedData(mobile)) {
                mobile
            } else {
                SecurityUtils.aesEncrypt(mobile)
            }
        } catch (e: Exception) {
            mobile // 加密失败时返回原始数据
        }
    }
}

/**
 * 解密联系人手机号用于显示
 */
private fun decryptContactMobile(encryptedMobile: String?): String? {
    return encryptedMobile?.takeIf { it.isNotBlank() }?.let { encrypted ->
        try {
            // 检查是否是加密数据
            if (SecurityUtils.isAESEncryptedData(encrypted)) {
                SecurityUtils.aesDecrypt(encrypted)
            } else {
                encrypted // 如果不是加密数据，直接返回
            }
        } catch (e: Exception) {
            encrypted // 解密失败时返回原始数据
        }
    }
}
```

## 测试示例

项目中包含了完整的测试示例：

- **测试Activity**: `TestSensitiveDataActivity.kt`
- **测试布局**: `test_sensitive_data_view.xml`

运行测试可以验证：
- ✅ 各种数据类型的脱敏效果
- ✅ 可视性切换功能
- ✅ AES加密解密功能
- ✅ 自定义脱敏规则
- ✅ 禁用切换功能

## 注意事项

1. **数据安全**: 组件会自动检测AES加密数据并进行解密，确保数据安全
2. **性能优化**: 避免频繁调用setValue方法，建议在数据确定后再设置
3. **UI线程**: 所有UI操作都在主线程中进行，加密解密操作较轻量
4. **错误处理**: 加密解密失败时会返回原始数据，不会导致应用崩溃
5. **兼容性**: 组件向下兼容，可以处理未加密的历史数据

## 更新日志

- **v1.0.0**: 初始版本，支持基本的脱敏和切换功能
- **v1.1.0**: 添加AES加密解密支持
- **v1.2.0**: 集成到工单系统，支持手机号加密存储

## 技术支持

如有问题或建议，请联系开发团队。