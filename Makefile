all: build run

build:
	mkdir -p classes
	fsc -d classes src/main/scala/org/so/plugin/DivByZero.scala
	cp scalac-plugin.xml classes
	(cd classes; jar cf ../divbyzero.jar .)

run:
	scalac -Xprint:all -Xplugin:divbyzero.jar src/test/scala/org/so/plugin/DivByZeroTest.scala