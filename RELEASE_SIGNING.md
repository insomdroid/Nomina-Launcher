# Release signing setup

The release workflow now requires one permanent Android signing key. It no longer generates a different temporary key for each build.

## Required GitHub Actions secrets

Add these four repository secrets in **Settings → Secrets and variables → Actions → New repository secret**:

- `NOMINA_RELEASE_KEYSTORE_BASE64` — the complete keystore file encoded as one Base64 string.
- `NOMINA_RELEASE_STORE_PASSWORD` — keystore password.
- `NOMINA_RELEASE_KEY_ALIAS` — alias of the release key.
- `NOMINA_RELEASE_KEY_PASSWORD` — password for that key.

Never commit the keystore, passwords, or Base64 content to the repository or paste them into an issue or chat.

## Important update-compatibility warning

The previous workflow generated a new temporary keystore on every run. Those APKs do not share a stable signing identity, and their private signing key was not retained. A permanent key configured now cannot be assumed to match any previously installed temporary-signed APK. Do not uninstall an existing production installation or assume an in-place update will work until the signing certificate has been compared.

Once selected, keep a secure offline backup of the keystore and its passwords. Losing the key prevents signing future APKs with the same identity.
