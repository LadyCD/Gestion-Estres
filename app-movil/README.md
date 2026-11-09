# app-movil

App Android nativa en Java para el prototipo del 40 % (HU001, HU020, HU021, HU002 y HU026).
El backend está en la raíz del repositorio (Spring Boot).

## Abrir el proyecto

1. Android Studio → **File → Open** → seleccionar esta carpeta (`app-movil`).
2. Esperar la sincronización de Gradle. La primera vez descarga Gradle 8.13 y las librerías.
   Si Android Studio ofrece actualizar el plugin de Android (AGP), aceptar.
3. Si pide instalar el SDK de Android 16 (API 36), aceptar.
4. Crear un emulador con **API 34 o superior y Google Play** (Health Connect ya viene integrado).

| Dato | Valor |
|---|---|
| Paquete | `com.ucentral.gestionestres.app` |
| SDK mínimo | API 28 (Android 9), mínimo de Health Connect (HU008) |
| SDK objetivo | API 36 |
| Lenguaje | Java 17. Único archivo Kotlin: `salud/SaludConexion.kt`, el puente con Health Connect (su API es de corrutinas y no se puede llamar desde Java) |

## Conectar con el backend

- Desde el emulador, el backend local es `http://10.0.2.2:8080/` (ya configurado en `app/build.gradle`, `API_URL`).
- En un teléfono físico, cambiar `API_URL` por la IP del computador en la misma red Wi-Fi
  y agregar esa IP en `res/xml/network_security_config.xml`.

## Datos simulados (Health Connect Toolbox)

1. Instalar **Health Connect Toolbox** en el emulador.
2. Insertar registros de frecuencia cardíaca (y HRV RMSSD, opcional) en las últimas 24 horas.
3. En la app: Mi dispositivo → Vincular dispositivo → conceder permisos.

## Estructura

```
app/src/main/java/com/ucentral/gestionestres/app/
├── api/        Retrofit: ApiServicio (endpoints), Dtos, ApiCliente (token), Errores, Fechas
├── sesion/     Sesion: token, rol, inactividad de 15 min (HU020) y credencial del dispositivo
├── salud/      SaludConexion.kt (Health Connect) y SincronizacionWorker (HU026, cada 15 min)
└── ui/         Login (HU020), Registro (HU001), Inicio, Chequeo (HU021), Dispositivo (HU002)
```

## Contrato de la API

La app espera estos endpoints. Errores en el formato `ErrorRespuesta {fecha, estado, mensaje, campos}`.
Fechas en texto ISO-8601, hora de Colombia, sin zona: `2026-11-09T08:15:00`.

| HU | Método y ruta | Cuerpo | Respuesta |
|---|---|---|---|
| HU001 | `GET /api/catalogos/areas` y `/cargos` (públicos) | — | `[{id, nombre}]` |
| HU001 | `POST /api/usuarios` (público) | `{nombreCompleto, correo, contrasena, cargoId, areaId, versionConsentimiento}` | `201 {id, nombreCompleto, correo, rol}` · `409` correo duplicado · `400` validación (incluye dominio no permitido) |
| HU020 | `POST /api/auth/login` (público) | `{correo, contrasena}` | `200 {token, rol, nombreCompleto, expiraEnMinutos}` · `401` mensaje genérico · `423` cuenta bloqueada 15 min |
| HU020 | `POST /api/auth/logout` | — | `204` |
| HU021 | `POST /api/cuestionarios/diario` | `{estres 0-10, energia 1-5, tension}` | `201` |
| HU002 | `GET /api/dispositivo` | — | `200 {estado, hrvDisponible, ultimaSincronizacion}` |
| HU002 | `PUT /api/dispositivo` | `{estado: VINCULADO \| DESCONECTADO, hrvDisponible}` | `200 {estado, hrvDisponible, ultimaSincronizacion, tokenDispositivo}` · `409` sin consentimiento vigente. `tokenDispositivo` solo llega al vincular |
| HU026 | `POST /api/ventanas` (encabezado `X-Token-Dispositivo`, sin sesión) | `{inicio, fin, fcPromedio, hrvPromedio \| null, numLecturas}` | `201` · `409` ventana repetida (misma hora de inicio) · `401` token inválido |
| HU025 | `GET /api/linea-base` | — | `200 {estado: EN_CALCULO \| LISTA, diasValidos, diasRequeridos}` |
| HU003 | `GET /api/detecciones/ultima` | — | `200 {fechaHora, nivel: BAJO \| MODERADO \| ALTO, parcial}` · `204` sin detección |

Todas las rutas no públicas usan `Authorization: Bearer <token>`, excepto `POST /api/ventanas`.

**Por qué hay un token del dispositivo:** la sesión del usuario vence a los 15 minutos de inactividad (HU020),
pero la sincronización (HU026) corre en segundo plano con la app cerrada. Por eso, al vincular, el backend
entrega un token que solo sirve para enviar ventanas. Guarda solo su hash (columna `token_hash`
de `dispositivo_vinculado`) y lo anula al desvincular.
