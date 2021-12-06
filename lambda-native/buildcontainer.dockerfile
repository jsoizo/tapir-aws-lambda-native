FROM hseeberger/scala-sbt:graalvm-ce-21.3.0-java11_1.5.5_3.1.0

RUN microdnf install gcc glibc-devel zlib-devel libstdc++-static && gu install native-image