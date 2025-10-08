package com.cognite.cdfdev

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional

class PortForwardCommand : CliktCommand(name = "port-forward") {
    private val ports by argument(help = "Ports to forward (format: local:remote or just use config defaults)").optional()

    override fun run() {
        val config = CdfDevConfig.findConfig()
            ?: throw IllegalStateException("No .cdf-dev.yaml found")

        val portMapping = if (ports != null) {
            ports!!
        } else {
            // Use default ports from config
            val apiPort = config.ports?.api ?: 8080
            val metricsPort = config.ports?.metrics

            if (metricsPort != null) {
                "$apiPort:$apiPort $metricsPort:$metricsPort"
            } else {
                "$apiPort:$apiPort"
            }
        }

        echo(" Port forwarding ${config.service.name}...")
        echo("   Ports: $portMapping")
        echo("   Press Ctrl+C to stop\n")

        val command = mutableListOf(
            "kubectl", "port-forward",
            "deployment/${config.service.deployment}",
            "-n", config.service.namespace,
            "--context", config.cluster.context
        )

        command.addAll(portMapping.split(" "))

        val result = CommandExecutor.execute(command)

        if (!result.success) {
            throw RuntimeException("Port forwarding failed: ${result.stderr}")
        }
    }
}
