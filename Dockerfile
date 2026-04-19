FROM maven:3.9-eclipse-temurin-11

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
COPY src/test/resources /src/test/resources

ENV WEBDRIVER_URL="http://chrome:4444/wd/hub"

CMD mvn test -Dwebdriver.url=http://chrome:4444/wd/hub -Dtest.email=${TEST_EMAIL} -Dtest.password=${TEST_PASSWORD}