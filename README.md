# chronostream

A small Java service that runs one or more computations and returns the stream of results to a simple
javascript front end.

This code was initially written to test various HSMs for correctness and performance. The purpose of open sourcing
the code is to allow anyone to look at our methodology and improve upon it.

In theory, you should be able to re-use this code for arbitrary benchmarking. You might also want to checkout this
Stack Overflow answer related to Java benchmark frameworks: http://stackoverflow.com/a/7445378/2217509

Obligatory reading: https://zedshaw.com/archive/programmers-need-to-learn-statistics-or-i-will-kill-them-all/

# running

    mvn package
    java -cp bcprov-jdk15on-1.51.jar:target/chronostream-1.0-SNAPSHOT.jar chronostream.App server src/main/resources/dev.yaml

and then open [http://localhost:8080/index](http://localhost:8080/index)
