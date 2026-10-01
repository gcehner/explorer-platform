package com.gregisoft.explorer.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "tour",
        description = "Manage Explorer tours.",
        mixinStandardHelpOptions = true,
        subcommands = TourImportCommand.class
)
public class TourCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
