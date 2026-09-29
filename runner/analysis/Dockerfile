FROM docker.io/library/eclipse-temurin@sha256:87f0a5e638d0bc739f2908adb4a0a697b585d8bee5e5a8de43c88e8d07b3786e
COPY target/array-analysis.jar /opt/analysis.jar
COPY target/test-classes/ /opt/tests/
USER 10001:10001
ENTRYPOINT ["java", "-Xmx256m", "-Xss2m", "-XX:ActiveProcessorCount=1", "-XX:-UsePerfData", "-cp", "/opt/analysis.jar:/opt/tests", "dev.codeviz.analysis.AnalyzerAcceptance"]
