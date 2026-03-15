declare var NativeModules: {
  BiometricModule?: {
    hasHardwareAsync(callback: (json: string) => void): void
    isEnrolledAsync(callback: (json: string) => void): void
    authenticateAsync(optionsJson: string, callback: (json: string) => void): void
    supportedAuthenticationTypesAsync(callback: (json: string) => void): void
  }
}
