# 로컬 부하 테스트와 Railway 배포가 같은 이미지를 쓰도록 만든 Dockerfile.
# 레포 루트에 Dockerfile이 있으면 Railway는 Railpack 대신 이걸로 빌드한다.

# ---- 빌드 ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# 의존성 레이어를 먼저 받아 두면 소스만 바뀌었을 때 다시 받지 않는다.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ---- 실행 ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar

# 512MB 컨테이너에서 기본값(힙 65%)으로 띄우면 기동 중에 힙 바깥 메모리까지 합쳐 한도를 넘어 OOMKilled 된다.
# 그래서 힙은 절반만 쓰고, 힙 바깥(메타스페이스·JIT 코드 캐시·스레드 스택)에도 상한을 건다.
# - SerialGC: CPU 1개 환경에선 G1보다 부가 메모리가 적다
# - Xss512k: Tomcat 스레드가 많아져도 스택 메모리가 덜 든다
# OOM이 나면 좀비로 남지 않고 바로 죽어서 Railway가 재시작하게 한다.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 \
    -XX:MaxMetaspaceSize=160m \
    -XX:ReservedCodeCacheSize=64m \
    -XX:MaxDirectMemorySize=32m \
    -Xss512k \
    -XX:+UseSerialGC \
    -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
