Pod::Spec.new do |s|
	s.name = "DefaultPlayer"
	s.version = "1.5.0"
	s.platform = :tvos, :ios
	s.swift_versions = ['5.0', '5.1']
	s.summary = "ZappPlugins"
	s.description = "Zapp Plugins store Protocol and Managers that relevan for Applicaster Zapp Plugin System"
	s.homepage = "https://applicaster.com"
	s.license = ''
	s.author = "Applicaster LTD."
	s.source = {
		 :git => 'git@github.com:applicaster/DefaultPlayer-iOS.git',
		 :tag => s.version.to_s
  }
	s.dependency 'React'
	s.dependency 'ZappCore'

	s.xcconfig = {
		 'ENABLE_BITCODE' => 'YES',
		 'ENABLE_TESTABILITY' => 'YES',
		 'OTHER_CFLAGS'  => '-fembed-bitcode',
		}

	 s.tvos.deployment_target = "10.0"
	 s.ios.deployment_target = "10.0"

	 s.source_files  = [
		'DefaultPlayer/**/*.{swift}',
		'DefaultPlayer/Helpers/ReactNative/ReactNativeModulesExports.m'
	 ]
	 s.exclude_files = [

	 ]

end
