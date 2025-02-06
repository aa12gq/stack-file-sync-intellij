package com.stackfilesync.intellij.backup

import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.stackfilesync.intellij.exception.SyncException
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.util.FilePatternMatcher
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.io.File
import java.time.ZoneId
import java.security.MessageDigest

class BackupManager(private val project: Project) {
    private val backupRoot = getBackupDirectory()
    
    fun backupFiles(repository: Repository, files: List<Path>) {
        // 检查是否启用备份
        if (repository.backupConfig?.enabled != true) {
            return
        }
        
        try {
            // 创建备份目录
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val backupDir = backupRoot.resolve("${repository.name}_$timestamp")
            Files.createDirectories(backupDir)
            
            // 过滤排除的文件
            val excludePatterns = repository.backupConfig?.excludePatterns ?: emptyList()
            val filesToBackup = if (excludePatterns.isEmpty()) {
                files
            } else {
                files.filter { file ->
                    val relativePath = getTargetDirectory(repository).relativize(file).toString()
                    !excludePatterns.any { pattern ->
                        FilePatternMatcher.matches(pattern, relativePath)
                    }
                }
            }
            
            // 备份文件
            filesToBackup.forEach { file ->
                if (Files.exists(file)) {
                    val relativePath = getTargetDirectory(repository).relativize(file)
                    val backupFile = backupDir.resolve(relativePath)
                    
                    // 创建父目录
                    Files.createDirectories(backupFile.parent)
                    
                    // 复制文件
                    Files.copy(file, backupFile, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        } catch (e: Exception) {
            throw SyncException.FileException("备份文件失败", e)
        }
    }
    
    fun cleanOldBackups(repository: Repository) {
        val maxBackups = repository.backupConfig?.maxBackups ?: 10
        cleanOldBackups(maxBackups)
    }
    
    private fun getBackupDirectory(): Path {
        val projectDir = project.basePath?.let { Path.of(it) }
            ?: throw RuntimeException("无法获取项目目录")
        
        return projectDir.resolve(".stack-file-sync/backups").also {
            Files.createDirectories(it)
        }
    }
    
    private fun getTargetDirectory(repository: Repository): Path {
        val projectDir = project.basePath?.let { Path.of(it) }
            ?: throw RuntimeException("无法获取项目目录")
            
        return if (repository.targetDirectory.startsWith("/")) {
            Path.of(repository.targetDirectory)
        } else {
            projectDir.resolve(repository.targetDirectory)
        }
    }
    
    data class BackupInfo(
        val timestamp: LocalDateTime,
        val repository: String,
        val fileCount: Int,
        val path: Path
    )
    
    fun getBackups(repository: Repository? = null): List<BackupInfo> {
        return try {
            Files.list(backupRoot)
                .filter { Files.isDirectory(it) }
                .filter { dir ->
                    repository == null || dir.fileName.toString()
                        .startsWith("${repository.name}_")
                }
                .map { dir ->
                    val (repoName, timestamp) = parseBackupDirName(dir)
                    val fileCount = Files.walk(dir)
                        .filter { Files.isRegularFile(it) }
                        .count()
                        .toInt()
                    
                    BackupInfo(timestamp, repoName, fileCount, dir)
                }
                .sorted { a, b -> b.timestamp.compareTo(a.timestamp) }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun restoreBackup(backup: BackupInfo, repository: Repository) {
        try {
            val targetDir = getTargetDirectory(repository)
            
            // 备份当前文件
            val currentFiles = Files.walk(targetDir)
                .filter { Files.isRegularFile(it) }
                .toList()
            backupFiles(repository, currentFiles)
            
            // 恢复备份文件
            Files.walk(backup.path)
                .filter { Files.isRegularFile(it) }
                .forEach { file ->
                    val relativePath = backup.path.relativize(file)
                    val targetFile = targetDir.resolve(relativePath)
                    
                    Files.createDirectories(targetFile.parent)
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                }
        } catch (e: Exception) {
            throw SyncException.FileException("恢复备份失败", e)
        }
    }
    
    private fun parseBackupDirName(dir: Path): Pair<String, LocalDateTime> {
        val fileName = dir.fileName.toString()
        val parts = fileName.split("_")
        if (parts.size != 2) {
            throw IllegalArgumentException("无效的备份目录名: $fileName")
        }
        
        val repoName = parts[0]
        val timestamp = LocalDateTime.parse(
            parts[1],
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        )
        
        return repoName to timestamp
    }
    
    fun exportBackup(backup: BackupInfo, targetFile: File) {
        try {
            ZipOutputStream(targetFile.outputStream().buffered()).use { zip ->
                Files.walk(backup.path)
                    .filter { Files.isRegularFile(it) }
                    .forEach { file ->
                        val relativePath = backup.path.relativize(file).toString()
                        zip.putNextEntry(ZipEntry(relativePath))
                        Files.copy(file, zip)
                        zip.closeEntry()
                    }
            }
        } catch (e: Exception) {
            throw SyncException.FileException("导出备份失败", e)
        }
    }
    
    fun importBackup(repository: Repository, zipFile: File) {
        try {
            // 创建临时目录
            val tempDir = Files.createTempDirectory("backup-import")
            
            // 解压文件
            java.util.zip.ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val target = tempDir.resolve(entry.name)
                    if (entry.isDirectory) {
                        Files.createDirectories(target)
                    } else {
                        Files.createDirectories(target.parent)
                        zip.getInputStream(entry).use { input ->
                            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                }
            }
            
            // 创建备份
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            val backupDir = backupRoot.resolve("${repository.name}_$timestamp")
            Files.move(tempDir, backupDir)
            
        } catch (e: Exception) {
            throw SyncException.FileException("导入备份失败", e)
        }
    }
    
    data class BackupStats(
        val totalSize: Long,
        val fileTypes: Map<String, Int>,
        val modifiedTimes: Map<String, LocalDateTime>,
        val largestFiles: List<Pair<Path, Long>>
    )
    
    fun getBackupStats(backup: BackupInfo): BackupStats {
        try {
            var totalSize = 0L
            val fileTypes = mutableMapOf<String, Int>()
            val modifiedTimes = mutableMapOf<String, LocalDateTime>()
            val fileSizes = mutableListOf<Pair<Path, Long>>()
            
            Files.walk(backup.path)
                .filter { Files.isRegularFile(it) }
                .forEach { file ->
                    val size = Files.size(file)
                    totalSize += size
                    
                    val extension = file.fileName.toString().substringAfterLast(".", "")
                    fileTypes[extension] = fileTypes.getOrDefault(extension, 0) + 1
                    
                    val modifiedTime = Files.getLastModifiedTime(file).toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()
                    val relativePath = backup.path.relativize(file).toString()
                    modifiedTimes[relativePath] = modifiedTime
                    
                    fileSizes.add(file to size)
                }
            
            return BackupStats(
                totalSize = totalSize,
                fileTypes = fileTypes,
                modifiedTimes = modifiedTimes,
                largestFiles = fileSizes.sortedByDescending { it.second }.take(10)
            )
        } catch (e: Exception) {
            throw SyncException.FileException("获取备份统计信息失败", e)
        }
    }
    
    data class BackupDiff(
        val addedFiles: List<String>,
        val modifiedFiles: List<String>,
        val deletedFiles: List<String>
    )
    
    fun compareBackups(backup1: BackupInfo, backup2: BackupInfo): BackupDiff {
        try {
            val files1 = getBackupFiles(backup1.path)
            val files2 = getBackupFiles(backup2.path)
            
            val paths1 = files1.keys
            val paths2 = files2.keys
            
            val addedFiles = paths2.filter { it !in paths1 }
            val deletedFiles = paths1.filter { it !in paths2 }
            val modifiedFiles = paths1.filter { path ->
                path in paths2 && files1[path] != files2[path]
            }
            
            return BackupDiff(
                addedFiles = addedFiles.toList(),
                modifiedFiles = modifiedFiles.toList(),
                deletedFiles = deletedFiles.toList()
            )
        } catch (e: Exception) {
            throw SyncException.FileException("比较备份失败", e)
        }
    }
    
    private fun getBackupFiles(backupPath: Path): Map<String, ByteArray> {
        return Files.walk(backupPath)
            .filter { Files.isRegularFile(it) }
            .associate { file ->
                val relativePath = backupPath.relativize(file).toString()
                val hash = MessageDigest.getInstance("MD5")
                    .digest(Files.readAllBytes(file))
                relativePath to hash
            }
    }
} 