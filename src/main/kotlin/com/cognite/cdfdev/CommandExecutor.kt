package com.cognite.cdfdev

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File

object CommandExecutor {
    data class CommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String
    ) {
        val success: Boolean get() = exitCode == 0
    }

    fun execute(
        command: List<String>,
        workingDir: String? = null,
        printOutput: Boolean = true,
        env: Map<String, String>? = null
    ): CommandResult {
        return executeInternal(
            command,
            workingDir?.let { File(it) },
            printOutput,
            env
        )
    }

    fun execute(
        command: List<String>,
        workingDir: File,
        printOutput: Boolean = true,
        env: Map<String, String>? = null
    ): CommandResult {
        return executeInternal(command, workingDir, printOutput, env)
    }

    private fun executeInternal(
        command: List<String>,
        workingDir: File? = null,
        printOutput: Boolean = true,
        env: Map<String, String>? = null
    ): CommandResult {
        val processBuilder = ProcessBuilder(command)

        if (workingDir != null) {
            processBuilder.directory(workingDir)
        }

        env?.forEach { (key, value) ->
            processBuilder.environment()[key] = value
        }

        if (printOutput) {
            println("Running: ${command.joinToString(" ")}")
        }

        processBuilder.redirectErrorStream(false)
        val process = processBuilder.start()

        val stdout = StringBuilder()
        val stderr = StringBuilder()

        val stdoutThread = Thread {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    stdout.appendLine(line)
                    if (printOutput) println(line)
                }
            }
        }

        val stderrThread = Thread {
            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                reader.lineSequence().forEach { line ->
                    stderr.appendLine(line)
                    if (printOutput) System.err.println(line)
                }
            }
        }

        stdoutThread.start()
        stderrThread.start()

        val exitCode = process.waitFor()
        stdoutThread.join()
        stderrThread.join()

        return CommandResult(exitCode, stdout.toString(), stderr.toString())
    }
}
