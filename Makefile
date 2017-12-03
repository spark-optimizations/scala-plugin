OUT_ROOT=out
CLASSES_PATH=${OUT_ROOT}/classes
ARTIFACTS_PATH=${OUT_ROOT}/artifacts

JAR_NAME=${ARTIFACTS_PATH}/build-plugin.jar
RESOURCES_PATH=src/main/resources
LIB_PATH=lib

TEST_LIB_PATH=lib-min

#TEST_FILE=src/test/scala/org/so/plugin/DivByZeroTest.scala
TEST_FILE=src/test/scala/org/so/plugin/BadMapSparkTest.scala

all: build run

build: setup
	@fsc -d ${CLASSES_PATH} \
		-cp "./${LIB_PATH}/*" \
		src/main/scala/org/so/plugin/**/*.scala \
		src/main/scala/org/so/plugin/*.scala
	@cp ${RESOURCES_PATH}/* ${CLASSES_PATH}
	@jar cf ${JAR_NAME} \
    		-C ${CLASSES_PATH} .

run: build
	@scalac \
		-cp "./${TEST_LIB_PATH}/*" \
		-d ${CLASSES_PATH} \
		-Xplugin:${JAR_NAME} \
		${TEST_FILE} \
		#-Xprint:all

debug-browse: build
	@scalac \
		-cp "./${TEST_LIB_PATH}/*" \
		-d ${CLASSES_PATH} \
		-Xplugin:${JAR_NAME} \
		-Ybrowse:all ${TEST_FILE}

setup: clean
	@mkdir -p ${CLASSES_PATH}
	@mkdir -p ${ARTIFACTS_PATH}

clean:
	@rm -rf ${OUT_ROOT}
