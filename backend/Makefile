## @author Manuel Gieseking

# All repositories the backend depends on
DEPENDENCIES_FOLDERS="libs,examples,framework,logics,modelchecker,synthesizer,synthesisDistrEnv,high-level,webinterface-backend"
#TARGETS=tools petrinetwithtransits logics mc petrigames bounded symbolic

t=javac

# should be executed no matter if a file with the same name exists or not
.PHONY: tools
		petrinetwithtransits
		logics
		mc
		petrigames
		symbolic
		bdd
		mtbdd
		highlevel
		synthesisDistrEnv
		backend
#		javadoc
		setClean
		setCleanAll
		setJavac
		setJar
		setDeploy
		clean
		clean-all
		pull_backend
		update_backend
		update_examples
		clean_backend
		deploy_backend
		publish_backend

## the targets for the submodules
tools: 
	ant -buildfile ./framework/tools/build.xml $(t)

petrinetwithtransits: tools
	ant -buildfile ./framework/petrinetWithTransits/build.xml $(t)

logics: petrinetwithtransits
	ant -buildfile ./logics/build.xml $(t)

mc: logics
	ant -buildfile ./modelchecker/build.xml $(t)

petrigames: petrinetwithtransits
	ant -buildfile ./synthesizer/petriGames/build.xml $(t)

bdd: petrigames
	ant -buildfile ./synthesizer/symbolicalgorithms/bddapproach/build.xml $(t)

mtbdd: petrigames
	ant -buildfile ./synthesizer/symbolicalgorithms/mtbddapproach/build.xml $(t)

symbolic: bdd mtbdd

synthesisDistrEnv: bdd
	ant -buildfile ./synthesisDistrEnv/build.xml $(t)

highlevel: petrigames
	ant -buildfile ./high-level/build.xml $(t)

backend: mc symbolic synthesisDistrEnv highlevel
	ant -buildfile ./webinterface-backend/build.xml $(t)

setJavac:
	$(eval t=javac)

setJar:
	$(eval t=jar)

setClean:
	$(eval t=clean)

setCleanAll:
	$(eval t=clean-all)

setDeploy:
	$(eval t=deploy)

clean: setClean backend

clean-all: setCleanAll backend

## targets for main project
pull_backend: 
	./pull_backend_submodules.sh ${DEPENDENCIES_FOLDERS}

#update_backend: pull_backend
#	./update_backend.sh ${DEPENDENCIES_FOLDERS}

clean_backend: clean-all

deploy_backend: setJavac mc symbolic synthesisDistrEnv highlevel setDeploy backend

integrate_backend: clean_backend 	
	./integrate_backend.sh

publish_backend: clean_backend pull_backend
	git pull
	./update_backend.sh ${DEPENDENCIES_FOLDERS}
	./publish_backend.sh

update_examples:
	./update_examples.sh

print_current_backend_version:
	@echo -n "Version 'adam_core.jar': "
	@grep -zoP "<artifactId>adam-core</artifactId>[[:space:]]*<version>\K[0-9]+\.[0-9]+(?=</version>)" ../server/pom.xml | tr '\0' '\n'
