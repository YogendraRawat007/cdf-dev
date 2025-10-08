package com.cognite.cdfdev

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

class LogsCommand : CliktCommand(name = "logs") {
    private val follow by option("-f", "--follow", help = "Follow log output").flag()
    private val tail by option("--tail", help = "Number of lines to show").int()

    override fun run() {
        val config = CdfDevConfig.findConfig()
            ?: throw IllegalStateException("No .cdf-dev.yaml found")

        echo(" Fetching logs for ${config.service.name}...")

        val command = mutableListOf(
            "kubectl", "logs",
            "deployment/${config.service.deployment}",
            "-n", config.service.namespace,
            "--context", config.cluster.context,
            "--tail=${tail ?: 100}"
        )

        if (follow) {
            command.add("-f")
        }

        val result = CommandExecutor.execute(command)

        if (!result.success) {
            throw RuntimeException("Failed to fetch logs: ${result.stderr}")
        }
    }
}
