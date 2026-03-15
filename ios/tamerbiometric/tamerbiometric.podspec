package = JSON.parse(File.read(File.join(__dir__, "..", "..", "package.json")))

Pod::Spec.new do |s|
  s.name             = 'tamerbiometric'
  s.version          = package["version"]
  s.summary          = 'Biometric authentication for Lynx.'
  s.description      = package["description"]
  s.homepage         = "https://github.com/nanofuxion"
  s.license          = package["license"]
  s.authors          = package["author"]
  s.source           = { :path => '.' }
  s.ios.deployment_target = '13.0'
  s.swift_version    = '5.0'
  s.source_files     = 'tamerbiometric/Classes/**/*.swift'
  s.frameworks       = 'LocalAuthentication'
  s.dependency "Lynx"
end
