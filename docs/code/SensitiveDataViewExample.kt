package com.jnrl.home.views

import android.content.Context
import android.widget.LinearLayout
import com.jnrl.basecommon.utils.SecurityUtils

/**
 * SensitiveDataView 使用示例
 * 展示如何在代码中使用敏感数据展示组件
 */
class SensitiveDataViewExample {

    /**
     * 在代码中创建和使用 SensitiveDataView
     */
    fun createSensitiveDataView(context: Context, parentLayout: LinearLayout) {
        // 创建手机号脱敏展示组件
        val phoneView = SensitiveDataView(context).apply {
            // 设置数据类型为手机号
            setDataType(SensitiveDataView.DataType.PHONE)
            
            // 设置手机号数据（可能是加密的）
            setValue("13812345678") // 或者是加密后的数据
            
            // 设置默认不显示完整数据
            setDefaultVisible(false)
            
            // 设置切换监听器
            onToggleListener = { isVisible ->
                // 处理显示状态切换事件
                println("手机号显示状态: ${if (isVisible) "显示" else "隐藏"}")
            }
        }
        
        // 添加到父布局
        parentLayout.addView(phoneView)
        
        // 创建身份证脱敏展示组件
        val idCardView = SensitiveDataView(context).apply {
            setDataType(SensitiveDataView.DataType.ID_CARD)
            setValue("110101199001011234")
            setDefaultVisible(false)
        }
        
        parentLayout.addView(idCardView)
        
        // 创建邮箱脱敏展示组件
        val emailView = SensitiveDataView(context).apply {
            setDataType(SensitiveDataView.DataType.EMAIL)
            setValue("example@email.com")
            setDefaultVisible(false)
        }
        
        parentLayout.addView(emailView)
        
        // 创建自定义脱敏规则的组件
        val customView = SensitiveDataView(context).apply {
            setDataType(SensitiveDataView.DataType.CUSTOM)
            setCustomMaskRule(SensitiveDataView.MaskRule(
                start = 2,
                end = 3,
                maskChar = '#'
            ))
            setValue("ABCDEFGHIJK")
            setDefaultVisible(false)
        }
        
        parentLayout.addView(customView)
    }
    
    /**
     * 处理加密数据的示例
     */
    fun handleEncryptedData(context: Context): SensitiveDataView {
        val phoneNumber = "13812345678"
        
        // 加密手机号
        val encryptedPhone = try {
            SecurityUtils.aesEncrypt(phoneNumber)
        } catch (e: Exception) {
            phoneNumber // 加密失败时使用原始数据
        }
        
        // 创建组件并设置加密数据
        return SensitiveDataView(context).apply {
            setDataType(SensitiveDataView.DataType.PHONE)
            setValue(encryptedPhone) // 组件会自动检测并解密
            setDefaultVisible(false)
            
            onToggleListener = { isVisible ->
                // 记录用户查看敏感信息的行为
                logSensitiveDataAccess("phone", isVisible)
            }
        }
    }
    
    /**
     * 记录敏感数据访问日志
     */
    private fun logSensitiveDataAccess(dataType: String, isVisible: Boolean) {
        // 这里可以添加日志记录逻辑
        println("敏感数据访问: 类型=$dataType, 状态=${if (isVisible) "查看" else "隐藏"}")
    }
}