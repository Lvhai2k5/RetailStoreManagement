# ==========================================
# STAGE 1: BUILD (Đóng gói ứng dụng)
# ==========================================
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# Copy toàn bộ source code vào trong container
COPY . .

# Chạy lệnh build bỏ qua test
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: RUN (Chạy ứng dụng)
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy file .jar từ Stage 1 sang Stage 2 và đổi tên cho gọn
COPY --from=build /app/target/RetailStoreManagement_Team-0.0.1-SNAPSHOT.jar app.jar

# Mở cổng 8080 (cổng mặc định của Spring Boot)
EXPOSE 8080

# Lệnh khởi động server
ENTRYPOINT ["java", "-jar", "app.jar"]
