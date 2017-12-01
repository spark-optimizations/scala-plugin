clean:
	rm -rf classes/*

compile: clean
	fsc -d classes src/*.scala
	cd classes; cp ../scalac-plugin.xml .; jar -cf ../divbyzero.jar .

run:
	scalac -Xplugin:divbyzero.jar src/Test.scala

debug:
	scalac -Xplugin:divbyzero.jar -Xprint:all src/Test.scala

debug-browse:
	scalac -Xplugin:divbyzero.jar -Ybrowse:all src/Test.scala
