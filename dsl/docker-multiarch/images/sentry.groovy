import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/getsentry/docker-sentry') }
				branches('*/master')
				clean()
			}
		}
		triggers {
			upstream("docker-${arch}-python", 'UNSTABLE')
			scm('H H/6 * * *')
		}
		wrappers { colorizeOutput() }
		steps {
			shell(multiarch.templateArgs(meta) + '''
# TODO, should this be done?
rm -r git

sed -i "s!^FROM !FROM $prefix/!" */{,*/}Dockerfile

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v" "$v"
	docker build -t "$repo:$v-onbuild" "$v/onbuild"
	if [ "$v" = "$latest" ]; then
		docker tag -f "$repo:$v" "$repo"
		docker tag -f "$repo:$v-onbuild" "$repo:onbuild"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
