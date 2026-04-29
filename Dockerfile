FROM eclipse-temurin:21-jdk as builder
WORKDIR /usr/local
RUN apt-get update && apt-get install -y ant unzip
RUN curl --location 'https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.117/bin/apache-tomcat-9.0.117.tar.gz' | tar xz
RUN cd apache-tomcat* && echo "catalina.home=$(pwd)" > ~/build.properties

COPY . /tmp/joai-project
WORKDIR /tmp/joai-project
RUN ant dist
RUN mkdir /war
WORKDIR /war
RUN unzip /tmp/joai-project/dist/oai.war


# stage 2
FROM tomcat:9.0.117-jdk21-corretto
LABEL author="Tom Saleeba"

WORKDIR /usr/local/tomcat/webapps
COPY --from=builder /war/ ./ROOT/
RUN \
  curl -L -o ../lib/woodstox-core-5.0.3.jar 'https://search.maven.org/remotecontent?filepath=com/fasterxml/woodstox/woodstox-core/5.0.3/woodstox-core-5.0.3.jar' && \
  curl -L -o ../lib/stax2-api-4.0.0.jar 'https://search.maven.org/remotecontent?filepath=org/codehaus/woodstox/stax2-api/4.0.0/stax2-api-4.0.0.jar' && \
  mkdir -p /joai/config/harvester /joai/config/repository && \
  ln -s /joai/config/harvester /usr/local/tomcat/webapps/ROOT/WEB-INF/harvester_settings_and_data && \
  ln -s /joai/config/repository /usr/local/tomcat/webapps/ROOT/WEB-INF/repository_settings_and_data

# just the config
VOLUME /joai/config

# the harvested/provided records
VOLUME /joai/data
