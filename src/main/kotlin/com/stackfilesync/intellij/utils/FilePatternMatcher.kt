package com.stackfilesync.intellij.utils

import java.nio.file.FileSystems
import java.nio.file.Path

object FilePatternMatcher {
    fun matches(pattern: String, path: String): Boolean {
        val matcher = FileSystems.getDefault()
            .getPathMatcher("glob:$pattern")
        return matcher.matches(Path.of(path))
    }

    fun findMatchingFiles(
        sourceDir: Path,
        includePatterns: List<String>,
        excludePatterns: List<String>
    ): List<Path> {
        return java.nio.file.Files.walk(sourceDir)
            .filter { java.nio.file.Files.isRegularFile(it) }
            .filter { file ->
                val relativePath = sourceDir.relativize(file).toString()
                includePatterns.any { pattern -> matches(pattern, relativePath) } &&
                !excludePatterns.any { pattern -> matches(pattern, relativePath) }
            }
            .toList()
    }
} 