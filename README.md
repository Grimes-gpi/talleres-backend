# Backend REST

API Spring Boot independiente para Talleres Comunitarios. Requiere JDK 25 y PostgreSQL. No incluye H2.

Prepara las tablas y los datos desde pgAdmin con `../database/talleres_comunitarios.sql`. Configura `.env.local` usando `.env.example` como guía e inicia con `./run-backend.ps1` desde PowerShell. Por defecto usa `localhost:5432/talleres_comunitarios` y expone REST en el puerto 8081. Las pruebas se ejecutan con `./test-backend.ps1` y usan la base independiente `talleres_comunitarios_test`.
