# Garmin Personal API CLI

Explorer uses Garmin's official Garmin Connect Personal API for access to the owner's Garmin data.
The CLI does not ask for a Garmin account password and does not use Garmin's private web endpoints.

Explorer supports direct import of a selected Garmin activity. Activity listing remains on the
Garmin Connect website; the CLI intentionally does not duplicate it.

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

## Import an activity

Open an activity on Garmin Connect Web and copy either its numeric activity ID or its canonical URL.
The ID appears after `/modern/activity/` in the URL.

```powershell
java -jar .\cli\target\explorer.jar garmin import 123456789
```

The equivalent canonical URL is also accepted:

```powershell
java -jar .\cli\target\explorer.jar garmin import https://connect.garmin.com/modern/activity/123456789
```

Use `--backend-url` when the Explorer backend is not at its default location. Resolution remains
identical to manual GPX import: the option, then `EXPLORER_BACKEND_URL`, then
`http://localhost:8080`.

The command authenticates with the stored key, checks Explorer for an earlier import, downloads the
original activity ZIP from Garmin's official Personal API, decodes its FIT activity and creates a
temporary GPX. The existing GPX normalization pipeline then creates a DRAFT tour. Temporary GPX
files are removed after success or failure.

Explorer stores `GARMIN_CONNECT` and the canonical Garmin activity ID with the imported track. If
the activity was already imported, the command reports the existing Explorer tour ID and does not
modify, overwrite or publish it. Database uniqueness remains the final protection if two imports
race. An ambiguous backend POST is not retried automatically.

Only exact HTTPS URLs on `connect.garmin.com` matching `/modern/activity/{id}` are accepted. The CLI
extracts the ID locally and never fetches the supplied URL.

### FIT conversion policy

- The Garmin archive must contain exactly one FIT file and no unexpected files.
- ZIP entry count, compressed download size and decompressed size are bounded.
- The FIT file must be one Activity file with exactly one non-multisport session.
- At least two usable GPS records and two elevation records are required by Explorer's existing GPX
  processor.
- Latitude, longitude, elevation, timestamps and record order are preserved when present.
- Equal timestamps are preserved; decreasing timestamps are rejected.
- Missing coordinates, elevation or timestamps are not fabricated.

Errors such as an expired key, missing activity, Garmin rate limiting, invalid ZIP, corrupt FIT or
missing GPS data are reported without printing keys or bearer tokens.

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
- Activity discovery/listing is intentionally performed in Garmin Connect Web.
- Multisport and multi-session FIT files are not imported.
