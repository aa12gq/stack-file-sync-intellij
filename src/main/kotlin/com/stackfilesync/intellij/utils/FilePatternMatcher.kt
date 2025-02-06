package com.stackfilesync.intellij.utils

import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.FileVisitResult
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.regex.Pattern

class FilePatternMatcher {
    companion object {
        fun findMatchingFiles(
            sourceDir: Path,
            includePatterns: List<String>,
            excludePatterns: List<String>
        ): List<Path> {
            val matchingFiles = mutableListOf<Path>()
            
            // 将 glob 模式转换为正则表达式
            val includeRegexes = includePatterns.map { globToRegex(it) }
            val excludeRegexes = excludePatterns.map { globToRegex(it) }
            
            Files.walkFileTree(sourceDir, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val relativePath = sourceDir.relativize(file).toString()
                    
                    // 检查是否匹配包含模式
                    val isIncluded = includeRegexes.any { regex ->
                        regex.matches(relativePath)
                    }
                    
                    // 检查是否匹配排除模式
                    val isExcluded = excludeRegexes.any { regex ->
                        regex.matches(relativePath)
                    }
                    
                    if (isIncluded && !isExcluded) {
                        matchingFiles.add(file)
                    }
                    
                    return FileVisitResult.CONTINUE
                }
            })
            
            return matchingFiles
        }
        
        private fun globToRegex(glob: String): Regex {
            val regex = glob
                .replace(".", "\\.")
                .replace("**", "§§") // 临时替换 ** 为特殊标记
                .replace("*", "[^/]*")
                .replace("§§", ".*")
                .replace("?", ".")
            
            return Regex("^${regex}$")
        }
    }
} 