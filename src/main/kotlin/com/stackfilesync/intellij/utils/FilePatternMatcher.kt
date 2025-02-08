package com.stackfilesync.intellij.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

object FilePatternMatcher {
    fun findMatchingFiles(
        sourceDir: Path,
        filePatterns: List<String>?,
        excludePatterns: List<String>?
    ): List<Path> {
        // 检查远程目录是否存在
        if (!Files.exists(sourceDir)) {
            return emptyList()
        }

        // 获取所有文件
        val allFiles = Files.walk(sourceDir)
            .filter { Files.isRegularFile(it) }
            .toList()

        // 如果没有指定匹配模式，默认使用 **/*.proto
        val patterns = filePatterns?.takeIf { it.isNotEmpty() } ?: listOf("**/*.proto")
        
        // 过滤文件
        return allFiles.filter { file ->
            val relativePath = sourceDir.relativize(file).toString()
            
            // 检查是否匹配任一模式
            val isMatched = patterns.any { pattern ->
                matchGlobPattern(relativePath, pattern)
            }
            
            // 检查是否被排除
            val isExcluded = excludePatterns?.any { pattern ->
                matchGlobPattern(relativePath, pattern)
            } ?: false
            
            isMatched && !isExcluded
        }
    }

    private fun matchGlobPattern(path: String, pattern: String): Boolean {
        // 将 glob 模式转换为正则表达式
        val regex = pattern
            .replace(".", "\\.")
            .replace("**", "###")
            .replace("*", "[^/]*")
            .replace("###", ".*")
            .let { "^$it$" }
            .toRegex()
        
        return regex.matches(path)
    }
} 