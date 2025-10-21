package com.stackfilesync.intellij.model

import java.awt.Color

/**
 * 仓库同步状态枚举
 */
enum class RepositoryStatus(val displayName: String, val color: Color, val description: String) {
    /**
     * 可以同步 - 绿色指示器
     */
    READY("就绪", Color(76, 175, 80), "仓库可以同步"),

    /**
     * 同步中 - 蓝色指示器
     */
    SYNCING("同步中", Color(33, 150, 243), "正在执行同步操作"),

    /**
     * 不可用 - 红色指示器
     */
    UNAVAILABLE("不可用", Color(244, 67, 54), "仓库无法连接或配置有误"),

    /**
     * 未知状态 - 灰色指示器
     */
    UNKNOWN("未知", Color(158, 158, 158), "状态检查中");

    companion object {
        fun fromString(value: String): RepositoryStatus {
            return values().find { it.name == value } ?: UNKNOWN
        }
    }
}

/**
 * 仓库状态信息（包含详细错误信息）
 */
data class RepositoryStatusInfo(
    val status: RepositoryStatus,
    val detailMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 获取完整的描述信息
     */
    fun getFullDescription(): String {
        return if (detailMessage.isNullOrBlank()) {
            status.description
        } else {
            "${status.description}\n详细信息: $detailMessage"
        }
    }
}
