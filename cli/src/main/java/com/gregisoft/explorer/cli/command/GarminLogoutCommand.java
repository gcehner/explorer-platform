package com.gregisoft.explorer.cli.command;

import com.gregisoft.explorer.cli.garmin.CredentialStore;
import com.gregisoft.explorer.cli.garmin.CredentialStoreException;
import com.gregisoft.explorer.cli.garmin.WindowsCredentialStore;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "logout",
        description = "Remove the locally stored Garmin Personal API key.",
        mixinStandardHelpOptions = true
)
public class GarminLogoutCommand implements Callable<Integer> {

    private final CredentialStore credentialStore;

    @Spec
    private CommandSpec spec;

    public GarminLogoutCommand() {
        this(new WindowsCredentialStore());
    }

    GarminLogoutCommand(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    @Override
    public Integer call() {
        try {
            boolean removed = credentialStore.delete();
            spec.commandLine().getOut().println(removed
                    ? "Removed the locally stored Garmin Personal API key."
                    : "No locally stored Garmin Personal API key was found.");
            spec.commandLine().getOut().println(
                    "Local logout does not revoke the key. Delete it in Garmin Connect Web to revoke access.");
            return 0;
        } catch (CredentialStoreException exception) {
            spec.commandLine().getErr().println("Garmin logout failed: " + exception.getMessage());
            return 1;
        }
    }
}
