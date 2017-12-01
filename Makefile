OUT_ROOT=out
CLASSES_PATH=${OUT_ROOT}/classes
ARTIFACTS_PATH=${OUT_ROOT}/artifacts

JAR_NAME=${ARTIFACTS_PATH}/build-plugin.jar
RESOURCES_PATH=src/main/resources
LIB_PATH=lib

all: build run

build:
	fsc -d ${CLASSES_PATH} \
		-cp "./${LIB_PATH}/*" \
		src/main/scala/org/so/plugin/*.scala
	cp ${RESOURCES_PATH}/* ${CLASSES_PATH}
	jar cf ${JAR_NAME} \
    		-C ${CLASSES_PATH} .

run:
	scalac -Xprint:all \
		-d ${CLASSES_PATH} \
		-Xplugin:${JAR_NAME} \
		src/test/scala/org/so/plugin/DivByZeroTest.scala

setup: clean
	mkdir -p ${CLASSES_PATH}
	mkdir -p ${ARTIFACTS_PATH}

clean:
	rm -rf ${OUT_ROOT}