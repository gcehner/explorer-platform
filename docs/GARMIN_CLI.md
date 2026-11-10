# Garmin Personal API CLI

Explorer uses Garmin's official Garmin Connect Personal API for access to the owner's Garmin data.
The CLI does not ask for a Garmin account password and does not use Garmin's private web endpoints.

Phase G2 provides credential configuration and authentication only. Activity listing, FIT download,
GPX conversion and activity import are not implemented yet.

## Requirements

- Windows with Windows Credential Manager available.
- Java 21.
- Maven for building the CLI.
- A Garmin Connect Personal API key.

Create the key in Garmin Connect Web while signed in:

1. Open Garmin Connect Account Settings.
2. Open the Personal API page.
3. Select **Create Key** and accept Garmin's terms.
4. Choose an expiration period.
5. Copy the key when it is displayed. Garmin displays it only once.

Garmin supports key expiration periods of 30, 90, 180 days or one year. Account login and any MFA
challenge take place in Garmin Connect Web, not in Explorer.

## Build

From the repository root in PowerShell:

```powershell
mvn -f .\cli\pom.xml clean package
```

The executable JAR is created at `cli\target\explorer.jar`.

## Configure Garmin access

Run the command directly in an interactive PowerShell terminal:

```powershell
java -jar .\cli\target\explorer.jar garmin configure
```

The CLI reads the key without echoing it, exchanges it for an access token through Garmin's official
HTTPS token endpoint, and stores the key only if authentication succeeds. The key is stored as a
Generic Credential in Windows Credential Manager under:

```text
ExplorerPlatform/GarminPersonalApi
```

The API key and access token are never written to project files, `.env` files or CLI output. Access
tokens remain in memory only. If no interactive console is available, configuration fails instead of
falling back to visible input.

## Check status

```powershell
java -jar .\cli\target\explorer.jar garmin status
```

The command reports whether a key is stored and whether Garmin currently accepts it. It performs a
fresh token exchange and does not display the key or token.

## Remove local credentials

```powershell
java -jar .\cli\target\explorer.jar garmin logout
```

Logout removes the local Windows Credential Manager entry and is safe to repeat. It does not revoke
the key at Garmin. To revoke access, delete the Personal API key in Garmin Connect Web. Run
`garmin configure` again to replace an expired, revoked or rotated key.

## Authentication contract

The CLI sends an empty `POST` request to Garmin's documented token endpoint with the API key in the
`Authorization: ApiKey ...` header. Garmin returns the access token as a plain-text JWT. Explorer
requires a well-formed JWT with a future numeric `exp` claim. Garmin documents that tokens remain
valid for 24 hours or until their Personal API key expires, whichever happens first, and does not
provide refresh tokens.

The Personal API documentation does not require scopes for this key exchange. Explorer deliberately
does not depend on Garmin error-response bodies because Garmin does not document a stable error
schema for the token endpoint; HTTP status is mapped to safe messages without echoing response data.

## Known limitations

- Credential persistence currently supports Windows only.
- Each `status` or `configure` invocation performs a token exchange; bearer-token caching is outside
  G2.
- The CLI cannot revoke a key remotely.
- Garmin activity discovery and import commands will be added in later phases.
