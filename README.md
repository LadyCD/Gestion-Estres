# Gestion-Estres

Herramienta tecnológica para medir y prevenir el estrés laboral. Práctica de Ingeniería III, Universidad Central, Grupo 2.

La documentación del proyecto (historias de usuario, requerimientos, cronograma y flujo de trabajo) está en la [Wiki](https://github.com/LadyCD/Gestion-Estres/wiki).

## Estructura del repositorio

```
Gestion-Estres/
├── pom.xml              ← backend Spring Boot (IntelliJ, Java 17)
├── src/main/java/com/ucentral/gestionestres/
│   ├── controller/      ← capa de presentación: endpoints REST
│   ├── service/         ← capa de negocio: reglas de las historias de usuario
│   ├── repository/      ← capa de datos: acceso a MySQL con Spring Data JPA
│   ├── model/           ← entidades mapeadas a las tablas
│   ├── dto/             ← datos que entran y salen por la API
│   ├── config/          ← seguridad y Swagger
│   └── exception/       ← manejo de errores en un solo lugar
└── app-movil/           ← app Android en Java (Android Studio)
```

Flujo de una petición: **controller → service → repository → MySQL**. El controller no tiene reglas de negocio y el service no conoce HTTP.

## Cómo ejecutar el backend

1. En MySQL 8 ejecutar, en este orden, `proyecto.sql` y `cambios_bd_HU.sql`. Crean la base `gestion_estres`.
2. Si el usuario de MySQL no es `root` sin contraseña, definir las variables de entorno `DB_USER` y `DB_PASSWORD` (en IntelliJ: *Run → Edit Configurations → Environment variables*).
3. Ejecutar `GestionEstresApplication`.
4. Abrir Swagger en <http://localhost:8080/swagger-ui.html> y probar `GET /api/catalogos/areas`.

Las tablas las crea el script SQL, no Hibernate (`ddl-auto=validate`). Si una entidad no coincide con su tabla, la aplicación no arranca y el error indica la columna. Al cambiar la base, se actualizan el script y la entidad.

Desde el emulador de Android, el backend local se llama con `http://10.0.2.2:8080`.
