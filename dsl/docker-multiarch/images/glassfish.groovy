import vars.multiarch

for (arch in multiarch.allArches()) {
	meta = multiarch.meta(getClass(), arch)

	freeStyleJob(meta.name) {
		description(meta.description)
		logRotator { daysToKeep(30) }
		label(meta.label)
		scm {
			git {
				remote { url('https://github.com/aws/aws-eb-glassfish-dockerfiles') }
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
			shell(multiarch.templateArgs(meta, ['dpkgArch']) + '''
sed -i "s!^FROM        !FROM        $prefix/!" */Dockerfile
sed -i "s!amd64!$dpkgArch!g" */Dockerfile

latest="$(./generate-stackbrew-library.sh | awk '$1 == "latest:" { print $3; exit }')"

for v in */; do
	v="${v%/}"
	docker build -t "$repo:$v" "$v"
	if [ "$v" = "$latest" ]; then
		docker tag -f "$repo:$v" "$repo"
	fi
done
''' + multiarch.templatePush(meta))
		}
	}
}
