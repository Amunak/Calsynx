#!/bin/bash
# Script to generate a signing key for releasing Calsynx

set -e

echo "=== Calsynx Release Signing Key Generator ==="
echo ""
echo "This will generate a keystore for signing release APKs."
echo "You'll need to add the generated secrets to GitHub."
echo ""

# Prompt for information
read -p "Your name (e.g., John Doe): " NAME
read -p "Organization (e.g., Your Organization): " ORG
read -p "City: " CITY
read -p "State/Province: " STATE
read -p "Country code (e.g., US): " COUNTRY

# Generate strong random passwords
KEYSTORE_PASS=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-24)
KEY_PASS=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-24)
KEY_ALIAS="calsynx"

echo ""
echo "Generating keystore..."

# Generate keystore
keytool -genkey -v \
  -keystore calsynx-release.keystore \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass "$KEYSTORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=$NAME, O=$ORG, L=$CITY, ST=$STATE, C=$COUNTRY"

echo ""
echo "=== Keystore generated successfully! ==="
echo ""
echo "IMPORTANT: Add these secrets to your GitHub repository:"
echo "(Settings → Secrets and variables → Actions → New repository secret)"
echo ""
echo "Secret name: SIGNING_KEYSTORE"
echo "Secret value:"
base64 -w 0 calsynx-release.keystore
echo ""
echo ""
echo "Secret name: SIGNING_KEY_ALIAS"
echo "Secret value: $KEY_ALIAS"
echo ""
echo "Secret name: SIGNING_KEYSTORE_PASSWORD"
echo "Secret value: $KEYSTORE_PASS"
echo ""
echo "Secret name: SIGNING_KEY_PASSWORD"
echo "Secret value: $KEY_PASS"
echo ""
echo "=== BACKUP YOUR KEYSTORE ==="
echo "File: calsynx-release.keystore"
echo ""
echo "Store this file and the passwords securely!"
echo "You'll need them to sign future releases."
echo "If you lose this, you cannot update the app!"
