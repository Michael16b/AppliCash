# AppliCash - Maestro E2E Tests

## Prerequisites

- Maestro installed on your machine
- Android emulator or physical device connected via ADB
- AppliCash debug APK installed on the device

## Install the APK

```bash
# From project root
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Run tests from the command line

```bash
# Run a single scenario
maestro test .maestro/01_create_group.yaml

# Run all scenarios in order
maestro test .maestro/00_full_suite.yaml

# Run all files in the folder
maestro test .maestro/
```

## Use Maestro Studio (interactive mode)

Maestro Studio is a browser-based UI that lets you:
- Inspect UI elements and their IDs/text/descriptions
- Write and run Maestro commands interactively
- Debug failing flows step by step

```bash
maestro studio
```

Then open `http://localhost:9999` in your browser.

### How to use Maestro Studio to debug a flow

1. Start `maestro studio` in your terminal
2. Open the browser at `http://localhost:9999`
3. In the **Flow** tab, paste the content of a YAML file to run it live
4. Use the **Inspector** tab to click on UI elements and see their attributes
5. Copy the generated selector (text, id, description) into your YAML

## Scenarios overview

| File | Scenario | Business Rules |
|------|----------|----------------|
| `01_create_group.yaml` | Create a group and add members | RG1, RG2, RG3, RG11, RG12 |
| `02_add_expense_check_balances.yaml` | Add expense and verify balances | RG4, RG10, RG11, RG14 |
| `03_view_reimbursements.yaml` | View suggested reimbursements | RG6, RG7, RG10 |
| `04_edit_delete_expense.yaml` | Edit/delete expense and members | RG5, RG11, RG13 |

## Important: scenario execution order

Scenarios 2, 3 and 4 depend on the state created by the previous scenario.
**Always run in order**: 01 -> 02 -> 03 -> 04.

Use `00_full_suite.yaml` to run them all in the correct order.

## Screenshots

Screenshots taken during test runs are saved in `.maestro/screenshots/`.

## Troubleshooting

### Device not found
```bash
adb devices
# Make sure your device/emulator appears in the list
maestro --device <device-id> test .maestro/01_create_group.yaml
```

### Flow fails on a tap command
Use Maestro Studio inspector to find the correct text/id/description of the UI element.

### App crashes (SubcomposeLayout intrinsic measurement error)
This is a known issue with `ExposedDropdownMenuBox` inside a `Column` with intrinsic width.
The expense screen uses `IntrinsicSize.Min` which conflicts with lazy layouts.
See `feature/expense` screens for the fix.

