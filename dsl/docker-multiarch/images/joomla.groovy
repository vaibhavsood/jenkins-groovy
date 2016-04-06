import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/joomla/docker-joomla') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-php", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta) + '''
sed -i "s!^FROM !FROM $prefix/!" */Dockerfile

docker build -t "$repo:apache" apache
docker build -t "$repo:apache-php7" apache-php7
docker build -t "$repo:fpm" fpm
docker build -t "$repo:fpm-php7" fpm-php7
docker tag -f "$repo:apache" "$repo"
''' + multiarch.templatePush(meta))
		}
	}
}
