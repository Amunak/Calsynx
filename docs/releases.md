# Release Management

## Creating Signed Releases

The project uses GitHub Actions to automatically build, sign, and publish releases when you create a version tag.

### Initial Setup

1. Generate a signing keystore (one-time setup):
```bash
./generate-signing-key.sh
```

2. Add the following secrets to your GitHub repository:
   - Go to Settings → Secrets and variables → Actions
   - Add these repository secrets:
     - `SIGNING_KEYSTORE` - Base64-encoded keystore file (output from script)
     - `SIGNING_KEY_ALIAS` - Key alias (default: `calsynx`)
     - `SIGNING_KEYSTORE_PASSWORD` - Keystore password (from script output)
     - `SIGNING_KEY_PASSWORD` - Key password (from script output)

3. **IMPORTANT**: Backup your keystore file (`calsynx-release.keystore`) and passwords securely!
   - If you lose this keystore, you cannot update the app for existing users
   - Users would need to uninstall and reinstall to get updates

### Publishing a Release

1. Update version in `app/build.gradle.kts`:
```kotlin
versionCode = 3
versionName = "1.2.0"
```

2. Commit changes:
```bash
git add app/build.gradle.kts
git commit -m "Bump version to 1.2.0"
```

3. Tag and push the release (use semver without 'v' prefix):
```bash
git tag 1.2.0
git push origin master
git push origin 1.2.0
```

The GitHub Action will automatically:
- Run tests
- Build and sign the APK
- Create a GitHub Release with the signed APK
- Update the F-Droid repository on the `fdroid` branch

### F-Droid Repository

Users can add your F-Droid repository in Droid-ify or F-Droid:
```
https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_REPO/fdroid/repo
```

The repository is automatically updated on each tagged release.

## Troubleshooting

### Build fails with "No value for signing secrets"
- Make sure all four signing secrets are added to GitHub repository settings
- Secrets are only available on tagged releases (not regular pushes)

### APK signature mismatch
- You may have generated a new keystore
- Users must uninstall the old app before installing with the new signature
- Always backup and reuse the same keystore

### F-Droid repository not updating
- Check the Actions tab for workflow errors
- Ensure the `fdroid` branch exists and has proper permissions
- The workflow needs write permissions to push to the `fdroid` branch
