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