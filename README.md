# tamer-biometric

Biometric authentication (fingerprint, Face ID) for Lynx. Standalone module for triggering Face ID / fingerprint prompts.

## Installation

```bash
npm install @tamer4lynx/tamer-biometric
```

Add to your app's dependencies and run `t4l link`. Uses **lynx.ext.json** (RFC standard).

## Usage

```ts
import {
  hasHardwareAsync,
  isEnrolledAsync,
  authenticateAsync,
  supportedAuthenticationTypesAsync,
  FINGERPRINT,
  FACIAL_RECOGNITION,
  IRIS,
} from '@tamer4lynx/tamer-biometric'

const hasHardware = await hasHardwareAsync()
const isEnrolled = await isEnrolledAsync()
const types = await supportedAuthenticationTypesAsync()

const result = await authenticateAsync({
  promptMessage: 'Authenticate to continue',
  cancelLabel: 'Cancel',
  disableDeviceFallback: false,
})
if (result.success) {
  // User authenticated
} else {
  console.log(result.error) // e.g. 'user_cancel', 'not_enrolled'
}
```

## API

| Method | Returns | Description |
|--------|---------|-------------|
| `hasHardwareAsync()` | `Promise<boolean>` | Whether biometric hardware is available |
| `isEnrolledAsync()` | `Promise<boolean>` | Whether biometrics are enrolled |
| `authenticateAsync(options?)` | `Promise<AuthenticateResult>` | Run biometric auth prompt |
| `supportedAuthenticationTypesAsync()` | `Promise<AuthenticationType[]>` | FINGERPRINT (1), FACIAL_RECOGNITION (2), IRIS (3) |

**AuthenticateOptions**: `promptMessage`, `cancelLabel`, `disableDeviceFallback` (use PIN/pattern on Android)

**AuthenticateResult**: `{ success: true }` or `{ success: false, error: string }` (e.g. `user_cancel`, `not_enrolled`, `not_available`)

## iOS: Face ID

Add to your app's `Info.plist`:

```xml
<key>NSFaceIDUsageDescription</key>
<string>Authenticate to access this feature</string>
```

Without this key, Face ID authentication will fail.

## Android

Uses `androidx.biometric`. No additional permissions required for Android 9+ (USE_BIOMETRIC is implicit).
