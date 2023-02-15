#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html
#
Pod::Spec.new do |s|
  s.name             = 'vungle'
  s.version          = '6.12.2'
  s.summary          = 'Vungle plugin for Flutter apps'
  s.description      = <<-DESC
The Vungle plugin for Flutter applications to enable ad monetization
                       DESC
  s.homepage         = "https://www.vungle.com/"
  s.license          = { :file => '../LICENSE' }
  s.author           = { "Vungle" => "tech-support@vungle.com" }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.public_header_files = 'Classes/**/*.h'
  s.dependency 'Flutter'
  s.dependency 'VungleSDK-iOS'

  s.ios.deployment_target = '9.0'
  
  s.static_framework = true
end

