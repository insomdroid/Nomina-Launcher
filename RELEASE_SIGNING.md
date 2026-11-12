# Release signing — deferred

Permanent release signing has intentionally been deferred. The current GitHub Actions workflow continues to use its previous temporary signing key so that builds do not require GitHub Actions secrets.

When permanent signing is ready, configure one stable keystore and the following repository secrets:

- `NOMINA_RELEASE_KEYSTORE_BASE64`
- `NOMINA_RELEASE_STORE_PASSWORD`
- `NOMINA_RELEASE_KEY_ALIAS`
- `NOMINA_RELEASE_KEY_PASSWORD`

Never commit the keystore, passwords, or Base64 content to the repository or paste them into an issue or chat.

## Important update-compatibility warning

The temporary signing key is generated anew on each workflow run. APKs from different runs do not share a stable signing identity. A permanent key configured later cannot be assumed to match an APK already installed on a device. Do not uninstall an existing production installation or assume an in-place update will work until the signing certificate has been compared.

Once selected, keep a secure offline backup of the keystore and its passwords. Losing the key prevents signing future APKs with the same identity.
