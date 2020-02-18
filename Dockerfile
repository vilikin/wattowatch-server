FROM openjdk:8-jdk

# copy files into working directory
COPY . .

# required to check that db is running
RUN apt-get update
RUN apt-get install -y netcat

# builds all the necessary jars
RUN ./gradlew stage
