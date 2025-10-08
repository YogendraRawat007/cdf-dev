package com.cognite.cdfdev

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

class EnvCommand : CliktCommand(name = "env") {
    init {
        subcommands(EnvSetCommand(), EnvApplyPresetCommand())
    }

    override fun run() = Unit
}

class EnvSetCommand : CliktCommand(name = "set") {
    private val envVars by argument(help = "Environment variables to set").multiple(required = true)
    private val restart by option("--restart", help = "Restart pods after patching").flag(default = true)

    override fun run() {
        val config = CdfDevConfig.findConfig()
            ?: throw IllegalStateException("No .cdf-dev.yaml found")

        echo(" Setting environment variables on ${config.service.name}")

        envVars.forEach { envVar ->
            val (key, value) = envVar.split("=", limit = 2).let {
                if (it.size != 2) throw IllegalArgumentException("Invalid format: $envVar. Use KEY=VALUE")
                it[0] to it[1]
            }

            echo("  Setting $key=$value")

            val patchJson = """[{"op": "add", "path": "/spec/template/spec/containers/0/env/-", "value": {"name": "$key", "value": "$value"}}]"""

            val result = CommandExecutor.execute(
                listOf(
                    "kubectl", "patch", "deployment", config.service.deployment,
                    "-n", config.service.namespace,
                    "--context", config.cluster.context,
                    "--type=json",
                    "-p", patchJson
                ),
                printOutput = false
            )

            if (!result.success) {
                throw RuntimeException("Failed to set $key: ${result.stderr}")
            }
        }

        if (restart) {
            echo("\n Restarting deployment...")
            val restartResult = CommandExecutor.execute(
                listOf(
                    "kubectl", "rollout", "restart",
                    "deployment/${config.service.deployment}",
                    "-n", config.service.namespace,
                    "--context", config.cluster.context
                )
            )
            if (!restartResult.success) {
                throw RuntimeException("Restart failed: ${restartResult.stderr}")
            }
        }

        echo("\n Environment variables updated!")
    }
}

class EnvApplyPresetCommand : CliktCommand(name = "preset") {
    private val presetName by argument(help = "Preset name from .cdf-dev.yaml")

    override fun run() {
        val config = CdfDevConfig.findConfig()
            ?: throw IllegalStateException("No .cdf-dev.yaml found")

        val preset = config.envPresets?.get(presetName)
            ?: throw IllegalArgumentException("Preset '$presetName' not found in config")

        echo(" Applying preset: $presetName")

        preset.forEach { envVar ->
            val (key, value) = envVar.split("=", limit = 2)
            echo("  Setting $key=$value")

            val patchJson = """[{"op": "add", "path": "/spec/template/spec/containers/0/env/-", "value": {"name": "$key", "value": "$value"}}]"""

            CommandExecutor.execute(
                listOf(
                    "kubectl", "patch", "deployment", config.service.deployment,
                    "-n", config.service.namespace,
                    "--context", config.cluster.context,
                    "--type=json",
                    "-p", patchJson
                ),
                printOutput = false
            )
        }

        echo("\n Restarting deployment...")
        CommandExecutor.execute(
            listOf(
                "kubectl", "rollout", "restart",
                "deployment/${config.service.deployment}",
                "-n", config.service.namespace,
                "--context", config.cluster.context
            )
        )

        echo("\n Preset applied!")
    }
}
