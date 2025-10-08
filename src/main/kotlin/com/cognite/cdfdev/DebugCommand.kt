package com.cognite.cdfdev

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class DebugCommand : CliktCommand(
    name = "debug",
    help = "Debug tools - exec into pod, test endpoints, check connectivity"
) {
    private val shell by option("--shell", "-s", help = "Open an interactive shell").flag()
    private val curl by option("--curl", "-c", help = "Test endpoint with curl (e.g., --curl /ping)")
    private val command by argument(help = "Command to execute in pod").multiple()

    override fun run() {
        val config = CdfDevConfig.findConfig()
            ?: throw IllegalStateException("No .cdf-dev.yaml found")

        when {
            shell -> {
                echo("Opening shell in ${config.service.name}...")
                val shellCommand = listOf(
                    "kubectl", "exec", "-it",
                    "deployment/${config.service.deployment}",
                    "-n", config.service.namespace,
                    "--context", config.cluster.context,
                    "--", "/bin/sh"
                )
                val result = CommandExecutor.execute(shellCommand)
                if (!result.success) {
                    throw RuntimeException("Shell exec failed: ${result.stderr}")
                }
            }
            curl != null -> {
                echo("Testing endpoint: $curl")
                val apiPort = config.ports?.api ?: 8080
                val curlCommand = listOf(
                    "kubectl", "exec",
                    "deployment/${config.service.deployment}",
                    "-n", config.service.namespace,
                    "--context", config.cluster.context,
                    "--", "curl", "-s", "-w", "\\n\\nHTTP Status: %{http_code}\\n",
                    "http://localhost:$apiPort$curl"
                )
                val result = CommandExecutor.execute(curlCommand)
                echo(result.stdout)
                if (!result.success) {
                    echo("Error: ${result.stderr}", err = true)
                }
            }
            command.isNotEmpty() -> {
                echo("Executing: ${command.joinToString(" ")}")
                val execCommand = listOf(
                    "kubectl", "exec",
                    "deployment/${config.service.deployment}",
                    "-n", config.service.namespace,
                    "--context", config.cluster.context,
                    "--"
                ) + command
                val result = CommandExecutor.execute(execCommand)
                echo(result.stdout)
                if (!result.success) {
                    echo("Error: ${result.stderr}", err = true)
                }
            }
            else -> {
                // Default: show debug info
                echo("Debug Info for ${config.service.name}\n")

                // Get pods
                echo("Pods:")
                val podsResult = CommandExecutor.execute(
                    listOf(
                        "kubectl", "get", "pods",
                        "-n", config.service.namespace,
                        "-l", "app=${config.service.name}",
                        "--context", config.cluster.context
                    )
                )
                echo(podsResult.stdout)

                // Get events
                echo("\n Recent Events:")
                val eventsResult = CommandExecutor.execute(
                    listOf(
                        "kubectl", "get", "events",
                        "-n", config.service.namespace,
                        "--field-selector", "involvedObject.name=${config.service.deployment}",
                        "--context", config.cluster.context,
                        "--sort-by", ".lastTimestamp"
                    )
                )
                echo(eventsResult.stdout.lines().takeLast(10).joinToString("\n"))

                echo("\n Quick commands:")
                echo("  cdf-dev debug --shell              # Open shell")
                echo("  cdf-dev debug --curl /ping         # Test endpoint")
                echo("  cdf-dev debug curl localhost:8080  # Custom command")
            }
        }
    }
}
