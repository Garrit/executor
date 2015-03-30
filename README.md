Garrit Executor
===============

[![Build Status](https://secure.travis-ci.org/Garrit/executor.svg?branch=master)](https://travis-ci.org/Garrit/executor)

Provides Garrit execution capabilities supported by LXC containerization.

Installation
------------

After checking out the repository, it can be built with
[Maven](http://maven.apache.org/):

```
mvn package
```

This will generate an executable JAR, `./target/executor-1.0.0-SNAPSHOT.jar`.

Usage
-----

Make a copy of
[`config-example.yml`](https://github.com/Garrit/executor/blob/master/config-example.yml):

```
cp config-example.yml config.yml
```

and customize it as necessary:

```
editor config.yml
```

At minimum, you'll need to change the `negotiator` and `problems` properties to
indicate the negotiator endpoint and directory storing problem sets,
respectively.

Then, to launch the executor:

```
java -jar /path/to/executor-1.0.0-SNAPSHOT.jar server /path/to/config.yml
```