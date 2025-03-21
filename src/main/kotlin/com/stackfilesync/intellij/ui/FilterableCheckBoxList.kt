package com.stackfilesync.intellij.ui

import com.intellij.ui.CheckBoxList
import javax.swing.JPanel
import java.awt.Dimension
import javax.swing.ListCellRenderer

class FilterableCheckBoxList<T> : CheckBoxList<T>() {
    private val visibilityMap = mutableMapOf<Int, Boolean>()
    
    // 不要使用初始化器中的setCellRenderer，改为在需要时重写getCellRenderer
    
    fun setItemVisible(index: Int, visible: Boolean) {
        visibilityMap[index] = visible
        repaint()
    }
    
    fun isItemVisible(index: Int): Boolean {
        return visibilityMap.getOrDefault(index, true)
    }
    
    // 重写获取可见项数量的方法
    fun getVisibleItemsCount(): Int {
        return (0 until itemsCount).count { isItemVisible(it) }
    }
} 