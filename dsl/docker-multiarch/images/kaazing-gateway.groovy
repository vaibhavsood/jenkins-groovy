import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/kaazing/gateway.docker') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-openjdk", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta) + '''
sed -i "s!^FROM !FROM $prefix/!" Dockerfile

docker build -t "$repo" . 
version="$(awk -F ' ' '$1 == "ENV" && $2 == "KAAZING_GATEWAY_VERSION" { print $3; exit }' Dockerfile)"
docker tag -f "$repo" "$repo:$version"
''' + multiarch.templatePush(meta))
		}
	}
}
