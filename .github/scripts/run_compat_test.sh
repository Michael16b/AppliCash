#!/usr/bin/env bash
# Usage: run_compat_test.sh <apk_path> <name> <api> <abi>
# Installs APK on connected device/emulator and runs a basic smoke test.

set -euo pipefail
APK_PATH="$1"
NAME="$2"
API="$3"
ABI="$4"
OUT_DIR=".ci/results/${NAME}"
mkdir -p "${OUT_DIR}"

RESULT_JSON="${OUT_DIR}/result.json"
LOG_FILE="${OUT_DIR}/logcat.txt"

PACKAGE_NAME="${PACKAGE_NAME:-com.example.app}"
MAIN_ACTIVITY="${MAIN_ACTIVITY:-com.example.app.MainActivity}"

# Find device
DEVICE_ID=$(adb devices | awk 'NR>1 && $2=="device" {print $1; exit}')
if [ -z "$DEVICE_ID" ]; then
  echo "No connected device/emulator found" > "${OUT_DIR}/error.txt"
  jq -n --arg m "unknown" --arg mo "unknown" --arg ap "$API" --arg ab "$ABI" --arg ins "no-device" --arg smoke "no-device" '{manufacturer:$m, model:$mo, api_level:$ap, abi:$ab, install_result:$ins, smoke_result:$smoke}' > "$RESULT_JSON"
  exit 2
fi

# collect device info
MANUFACTURER=$(adb -s "$DEVICE_ID" shell getprop ro.product.manufacturer | tr -d '\r')
MODEL=$(adb -s "$DEVICE_ID" shell getprop ro.product.model | tr -d '\r')
API_LEVEL=$(adb -s "$DEVICE_ID" shell getprop ro.build.version.sdk | tr -d '\r')
ABI_PROP=$(adb -s "$DEVICE_ID" shell getprop ro.product.cpu.abi | tr -d '\r')
DENSITY=$(adb -s "$DEVICE_ID" shell wm density | tr -d '\r' || echo "unknown")

INSTALL_RESULT="failure"
SMOKE_RESULT="failure"

if [ -n "$APK_PATH" ] && [ -f "$APK_PATH" ]; then
  adb -s "$DEVICE_ID" install -r "$APK_PATH" > "$OUT_DIR/install.log" 2>&1 || true
  if grep -q "Success" "$OUT_DIR/install.log"; then
    INSTALL_RESULT="success"

    # start main activity
    adb -s "$DEVICE_ID" shell am start -W -n "${PACKAGE_NAME}/${MAIN_ACTIVITY}" > "$OUT_DIR/start.log" 2>&1 || true
    if grep -q "Status: ok" "$OUT_DIR/start.log" || grep -q "Displayed" "$OUT_DIR/start.log"; then
      # check process
      if adb -s "$DEVICE_ID" shell pidof "$PACKAGE_NAME" > /dev/null 2>&1; then
        SMOKE_RESULT="success"
      fi
    fi
  fi
else
  echo "APK not found: ${APK_PATH}" > "$OUT_DIR/install.log"
fi

# collect logcat
adb -s "$DEVICE_ID" logcat -d > "$LOG_FILE" || true

# write json
jq -n --arg manufacturer "$MANUFACTURER" --arg model "$MODEL" --arg api "$API_LEVEL" --arg abi "$ABI_PROP" \
  --arg density "$DENSITY" --arg install "$INSTALL_RESULT" --arg smoke "$SMOKE_RESULT" \
  '{manufacturer:$manufacturer, model:$model, api_level:$api, abi:$abi, density:$density, install_result:$install, smoke_result:$smoke, logs:"'"${LOG_FILE}"'"}' > "$RESULT_JSON"

if [ "$SMOKE_RESULT" = "success" ]; then
  exit 0
else
  exit 3
fi

