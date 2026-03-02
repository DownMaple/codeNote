# SensitiveDataView - Android 敏感数据布局组件

本篇笔记主要介绍如何在 Android 项目中使用自定义的 `SensitiveDataView` 组件来安全地展示敏感数据，支持数据脱敏、可视性切换和AES加密解密功能。

祖传开篇：作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

环境要求：Android 项目，支持 Kotlin 和 Java。

## 组件概述

`SensitiveDataView` 是一个专门用于展示敏感数据的自定义 Android 布局组件，它继承自 `LinearLayout`，主要解决了以下问题：

- 🔒 **数据安全**：自动检测并处理AES加密的敏感数据
- 👁️ **用户体验**：提供直观的显示/隐藏切换功能
- 🎨 **灵活配置**：支持多种数据类型和自定义脱敏规则
- 🚫 **权限控制**：可禁用切换功能，强制脱敏显示

## 功能特性

### 支持的数据类型

| 数据类型 | 枚举值 | 脱敏规则 | 示例效果 |
|---------|--------|----------|----------|
| 手机号 | `PHONE` | 保留前3位和后4位 | `138****5678` |
| 身份证号 | `ID_CARD` | 保留前6位和后4位 | `110101********1234` |
| 邮箱地址 | `EMAIL` | 保留@前2位和@后全部 | `ex****@email.com` |
| 自定义 | `CUSTOM` | 根据maskStart和maskEnd设置 | 可自定义规则 |

### 核心功能

- ✅ **自动脱敏**：根据数据类型自动应用脱敏规则
- ✅ **AES解密**：自动检测并解密AES加密的数据
- ✅ **可视性切换**：点击眼睛图标切换显示状态
- ✅ **自定义样式**：支持自定义脱敏字符、位置等
- ✅ **数据绑定**：支持 Android DataBinding
- ✅ **状态监听**：提供切换状态回调

## XML 属性配置

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

### 1. XML 布局中使用

#### 基础用法

```xml
<!-- 手机号脱敏展示 -->
<com.jnrl.home.views.SensitiveDataView
    android:id="@+id/sensitive_phone"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textSize="16sp"
    android:textColor="#333333"
    app:dataType="phone"
    app:defaultVisible="false" />

<!-- 身份证脱敏展示 -->
<com.jnrl.home.views.SensitiveDataView
    android:id="@+id/sensitive_id_card"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:dataType="idCard"
    app:defaultVisible="false" />

<!-- 邮箱脱敏展示 -->
<com.jnrl.home.views.SensitiveDataView
    android:id="@+id/sensitive_email"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:dataType="email"
    app:defaultVisible="false" />
```

#### 自定义脱敏规则

```xml
<!-- 自定义脱敏规则：保留前2位和后3位，使用#作为脱敏字符 -->
<com.jnrl.home.views.SensitiveDataView
    android:id="@+id/sensitive_custom"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:dataType="custom"
    app:defaultVisible="false"
    app:maskStart="2"
    app:maskEnd="3"
    app:maskChar="#" />
```

#### 禁用切换功能

```xml
<!-- 禁用切换功能，仅显示脱敏数据 -->
<com.jnrl.home.views.SensitiveDataView
    android:id="@+id/sensitive_disabled"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:dataType="phone"
    app:defaultVisible="true"
    app:disabled="true" />
```

### 2. Kotlin 代码中使用

#### 基础设置

```kotlin
// 获取组件实例
val sensitiveDataView = findViewById<SensitiveDataView>(R.id.sensitive_phone)

// 设置数据（支持加密和未加密数据）
sensitiveDataView.setValue("13812345678")

// 设置加密数据（组件会自动检测并解密）
val encryptedPhone = SecurityUtils.aesEncrypt("13812345678")
sensitiveDataView.setValue(encryptedPhone)

// 设置切换监听器
sensitiveDataView.onToggleListener = { isVisible ->
    Log.d("SensitiveData", "数据显示状态: ${if (isVisible) "显示" else "隐藏"}")
    // 可以在这里记录用户查看敏感信息的行为
}
```

#### 程序控制显示状态

```kotlin
// 程序控制显示/隐藏
sensitiveDataView.setVisible(true)  // 显示原始数据
sensitiveDataView.setVisible(false) // 显示脱敏数据

// 切换显示状态
sensitiveDataView.toggleVisibility()

// 检查当前状态
val isVisible = sensitiveDataView.isVisible()
```

#### 动态配置

```kotlin
// 设置数据类型
sensitiveDataView.setDataType(SensitiveDataView.DataType.PHONE)

// 设置自定义脱敏规则
sensitiveDataView.setCustomMaskRule(
    SensitiveDataView.MaskRule(
        start = 2,
        end = 3,
        maskChar = '#'
    )
)

// 设置默认可见性
sensitiveDataView.setDefaultVisible(false)

// 禁用/启用切换功能
sensitiveDataView.setDisabled(true)
```

### 3. 动态创建组件

```kotlin
fun createSensitiveDataView(context: Context, parentLayout: LinearLayout) {
    // 创建手机号脱敏展示组件
    val phoneView = SensitiveDataView(context).apply {
        setDataType(SensitiveDataView.DataType.PHONE)
        setValue("13812345678")
        setDefaultVisible(false)
        
        onToggleListener = { isVisible ->
            // 处理显示状态切换事件
            handleSensitiveDataToggle("phone", isVisible)
        }
    }
    
    // 设置布局参数
    val layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    )
    phoneView.layoutParams = layoutParams
    
    // 添加到父布局
    parentLayout.addView(phoneView)
}
```

### 4. DataBinding 支持

#### 在 XML 中使用 DataBinding

```xml
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <data>
        <variable
            name="userInfo"
            type="com.example.UserInfo" />
    </data>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <!-- 使用 DataBinding 设置数据 -->
        <com.jnrl.home.views.SensitiveDataView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@{userInfo.phoneNumber}"
            app:dataType="phone"
            app:defaultVisible="false" />

        <com.jnrl.home.views.SensitiveDataView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@{userInfo.idCard}"
            app:dataType="idCard"
            app:defaultVisible="false" />

    </LinearLayout>
</layout>
```

## 实际应用场景

### 1. 用户信息展示页面

```kotlin
class UserProfileActivity : AppCompatActivity() {
    
    private lateinit var phoneView: SensitiveDataView
    private lateinit var idCardView: SensitiveDataView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        
        initViews()
        loadUserData()
    }
    
    private fun initViews() {
        phoneView = findViewById(R.id.sensitive_phone)
        idCardView = findViewById(R.id.sensitive_id_card)
        
        // 设置监听器，记录敏感信息查看行为
        phoneView.onToggleListener = { isVisible ->
            logSensitiveDataAccess("phone", isVisible)
        }
        
        idCardView.onToggleListener = { isVisible ->
            logSensitiveDataAccess("idCard", isVisible)
        }
    }
    
    private fun loadUserData() {
        // 从服务器获取用户数据（可能是加密的）
        val userInfo = getUserInfoFromServer()
        
        // 设置数据（组件会自动处理加密数据）
        phoneView.setValue(userInfo.encryptedPhone)
        idCardView.setValue(userInfo.encryptedIdCard)
    }
    
    private fun logSensitiveDataAccess(dataType: String, isVisible: Boolean) {
        // 记录用户查看敏感信息的行为
        val action = if (isVisible) "view" else "hide"
        Log.i("SensitiveData", "User $action $dataType")
        
        // 可以上报到数据分析平台
        Analytics.track("sensitive_data_access", mapOf(
            "data_type" to dataType,
            "action" to action,
            "timestamp" to System.currentTimeMillis()
        ))
    }
}
```

### 2. 工单系统集成

```kotlin
class CreateOrEditWorkOrderViewModel : BaseViewModel() {
    
    /**
     * 创建或更新工单时加密敏感数据
     */
    fun createOrUpdateWorkOrder(workOrderBean: CreateOrEditWorkOrderBean) {
        val submitBean = workOrderBean.copy(
            contactMobile = encryptContactMobile(workOrderBean.contactMobile)
        )
        
        // 提交到服务器
        submitWorkOrder(submitBean)
    }
    
    /**
     * 获取工单详情时解密敏感数据
     */
    fun getWorkOrderDetail(orderId: String) {
        workOrderService.getDetail(orderId) { response ->
            val workOrderBean = response.data.copy(
                contactMobile = decryptContactMobile(response.data.contactMobile)
            )
            
            // 更新UI
            updateWorkOrderData(workOrderBean)
        }
    }
    
    /**
     * 加密联系人手机号
     */
    private fun encryptContactMobile(contactMobile: String?): String? {
        return contactMobile?.takeIf { it.isNotBlank() }?.let { mobile ->
            try {
                if (SecurityUtils.isAESEncryptedData(mobile)) {
                    mobile // 已经是加密数据
                } else {
                    SecurityUtils.aesEncrypt(mobile)
                }
            } catch (e: Exception) {
                Log.e("Encrypt", "加密失败", e)
                mobile // 加密失败时返回原始数据
            }
        }
    }
    
    /**
     * 解密联系人手机号
     */
    private fun decryptContactMobile(encryptedMobile: String?): String? {
        return encryptedMobile?.takeIf { it.isNotBlank() }?.let { encrypted ->
            try {
                if (SecurityUtils.isAESEncryptedData(encrypted)) {
                    SecurityUtils.aesDecrypt(encrypted)
                } else {
                    encrypted // 不是加密数据，直接返回
                }
            } catch (e: Exception) {
                Log.e("Decrypt", "解密失败", e)
                encrypted // 解密失败时返回原始数据
            }
        }
    }
}
```

## 组件架构设计

### 核心类结构

```kotlin
class SensitiveDataView : LinearLayout {
    
    // 数据类型枚举
    enum class DataType {
        PHONE,      // 手机号
        ID_CARD,    // 身份证
        EMAIL,      // 邮箱
        CUSTOM      // 自定义
    }
    
    // 自定义脱敏规则
    data class MaskRule(
        val start: Int = 3,     // 保留开头字符数
        val end: Int = 4,       // 保留结尾字符数
        val maskChar: Char = '*' // 脱敏字符
    )
    
    // 核心属性
    private var originalValue: String? = null
    private var isVisible: Boolean = false
    private var dataType: DataType = DataType.PHONE
    private var isDisabled: Boolean = false
    private var customMaskRule: MaskRule = MaskRule()
    
    // 切换监听器
    var onToggleListener: ((isVisible: Boolean) -> Unit)? = null
}
```

### 脱敏算法实现

```kotlin
/**
 * 手机号脱敏：保留前3位和后4位
 * 示例：13812345678 -> 138****5678
 */
private fun maskPhone(phone: String): String {
    return when {
        phone.length <= 7 -> phone.replace(Regex("."), "*")
        else -> {
            val start = phone.substring(0, 3)
            val end = phone.substring(phone.length - 4)
            val middle = "*".repeat(phone.length - 7)
            "$start$middle$end"
        }
    }
}

/**
 * 身份证脱敏：保留前6位和后4位
 * 示例：110101199001011234 -> 110101********1234
 */
private fun maskIdCard(idCard: String): String {
    return when {
        idCard.length <= 10 -> idCard.replace(Regex("."), "*")
        else -> {
            val start = idCard.substring(0, 6)
            val end = idCard.substring(idCard.length - 4)
            val middle = "*".repeat(idCard.length - 10)
            "$start$middle$end"
        }
    }
}

/**
 * 邮箱脱敏：保留@前2位和@后全部
 * 示例：example@email.com -> ex****@email.com
 */
private fun maskEmail(email: String): String {
    val atIndex = email.indexOf("@")
    return if (atIndex > 2) {
        val start = email.substring(0, 2)
        val end = email.substring(atIndex)
        val middle = "*".repeat(atIndex - 2)
        "$start$middle$end"
    } else {
        email.replace(Regex("[^@.]"), "*")
    }
}
```

## 安全考虑

### 1. 数据加密存储

```kotlin
// 存储时加密
val encryptedData = SecurityUtils.aesEncrypt(sensitiveData)
preferences.edit().putString("user_phone", encryptedData).apply()

// 读取时解密
val encryptedData = preferences.getString("user_phone", "")
val decryptedData = SecurityUtils.aesDecrypt(encryptedData)
sensitiveDataView.setValue(decryptedData)
```

### 2. 内存安全

```kotlin
// 在组件销毁时清理敏感数据
override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    originalValue = null
    onToggleListener = null
}
```

### 3. 日志安全

```kotlin
// 避免在日志中输出敏感信息
private fun logSafely(message: String, sensitiveData: String?) {
    val safeData = sensitiveData?.let { 
        if (it.length > 4) "${it.take(2)}****${it.takeLast(2)}" else "****"
    } ?: "null"
    Log.d(TAG, "$message: $safeData")
}
```

## 性能优化

### 1. 避免频繁重绘

```kotlin
private fun updateDisplayText() {
    val newText = getDisplayText()
    if (dataText.text.toString() != newText) {
        dataText.text = newText
    }
}
```

### 2. 缓存脱敏结果

```kotlin
private var cachedMaskedText: String? = null

private fun getMaskedText(): String {
    if (cachedMaskedText == null) {
        cachedMaskedText = when (dataType) {
            DataType.PHONE -> maskPhone(originalValue ?: "")
            DataType.ID_CARD -> maskIdCard(originalValue ?: "")
            DataType.EMAIL -> maskEmail(originalValue ?: "")
            DataType.CUSTOM -> maskCustom(originalValue ?: "", customMaskRule)
        }
    }
    return cachedMaskedText ?: ""
}
```

## 注意事项

1. **数据安全**：
   - 组件会自动检测AES加密数据并进行解密
   - 建议在存储敏感数据时使用加密
   - 避免在日志中输出敏感信息

2. **性能考虑**：
   - 避免频繁调用 `setValue` 方法
   - 脱敏计算会缓存结果，提高性能
   - 在组件销毁时会自动清理内存

3. **UI线程安全**：
   - 所有UI操作都在主线程中进行
   - 加密解密操作较轻量，不会阻塞UI

4. **兼容性**：
   - 组件向下兼容，可以处理未加密的历史数据
   - 加密解密失败时会返回原始数据，不会导致应用崩溃

5. **用户体验**：
   - 提供直观的眼睛图标表示显示/隐藏状态
   - 支持禁用切换功能，适用于只读场景
   - 支持自定义脱敏规则，满足不同业务需求

## 更新日志

- **v1.0.0**: 初始版本，支持基本的脱敏和切换功能
- **v1.1.0**: 添加AES加密解密支持，提升数据安全性
- **v1.2.0**: 集成到工单系统，支持手机号加密存储
- **v1.3.0**: 添加DataBinding支持，优化性能和内存管理

## 技术支持

如有问题或建议，请联系开发团队或提交 Issue。

## 总结

SensitiveDataView 是一个功能完善的敏感数据展示组件，具有以下特点：

- **安全性**：支持数据加密存储和脱敏展示
- **灵活性**：支持多种数据类型和自定义脱敏规则
- **易用性**：简单的 API 设计，支持 XML 属性配置
- **可扩展性**：支持自定义样式和行为

该组件适用于需要展示敏感信息的各种场景，如用户信息页面、订单详情、账户设置等。

## 完整源码

以下是 SensitiveDataView 组件的完整源码实现：

```kotlin
package com.jnrl.home.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.jnrl.basecommon.utils.SecurityUtils
import com.jnrl.home.R

/**
 * 敏感数据展示组件
 * 支持数据脱敏展示和显示/隐藏切换功能
 */
@SuppressLint("ResourceType")
class SensitiveDataView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var dataText: TextView
    private lateinit var toggleIcon: ImageView
    
    private var originalValue: String? = null
    private var isVisible: Boolean = false
    private var dataType: DataType = DataType.PHONE
    private var isDisabled: Boolean = false
    
    // 数据类型枚举
    enum class DataType {
        PHONE,      // 手机号
        ID_CARD,    // 身份证
        EMAIL,      // 邮箱
        CUSTOM      // 自定义
    }
    
    // 自定义脱敏规则
    data class MaskRule(
        val start: Int = 3,
        val end: Int = 4,
        val maskChar: Char = '*'
    )
    
    private var customMaskRule: MaskRule = MaskRule()
    
    // 切换监听器
    var onToggleListener: ((isVisible: Boolean) -> Unit)? = null

    init {
        // 设置布局方向
        orientation = HORIZONTAL
        
        // 加载布局
        try {
            val view = LayoutInflater.from(context).inflate(R.layout.view_sensitive_data, this, true)
            dataText = view.findViewById(R.id.tv_data_text)
            toggleIcon = view.findViewById(R.id.iv_toggle_icon)
        } catch (e: Exception) {
            // 如果布局加载失败，创建默认视图
            val textView = TextView(context).apply {
                id = R.id.tv_data_text
                text = "暂无数据"
                textSize = 14f
            }
            val imageView = ImageView(context).apply {
                id = R.id.iv_toggle_icon
                layoutParams = LinearLayout.LayoutParams(48, 48)
            }
            addView(textView)
            addView(imageView)
            
            dataText = textView
            toggleIcon = imageView
        }
        
        // 设置点击监听
        toggleIcon.setOnClickListener {
            if (!isDisabled) {
                toggleVisibility()
            }
        }
        
        // 处理自定义属性
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.SensitiveDataView)
            
            try {
                val typeValue = typedArray.getInt(R.styleable.SensitiveDataView_dataType, 0)
                dataType = when (typeValue) {
                    0 -> DataType.PHONE
                    1 -> DataType.ID_CARD
                    2 -> DataType.EMAIL
                    3 -> DataType.CUSTOM
                    else -> DataType.PHONE
                }
                
                isVisible = typedArray.getBoolean(R.styleable.SensitiveDataView_defaultVisible, false)
                isDisabled = typedArray.getBoolean(R.styleable.SensitiveDataView_disabled, false)
                
                val maskStart = typedArray.getInt(R.styleable.SensitiveDataView_maskStart, 3)
                val maskEnd = typedArray.getInt(R.styleable.SensitiveDataView_maskEnd, 4)
                val maskChar = typedArray.getString(R.styleable.SensitiveDataView_maskChar)?.firstOrNull() ?: '*'
                
                customMaskRule = MaskRule(maskStart, maskEnd, maskChar)
            } catch (e: Exception) {
                // 使用默认值
                dataType = DataType.PHONE
                isVisible = false
                isDisabled = false
                customMaskRule = MaskRule()
            } finally {
                typedArray.recycle()
            }
            
            // 处理 TextView 相关属性
            val textAttrs = context.obtainStyledAttributes(attrs, intArrayOf(
                android.R.attr.textSize,
                android.R.attr.textColor,
                android.R.attr.textColorHint,
                android.R.attr.hint,
                android.R.attr.maxLines,
                android.R.attr.ellipsize
            ))
            
            try {
                // 应用文本大小
                if (textAttrs.hasValue(0)) {
                    val textSize = textAttrs.getDimensionPixelSize(0, 0)
                    if (textSize > 0) {
                        dataText.textSize = textSize / resources.displayMetrics.scaledDensity
                    }
                }
                
                // 应用文本颜色
                if (textAttrs.hasValue(1)) {
                    val textColor = textAttrs.getColor(1, 0)
                    if (textColor != 0) {
                        dataText.setTextColor(textColor)
                    }
                }
                
                // 应用提示文本颜色
                if (textAttrs.hasValue(2)) {
                    val hintColor = textAttrs.getColor(2, 0)
                    if (hintColor != 0) {
                        dataText.setHintTextColor(hintColor)
                    }
                }
                
                // 应用提示文本
                if (textAttrs.hasValue(3)) {
                    val hint = textAttrs.getString(3)
                    if (!hint.isNullOrEmpty()) {
                        dataText.hint = hint
                    }
                }
                
                // 应用最大行数
                if (textAttrs.hasValue(4)) {
                    val maxLines = textAttrs.getInt(4, 1)
                    if (maxLines > 0) {
                        dataText.maxLines = maxLines
                    }
                }
                
                // 应用省略号
                if (textAttrs.hasValue(5)) {
                    val ellipsize = textAttrs.getInt(5, 0)
                    when (ellipsize) {
                        1 -> dataText.ellipsize = android.text.TextUtils.TruncateAt.START
                        2 -> dataText.ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                        3 -> dataText.ellipsize = android.text.TextUtils.TruncateAt.END
                        4 -> dataText.ellipsize = android.text.TextUtils.TruncateAt.MARQUEE
                    }
                }
            } catch (e: Exception) {
                // 忽略属性设置错误
            } finally {
                textAttrs.recycle()
            }
        }
        
        updateUI()
    }
    
    /**
     * 设置数据值并重置显示状态
     */
    fun setValue(value: String?) {
        originalValue = value
        // 每次设置新值时重置为隐藏状态，确保一致性
        isVisible = false
        updateUI()
    }
    
    /**
     * 重置组件状态 - 用于列表视图回收时
     */
    fun resetState() {
        originalValue = null
        isVisible = false
        // 确保UI立即更新
        updateUI()
    }
    
    /**
     * 获取原始数据值
     */
    fun getValue(): String? = originalValue
    
    /**
     * 设置数据类型
     */
    fun setDataType(type: DataType) {
        dataType = type
        updateDisplayText()
    }
    
    /**
     * 设置自定义脱敏规则
     */
    fun setCustomMaskRule(rule: MaskRule) {
        customMaskRule = rule
        if (dataType == DataType.CUSTOM) {
            updateDisplayText()
        }
    }
    
    /**
     * 设置默认显示状态
     */
    fun setDefaultVisible(visible: Boolean) {
        isVisible = visible
        updateUI()
    }
    
    /**
     * 设置是否禁用切换功能
     */
    fun setDisabled(disabled: Boolean) {
        isDisabled = disabled
        updateUI()
    }
    
    /**
     * 切换显示状态
     */
    fun toggleVisibility() {
        if (isDisabled) return
        
        isVisible = !isVisible
        updateUI()
        onToggleListener?.invoke(isVisible)
    }
    
    /**
     * 设置显示状态
     */
    fun setVisible(visible: Boolean) {
        isVisible = visible
        updateUI()
    }
    
    /**
     * 获取当前显示状态
     */
    fun isCurrentlyVisible(): Boolean = isVisible
    
    /**
     * 更新UI显示
     */
    private fun updateUI() {
        updateDisplayText()
        updateToggleIcon()
    }
    
    /**
     * 更新显示文本
     */
    private fun updateDisplayText() {
        try {
            val displayText = getDisplayText()
            dataText.text = displayText
            
            // 根据是否可见或禁用状态设置透明度
            dataText.alpha = if (isVisible || isDisabled) 1.0f else 0.7f
        } catch (e: Exception) {
            // 异常时显示默认文本
            dataText.text = "暂无数据"
            dataText.alpha = 0.7f
        }
    }
    
    /**
     * 更新切换图标
     */
    private fun updateToggleIcon() {
        try {
            if (originalValue.isNullOrBlank() || originalValue == "暂无数据") {
                toggleIcon.visibility = GONE
                return
            }
            
            toggleIcon.visibility = if (isDisabled) GONE else VISIBLE
            
            // 设置图标资源
            val iconRes = if (isVisible) {
                R.drawable.icon_eye_close // 显示时用"隐藏"图标
            } else {
                R.drawable.icon_eye_open // 隐藏时用"显示"图标
            }
            
            toggleIcon.setImageResource(iconRes)
            
            // 设置图标透明度
            toggleIcon.alpha = if (isDisabled) 0.3f else 1.0f
        } catch (e: Exception) {
            // 图标更新失败时隐藏图标
            toggleIcon.visibility = GONE
        }
    }
    
    /**
     * 获取显示文本（脱敏或原始）
     */
    private fun getDisplayText(): String {
        if (originalValue.isNullOrEmpty()) {
            return "暂无数据"
        }
        
        return if (isVisible) {
            // 显示原始数据，需要解密
            try {
                if (SecurityUtils.isAESEncryptedData(originalValue!!)) {
                    SecurityUtils.aesDecrypt(originalValue!!)
                } else {
                    originalValue!!
                }
            } catch (e: Exception) {
                // 解密失败时返回原始值
                originalValue!!
            }
        } else {
            // 显示脱敏数据
            try {
                val decryptedText = if (SecurityUtils.isAESEncryptedData(originalValue!!)) {
                    SecurityUtils.aesDecrypt(originalValue!!)
                } else {
                    originalValue!!
                }
                
                when (dataType) {
                    DataType.PHONE -> maskPhone(decryptedText)
                    DataType.ID_CARD -> maskIdCard(decryptedText)
                    DataType.EMAIL -> maskEmail(decryptedText)
                    DataType.CUSTOM -> maskCustom(decryptedText, customMaskRule)
                }
            } catch (e: Exception) {
                // 处理失败时返回默认脱敏
                when (dataType) {
                    DataType.PHONE -> maskPhone(originalValue!!)
                    DataType.ID_CARD -> maskIdCard(originalValue!!)
                    DataType.EMAIL -> maskEmail(originalValue!!)
                    DataType.CUSTOM -> maskCustom(originalValue!!, customMaskRule)
                }
            }
        }
    }
    
    /**
     * 手机号脱敏：中间4位为****
     */
    private fun maskPhone(phone: String): String {
        if (phone.length < 7) return phone
        
        // 移除非数字字符
        val cleanPhone = phone.replace(Regex("\\D"), "")
        
        return when {
            cleanPhone.length == 11 -> {
                // 普通手机号：138****5678
                cleanPhone.replaceRange(3, 7, "****")
            }
            cleanPhone.length > 7 -> {
                // 其他格式：保留前3位和后4位
                val start = cleanPhone.substring(0, 3)
                val end = cleanPhone.substring(cleanPhone.length - 4)
                "$start****$end"
            }
            else -> phone
        }
    }
    
    /**
     * 身份证脱敏：中间10位为**********
     */
    private fun maskIdCard(idCard: String): String {
        if (idCard.length < 15) return idCard
        
        val cleanIdCard = idCard.uppercase().trim()
        
        return when (cleanIdCard.length) {
            15 -> {
                // 15位身份证：前4位 + 7个* + 后4位
                cleanIdCard.replaceRange(4, 11, "*******")
            }
            18 -> {
                // 18位身份证：前4位 + 10个* + 后4位
                cleanIdCard.replaceRange(4, 14, "**********")
            }
            else -> idCard
        }
    }
    
    /**
     * 邮箱脱敏：@符号前的所有字符为****
     */
    private fun maskEmail(email: String): String {
        if (!email.contains("@")) return email
        
        val atIndex = email.indexOf("@")
        if (atIndex <= 0) return email
        
        // 保留@符号及其后面的域名部分
        val domainPart = email.substring(atIndex)
        return "****$domainPart"
    }
    
    /**
     * 自定义脱敏
     */
    private fun maskCustom(text: String, rule: MaskRule): String {
        val textLength = text.length
        
        if (textLength <= rule.start + rule.end) return text
        
        val startPart = text.substring(0, rule.start)
        val endPart = text.substring(textLength - rule.end)
        val maskLength = textLength - rule.start - rule.end
        val maskPart = rule.maskChar.toString().repeat(maskLength)
        
        return "$startPart$maskPart$endPart"
    }
}
```

### 源码说明

这个完整的源码实现包含了以下核心功能：

1. **数据类型支持**：手机号、身份证、邮箱和自定义类型的脱敏处理
2. **安全加密**：集成 SecurityUtils 进行数据加密解密
3. **UI 交互**：点击切换显示/隐藏状态，支持禁用功能
4. **自定义属性**：支持 XML 属性配置和运行时设置
5. **异常处理**：完善的异常处理机制，确保组件稳定性
6. **状态管理**：适用于列表视图的状态重置功能

通过这个源码，开发者可以深入了解组件的实现细节，并根据具体需求进行定制化修改。

---

*本文档基于 SensitiveDataView v1.3.0 编写，如有更新请以最新版本为准。*