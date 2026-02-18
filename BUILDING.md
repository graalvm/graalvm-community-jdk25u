# Building GraalVM Community Edition

To build GraalVM Community Edition, you need to have an OpenJDK 25 build, along with **static libraries** and **jmods**, installed on your system. You can use the [Eclipse Temurin](https://adoptium.net/temurin/releases?version=25) distribution or any other OpenJDK 25 build as long as it ships static libraries and jmods.

If you decide to use the Eclipse Temurin distribution, you can use an API to prepare a JAVA_HOME with all the required libraries and jmods, e.g., with Linux:

```bash
#!/bin/sh

if [[ "`uname -p`" == *aarch64* ]]; then
    export JDK_ARCH=aarch64
else
    export JDK_ARCH=x64
fi

for JDK_VERSION in 25; do
    export JDK_RELEASE=ga
    echo Downloading...
    curl -OJLs https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/${JDK_RELEASE}/linux/${JDK_ARCH}/jdk/hotspot/normal/eclipse
    curl -OJLs https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/${JDK_RELEASE}/linux/${JDK_ARCH}/staticlibs/hotspot/normal/eclipse
    curl -OJLs https://api.adoptium.net/v3/binary/latest/${JDK_VERSION}/${JDK_RELEASE}/linux/${JDK_ARCH}/jmods/hotspot/normal/eclipse
    echo Creating new distro...
    rm -rf jdk-${JDK_VERSION}*
    tar -xf OpenJDK*-jdk*
    export JAVA_HOME=$( pwd )/openjdk-${JDK_VERSION}
    rm -rf "${JAVA_HOME}"
    ln -s $( pwd )/$( echo jdk-${JDK_VERSION}* ) "${JAVA_HOME}"
    if [[ ! -e "${JAVA_HOME}/bin/java" ]]; then
        echo "Cannot find downloaded JDK. Quitting..."
        exit 1
    fi
    tar -xf OpenJDK*-static-libs* --strip-components=1 -C ${JAVA_HOME}
    mkdir -p ${JAVA_HOME}/jmods
    tar -xf OpenJDK*-jmods* --strip-components=1 -C ${JAVA_HOME}/jmods
    rm -rf OpenJDK*
    echo Done.
done
```

After you have the required OpenJDK 25 build, follow these steps to build GraalVM Community Edition:

```bash
export JAVA_HOME=/path/to/your/jdk25
git clone https://github.com/graalvm/graalvm-community-jdk25u graalvm-jdk25
export MX_VERSION=$(jq -r .mx_version graalvm-jdk25/common.json)
git clone https://github.com/graalvm/mx.git mx --branch $MX_VERSION
cd graalvm-jdk25
../mx/mx --primary-suite vm --env ce build
```

Once the build completes successfully, you can find the GraalVM Community Edition distribution in the directory where the `sdk/latest_graalvm_home` link points to.

