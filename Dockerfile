FROM maven:3.9.9-eclipse-temurin-21

ARG TEST_PROFILE=api

ENV TEST_PROFILE=${TEST_PROFILE}
ENV APIBASEURL=http://backend:4111
ENV UIBASEURL=http://nginx
ENV SELENOID_URL=http://selenoid:4444

WORKDIR /app

COPY pom.xml .

RUN mvn --batch-mode dependency:go-offline

COPY . .

CMD ["/bin/bash", "-c", "\
mkdir -p /app/logs; \
echo \">>> Running tests with profile: ${TEST_PROFILE}\" | tee /app/logs/run.log; \
mvn --batch-mode test -P${TEST_PROFILE} 2>&1 | tee -a /app/logs/run.log; \
TEST_EXIT_CODE=${PIPESTATUS[0]}; \
echo \">>> Running surefire-report:report\" | tee -a /app/logs/run.log; \
mvn --batch-mode -DskipTests=true surefire-report:report 2>&1 | tee -a /app/logs/run.log; \
exit ${TEST_EXIT_CODE}"]