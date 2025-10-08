package com.cognite.cdfdev

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class RollbackCommand : CliktCommand(name = "rollback") {
    private val wait by option("--wait", help = "Wait for rollback to complete").flag(default = true)

    override fun run() {
        val config = CdfDevConfig.findConfig()
            ?: throw IllegalStateException("No .cdf-dev.yaml found")

        echo(" Rolling back ${config.service.name}...")

        val rollbackResult = CommandExecutor.execute(
            listOf(
                "kubectl", "rollout", "undo",
                "deployment/${config.service.deployment}",
                "-n", config.service.namespace,
                "--context", config.cluster.context
            )
        )

        if (!rollbackResult.success) {
            throw RuntimeException("Rollback failed: ${rollbackResult.stderr}")
        }

        if (wait) {
            echo("\n Waiting for rollback to complete...")
            val statusResult = CommandExecutor.execute(
                listOf(
                    "kubectl", "rollout", "status",
                    "deployment/${config.service.deployment}",
                    "-n", config.service.namespace,
                    "--context", config.cluster.context,
                    "--timeout=3m"
                )
            )

            if (!statusResult.success) {
                throw RuntimeException("Rollback status check failed: ${statusResult.stderr}")
            }
        }

        echo("\n Rollback complete!")
    }
}
