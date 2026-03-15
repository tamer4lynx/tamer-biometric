import Foundation
import Lynx
import LocalAuthentication

@objcMembers
public final class BiometricModule: NSObject, LynxModule {

    private static let FINGERPRINT = 1
    private static let FACIAL_RECOGNITION = 2
    private static let IRIS = 3

    @objc public static var name: String { "BiometricModule" }

    @objc public static var methodLookup: [String: String] {
        [
            "hasHardwareAsync": NSStringFromSelector(#selector(hasHardwareAsync(_:))),
            "isEnrolledAsync": NSStringFromSelector(#selector(isEnrolledAsync(_:))),
            "authenticateAsync": NSStringFromSelector(#selector(authenticateAsync(_:callback:))),
            "supportedAuthenticationTypesAsync": NSStringFromSelector(#selector(supportedAuthenticationTypesAsync(_:)))
        ]
    }

    @objc public init(param: Any) { super.init() }
    @objc public override init() { super.init() }

    @objc func hasHardwareAsync(_ callback: @escaping (String) -> Void) {
        let context = LAContext()
        var error: NSError?
        let canEvaluate = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)
        let hasHardware = canEvaluate || (error?.code == LAError.biometryNotEnrolled.rawValue)
        callback(createJSON(["value": hasHardware]))
    }

    @objc func isEnrolledAsync(_ callback: @escaping (String) -> Void) {
        let context = LAContext()
        var error: NSError?
        let enrolled = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) && error == nil
        callback(createJSON(["value": enrolled]))
    }

    @objc func authenticateAsync(_ optionsJson: String, callback: @escaping (String) -> Void) {
        let options = parseOptions(optionsJson)
        let reason = options["promptMessage"] as? String ?? "Authenticate"

        let context = LAContext()
        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, error in
            if success {
                callback(self.createJSON(["success": true]))
            } else if let laError = error as? LAError {
                let errorCode = self.mapLAError(laError)
                callback(self.createJSON(["success": false, "error": errorCode]))
            } else {
                callback(self.createJSON(["success": false, "error": "unknown"]))
            }
        }
    }

    @objc func supportedAuthenticationTypesAsync(_ callback: @escaping (String) -> Void) {
        let context = LAContext()
        var error: NSError?
        _ = context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error)

        var types: [Int] = []
        switch context.biometryType {
        case .faceID:
            types.append(BiometricModule.FACIAL_RECOGNITION)
        case .touchID:
            types.append(BiometricModule.FINGERPRINT)
        case .opticID:
            types.append(BiometricModule.IRIS)
        case .none:
            break
        @unknown default:
            break
        }
        callback(createJSON(["types": types]))
    }

    private func parseOptions(_ json: String) -> [String: Any] {
        guard let data = json.data(using: .utf8),
              let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return [:]
        }
        return dict ?? [:]
    }

    private func mapLAError(_ error: LAError) -> String {
        switch error.code {
        case .userCancel, .userFallback:
            return "user_cancel"
        case .biometryNotEnrolled:
            return "not_enrolled"
        case .biometryNotAvailable, .biometryNotPaired:
            return "not_available"
        case .biometryLockout:
            return "lockout"
        case .authenticationFailed:
            return "authentication_failed"
        case .invalidContext:
            return "invalid_context"
        case .notInteractive:
            return "not_interactive"
        case .passcodeNotSet:
            return "passcode_not_set"
        case .systemCancel:
            return "user_cancel"
        default:
            return "unknown"
        }
    }

    private func createJSON(_ dict: [String: Any]) -> String {
        (try? JSONSerialization.data(withJSONObject: dict)).flatMap { String(data: $0, encoding: .utf8) } ?? "{}"
    }
}
