package com.gregisoft.explorer.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "garmin",
        description = "Configure Garmin Connect Personal API access.",
        mixinStandardHelpOptions = true,
        subcommands = {
                GarminConfigureCommand.class,
                GarminStatusCommand.class,
                GarminLogoutCommand.class
        }
)
public class GarminCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
