# ErgoPool Proxy
This project is a proxy server for [ergopool.io](https://ergopool.io)
## Setup
### Prerequisite
#### ErgoNode
To use the proxy, node [v3.2.7](https://github.com/ergoplatform/ergo/releases/tag/v3.2.7) or above is needed.
#### OpenJDK 11
[Here](https://jdk.java.net/java-se-ri/11), binaries for OpenJDK 11 exists that depend on your OS, you can install one.
#### sbt 1.2.8
Depend on your OS, you can follow instructions in [this](https://www.scala-sbt.org/1.0/docs/Setup.html) page to install sbt
#### Appkit library (develop - 60478389921a2aabe4d79f223f54a6bd12a09e4c)
Appkit require GraalVM (Community or Enterprise edition) to be
[downloaded](https://www.graalvm.org/downloads/) and installed. Community
edition should be enough for Ergo Appkit library.

#### Install GraalVM Community Edition on MacOS

First you need to download an archive with the [latest
release](https://github.com/oracle/graal/releases) of GraalVM (e.g.
`graalvm-ce-java8-linux-amd64-19.3.1.tar.gz`) for Linux and put the programs from it
onto the `$PATH`.

```shell script
cd <your/directory/with/downloaded/graal>
tar -zxf graalvm-ce-java8-linux-amd64-19.3.1.tar.gz
export GRAAL_HOME=<your/directory/with/downloaded/graal>/graalvm-ce-java8-19.3.1
export PATH=$PATH:${GRAAL_HOME}/bin
```
For mac, you can view Appkit [README](https://github.com/aslesarenko/ergo-appkit/tree/60478389921a2aabe4d79f223f54a6bd12a09e4c)
Now it's time to build Appkit:
```shell script
git clone https://github.com/aslesarenko/ergo-appkit.git -b develop
cd ergo-appkit
git checkout 60478389921a2aabe4d79f223f54a6bd12a09e4c
JAVA_HOME=$GRAAL_HOME sbt publishLocal
```
## Installation
Now you have to clone and build this project:
```shell script
git clone http://github.com/ergopool-io/proxy.git
cd ergo-proxy
sbt assembly
```
Now you should set your config. An example is available in `/config/application.conf`.
Copy that into a new file and set proper settings for all keys.  
After creating the config file, you can run your proxy:  
* For Windows:
```shell script
java -D"config.file"=path\to\config -jar ergo-proxy-assembly-<PROXY_VERSION>.jar
```
* For Unix:
```shell script
java -Dconfig.file=path/to/config -jar ergo-proxy-assembly-<PROXY_VERSION>.jar
```
**NOTE**: After the startup, you should save the mnemonic with your desired password in proxy’s dashboard or swagger panel (/proxy/mnemonic/save). After that, every time you run the proxy, you need to load the mnemonic with the previously used password in the swagger panel.

**Otherwise**, your collateral address will change every time you start the proxy and proxy will have to move your ergs to the new collateral address which will result in a few minutes delay and some other problems.

**So, we strongly recommend you to save the mnemonic!**

## Docker Quick Start
To use [dockerized proxy](https://hub.docker.com/r/ergopoolio/proxy), create your desired config file and an empty folder to be used by container and run the proxy:
```shell
$ mkdir /empty/folder/path
$ chown -R 9052:9052 /empty/folder/path
$ docker run -p 9000:9000 \
  --restart=always \
  -v /desired/path/configFile.conf:/home/ergo/proxy.conf \
  -v /empty/folder/path/:/home/ergo/proxy/ \
  -d ergopoolio/proxy:latest

```
You can use 9000 port to load the proxy's panel.
