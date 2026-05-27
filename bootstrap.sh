#!/usr/bin/env bash
set -e
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
WRAPPER_VERSION="8.9"

if file "$WRAPPER_JAR" 2>/dev/null | grep -q "Zip"; then
  echo "✓ gradle-wrapper.jar already valid"
  exit 0
fi

echo "Downloading gradle-wrapper.jar v${WRAPPER_VERSION}..."
curl -sL \
  "https://services.gradle.org/distributions/gradle-${WRAPPER_VERSION}-bin.zip" \
  -o /tmp/gradle-dist.zip

echo "Extracting jar..."
unzip -l /tmp/gradle-dist.zip | grep "gradle-wrapper" | awk '{print $4}' | head -1 | \
  xargs -I{} unzip -j /tmp/gradle-dist.zip {} -d /tmp/ 2>/dev/null

cp /tmp/gradle-wrapper*.jar "$WRAPPER_JAR"
echo "✓ gradle-wrapper.jar installed ($(du -h $WRAPPER_JAR | cut -f1))"
