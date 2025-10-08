package com.cognite.cdfdev

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class CdfDev : CliktCommand(name = "cdf-dev", help = "CDF Development CLI - Deploy and manage services on dev clusters") {
    override fun run() = Unit
}

fun main(args: Array<String>) = CdfDev()
    .subcommands(
        DeployCommand(),
        ApplyCommand(),
        EnvCommand(),
        LogsCommand(),
        PortForwardCommand(),
        RollbackCommand(),
        DebugCommand()
    )
    .main(args)
