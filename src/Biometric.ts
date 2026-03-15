export const FINGERPRINT = 1
export const FACIAL_RECOGNITION = 2
export const IRIS = 3

export type AuthenticationType = typeof FINGERPRINT | typeof FACIAL_RECOGNITION | typeof IRIS

export type AuthenticateOptions = {
  promptMessage?: string
  cancelLabel?: string
  disableDeviceFallback?: boolean
}

export type AuthenticateResult =
  | { success: true }
  | { success: false; error: string }

function getMod() {
  return typeof NativeModules !== 'undefined' ? NativeModules?.BiometricModule : null
}

export async function hasHardwareAsync(): Promise<boolean> {
  const mod = getMod()
  if (!mod?.hasHardwareAsync) return false
  return new Promise((resolve) => {
    mod.hasHardwareAsync((json: string) => {
      try {
        const r = JSON.parse(json)
        resolve(r.value === true)
      } catch {
        resolve(false)
      }
    })
  })
}

export async function isEnrolledAsync(): Promise<boolean> {
  const mod = getMod()
  if (!mod?.isEnrolledAsync) return false
  return new Promise((resolve) => {
    mod.isEnrolledAsync((json: string) => {
      try {
        const r = JSON.parse(json)
        resolve(r.value === true)
      } catch {
        resolve(false)
      }
    })
  })
}

export async function authenticateAsync(options: AuthenticateOptions = {}): Promise<AuthenticateResult> {
  const mod = getMod()
  if (!mod?.authenticateAsync) return { success: false, error: 'not_available' }
  const optionsJson = JSON.stringify({
    promptMessage: options.promptMessage ?? 'Authenticate',
    cancelLabel: options.cancelLabel ?? 'Cancel',
    disableDeviceFallback: options.disableDeviceFallback ?? false,
  })
  return new Promise((resolve) => {
    mod.authenticateAsync(optionsJson, (json: string) => {
      try {
        const r = JSON.parse(json)
        if (r.success) resolve({ success: true })
        else resolve({ success: false, error: r.error ?? 'unknown' })
      } catch {
        resolve({ success: false, error: 'unknown' })
      }
    })
  })
}

export async function supportedAuthenticationTypesAsync(): Promise<AuthenticationType[]> {
  const mod = getMod()
  if (!mod?.supportedAuthenticationTypesAsync) return []
  return new Promise((resolve) => {
    mod.supportedAuthenticationTypesAsync((json: string) => {
      try {
        const r = JSON.parse(json)
        resolve(Array.isArray(r.types) ? r.types : [])
      } catch {
        resolve([])
      }
    })
  })
}
