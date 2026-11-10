package com.gregisoft.explorer.cli.garmin;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

public final class WindowsCredentialStore implements CredentialStore {

    public static final String TARGET = "ExplorerPlatform/GarminPersonalApi";
    private static final int CRED_TYPE_GENERIC = 1;
    private static final int CRED_PERSIST_LOCAL_MACHINE = 2;
    private static final int ERROR_NOT_FOUND = 1168;

    @Override
    public Optional<char[]> read() {
        requireWindows();
        PointerByReference result = new PointerByReference();
        if (!CredentialApi.INSTANCE.CredReadW(
                new WString(TARGET), CRED_TYPE_GENERIC, 0, result)) {
            int error = Native.getLastError();
            if (error == ERROR_NOT_FOUND) {
                return Optional.empty();
            }
            throw failure("read", error);
        }

        Pointer pointer = result.getValue();
        try {
            Credential credential = new Credential(pointer);
            if (credential.CredentialBlob == null || credential.CredentialBlobSize <= 0) {
                throw new CredentialStoreException(
                        "Windows Credential Manager returned an empty Garmin credential.");
            }
            byte[] encoded = credential.CredentialBlob.getByteArray(0, credential.CredentialBlobSize);
            try {
                CharBuffer decoded = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded));
                char[] secret = new char[decoded.remaining()];
                decoded.get(secret);
                return Optional.of(secret);
            } finally {
                Arrays.fill(encoded, (byte) 0);
            }
        } finally {
            CredentialApi.INSTANCE.CredFree(pointer);
        }
    }

    @Override
    public void store(char[] secret) {
        requireWindows();
        if (secret == null || secret.length == 0) {
            throw new CredentialStoreException("Cannot store an empty Garmin credential.");
        }

        ByteBuffer encodedBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(secret));
        byte[] encoded = new byte[encodedBuffer.remaining()];
        encodedBuffer.get(encoded);
        Memory memory = new Memory(encoded.length);
        try {
            memory.write(0, encoded, 0, encoded.length);
            Credential credential = new Credential();
            credential.Type = CRED_TYPE_GENERIC;
            credential.TargetName = new WString(TARGET);
            credential.CredentialBlobSize = encoded.length;
            credential.CredentialBlob = memory;
            credential.Persist = CRED_PERSIST_LOCAL_MACHINE;
            credential.UserName = new WString("Explorer Platform");
            credential.write();
            if (!CredentialApi.INSTANCE.CredWriteW(credential, 0)) {
                throw failure("store", Native.getLastError());
            }
        } finally {
            memory.clear();
            Arrays.fill(encoded, (byte) 0);
        }
    }

    @Override
    public boolean delete() {
        requireWindows();
        if (CredentialApi.INSTANCE.CredDeleteW(new WString(TARGET), CRED_TYPE_GENERIC, 0)) {
            return true;
        }
        int error = Native.getLastError();
        if (error == ERROR_NOT_FOUND) {
            return false;
        }
        throw failure("delete", error);
    }

    private static void requireWindows() {
        if (!Platform.isWindows()) {
            throw new CredentialStoreException(
                    "Garmin credential storage currently requires Windows Credential Manager.");
        }
    }

    private static CredentialStoreException failure(String operation, int error) {
        return new CredentialStoreException(
                "Could not " + operation + " the Garmin credential in Windows Credential Manager"
                        + " (Windows error " + error + ").");
    }

    private interface CredentialApi extends StdCallLibrary {
        CredentialApi INSTANCE = Native.load("Advapi32", CredentialApi.class);

        boolean CredReadW(WString targetName, int type, int flags, PointerByReference credential);

        boolean CredWriteW(Credential credential, int flags);

        boolean CredDeleteW(WString targetName, int type, int flags);

        void CredFree(Pointer credential);
    }

    @Structure.FieldOrder({
            "Flags", "Type", "TargetName", "Comment", "LastWritten", "CredentialBlobSize",
            "CredentialBlob", "Persist", "AttributeCount", "Attributes", "TargetAlias", "UserName"
    })
    public static class Credential extends Structure {
        public int Flags;
        public int Type;
        public WString TargetName;
        public WString Comment;
        public FileTime LastWritten;
        public int CredentialBlobSize;
        public Pointer CredentialBlob;
        public int Persist;
        public int AttributeCount;
        public Pointer Attributes;
        public WString TargetAlias;
        public WString UserName;

        public Credential() {
            LastWritten = new FileTime();
        }

        public Credential(Pointer pointer) {
            super(pointer);
            LastWritten = new FileTime();
            read();
        }
    }

    @Structure.FieldOrder({"dwLowDateTime", "dwHighDateTime"})
    public static class FileTime extends Structure {
        public int dwLowDateTime;
        public int dwHighDateTime;
    }
}
