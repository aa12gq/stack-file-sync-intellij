package com.stackfilesync.intellij.utils

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key
import java.io.File
import java.util.concurrent.CompletableFuture

class CommandExecutor {
    companion object {
        fun execute(command: String, workingDir: File): CompletableFuture<CommandResult> {
            val future = CompletableFuture<CommandResult>()
            val output = StringBuilder()
            val error = StringBuilder()
            
            try {
                val commandLine = if (isWindows()) {
                    GeneralCommandLine("cmd", "/c", command)
                } else {
                    GeneralCommandLine("sh", "-c", command)
                }
                
                commandLine.setWorkDirectory(workingDir)
                
                val processHandler = OSProcessHandler(commandLine)
                
                processHandler.addProcessListener(object : ProcessAdapter() {
                    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                        when (outputType.toString()) {
                            "stdout" -> output.append(event.text)
                            "stderr" -> error.append(event.text)
                        }
                    }
                    
                    override fun processTerminated(event: ProcessEvent) {
                        val exitCode = event.exitCode
                        future.complete(
                            CommandResult(
                                exitCode = exitCode,
                                output = output.toString(),
                                error = error.toString()
                            )
                        )
                    }
                })
                
                processHandler.startNotify()
                
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
            
            return future
        }
        
        private fun isWindows(): Boolean {
            return System.getProperty("os.name").lowercase().contains("windows")
        }
    }
    
    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val error: String
    )
} 