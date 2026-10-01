package com.gregisoft.explorer.cli;

import com.gregisoft.explorer.cli.command.TourCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "explorer",
        description = "Explorer Platform command-line tools.",
        mixinStandardHelpOptions = true,
        subcommands = TourCommand.class
)
public class ExplorerCli implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ExplorerCli()).execute(args);
        System.exit(exitCode);
    }
}
