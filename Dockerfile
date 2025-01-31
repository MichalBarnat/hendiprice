# Używamy obrazu Javy 17
FROM openjdk:17-jdk-slim

# Ustawiamy katalog roboczy
WORKDIR /app

# Kopiujemy plik JAR (za chwilę go wygenerujemy)
COPY target/*.jar app.jar

# Ustawiamy port aplikacji (8080)
EXPOSE 8080

# Uruchamiamy aplikację
ENTRYPOINT ["java", "-jar", "app.jar"]
