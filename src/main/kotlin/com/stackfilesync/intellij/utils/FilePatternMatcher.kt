package com.stackfilesync.intellij.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList
import java.nio.file.FileSystems

object FilePatternMatcher {
    fun findMatchingFiles(baseDir: Path, includePatterns: List<String>, excludePatterns: List<String>): List<Path> {
        // 检查是否使用了递归查找
        val useRecursive = includePatterns.any { pattern -> pattern.contains("**") }
        
        // 增加调试日志
        // println("使用递归查找: $useRecursive")
        
        val matcher = FileSystems.getDefault().getPathMatcher("glob:**/*")
        val result = mutableListOf<Path>()
        
        // 无论是否在pattern中指定了递归，都使用递归查找来保证子目录的文件能被找到
        Files.walk(baseDir).use { stream ->
            stream.filter { Files.isRegularFile(it) }
                .filter { path ->
                    // 如果includePatterns为空，则匹配所有文件
                    val relativePath = baseDir.relativize(path)
                    val matched = if (includePatterns.isEmpty()) {
                        true
                    } else {
                        includePatterns.any { pattern ->
                            val pathMatcher = if (pattern == "*") {
                                // 如果模式就是 "*"，直接匹配所有文件
                                true
                            } else {
                                val globPattern = if (pattern.contains("**")) {
                                    // 已经包含递归匹配
                                    "glob:$pattern"
                                } else {
                                    // 添加递归匹配
                                    "glob:**/$pattern"
                                }
                                FileSystems.getDefault().getPathMatcher(globPattern).matches(relativePath)
                            }
                            pathMatcher
                        }
                    }
                    
                    // 如果匹配包含模式，检查是否被排除
                    if (matched && excludePatterns.isNotEmpty()) {
                        !excludePatterns.any { pattern ->
                            val globPattern = if (pattern.contains("**")) {
                                "glob:$pattern"
                            } else {
                                "glob:**/$pattern"
                            }
                            FileSystems.getDefault().getPathMatcher(globPattern).matches(relativePath)
                        }
                    } else {
                        matched
                    }
                }
                .forEach { result.add(it) }
        }
        
        return result
    }

    fun matchGlobPattern(path: String, pattern: String): Boolean {
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