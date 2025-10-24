package com.jnrl.home.views

import androidx.databinding.BindingAdapter

/**
 * SensitiveDataView 数据绑定适配器
 * 支持在XML中使用 android:text="@{data.field}" 的方式设置数据
 */
object SensitiveDataViewBindingAdapter {

    /**
     * 为SensitiveDataView添加text属性的数据绑定支持
     * 使用方式：android:text="@{data.phoneNumber}"
     */
    @JvmStatic
    @BindingAdapter("android:text")
    fun setText(view: SensitiveDataView, text: String?) {
        view.setValue(text)
    }

    /**
     * 为SensitiveDataView添加text属性的数据绑定支持（重载方法）
     * 处理可能为null的情况
     */
    @JvmStatic
    @BindingAdapter("text")
    fun setTextValue(view: SensitiveDataView, text: String?) {
        view.setValue(text)
    }

    /**
     * 设置数据类型
     * 使用方式：app:dataType="@{DataType.PHONE}"
     */
    @JvmStatic
    @BindingAdapter("dataType")
    fun setDataType(view: SensitiveDataView, dataType: SensitiveDataView.DataType?) {
        dataType?.let {
            view.setDataType(it)
        }
    }

    /**
     * 设置默认可见性
     * 使用方式：app:defaultVisible="@{true}"
     */
    @JvmStatic
    @BindingAdapter("defaultVisible")
    fun setDefaultVisible(view: SensitiveDataView, visible: Boolean) {
        view.setDefaultVisible(visible)
    }

    /**
     * 设置是否禁用
     * 使用方式：app:disabled="@{false}"
     */
    @JvmStatic
    @BindingAdapter("disabled")
    fun setDisabled(view: SensitiveDataView, disabled: Boolean) {
        view.setDisabled(disabled)
    }
}