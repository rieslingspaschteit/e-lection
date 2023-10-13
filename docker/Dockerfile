FROM cypress/browsers:node-18.14.1-chrome-110.0.5481.96-1-ff-109.0-edge-110.0.1587.41-1

RUN apt-get update && \
  apt-get install curl ca-certificates -y

RUN curl -LO https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_linux-x64_bin.tar.gz && \
    tar -xvf openjdk-17.0.2_linux-x64_bin.tar.gz && \
    mv jdk-17.0.2 /usr/local && \
    rm openjdk-17.0.2_linux-x64_bin.tar.gz

ENV JAVA_HOME=/usr/local/jdk-17.0.2
ENV PATH=$JAVA_HOME/bin:$PATH

