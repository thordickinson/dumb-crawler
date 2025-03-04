# Etapa 1: Compilación
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración y dependencias primero para aprovechar el cacheo de Docker
COPY pom.xml ./
RUN mvn dependency:go-offline

# Copiar el resto del código fuente
COPY src ./src

# Compilar el proyecto
RUN mvn clean package -DskipTests

# Etapa 2: Imagen final sin código fuente
FROM eclipse-temurin:21-jdk-jammy

# Establecer el directorio de trabajo en la imagen final
WORKDIR /app

# Copiar el archivo JAR desde la etapa de compilación
COPY --from=build /app/target/crawler.jar crawler.jar

# Comando por defecto para ejecutar la aplicación
CMD ["java", "-jar", "crawler.jar"]
