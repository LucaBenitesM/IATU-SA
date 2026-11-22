# IATU-SA

## Proyecto TIC 2026 · Plataforma turística para Uruguay Mnatural

IATU-SA es el emprendimiento de software del equipo en el marco del Proyecto TIC 2026. Este repositorio está destinado al desarrollo y la documentación de una plataforma que centralice la oferta de actividades turísticas de Uruguay.

La solución responde a la propuesta de **Uruguay Mnatural (UM)**, una organización vinculada al Ministerio de Turismo en el contexto del proyecto académico. Su propósito es conectar a turistas con empresas y prestadores particulares, facilitando la búsqueda de experiencias, las reservas y los pagos desde un único lugar.

> **Alcance de este README:** las secciones de requisitos describen lo previsto en la consigna. El estado real de cada requisito está en la sección *Estado de los requisitos*, al final de *Ejecución y pruebas*. Las decisiones técnicas y reglas de negocio pendientes se concretarán durante el análisis con el cliente.

## Problema y objetivo

Actualmente, la oferta turística se encuentra dispersa entre sitios web, redes sociales y canales de contacto propios. Esto dificulta conocer, comparar y reservar actividades disponibles en distintos departamentos.

El objetivo es construir una plataforma integrada que permita:

- Explorar experiencias turísticas de todo el país.
- Consultar información, fechas, horarios, precios y disponibilidad.
- Reservar y pagar actividades mediante un servicio externo.
- Dar a los prestadores herramientas para gestionar sus emprendimientos y su oferta.
- Compartir comentarios y valoraciones que ayuden a otros turistas.
- Ofrecer información a otras plataformas mediante una API REST.

## Usuarios y responsabilidades

| Actor | Responsabilidades principales |
| --- | --- |
| Visitante | Explorar el catálogo y consultar actividades sin registrarse. |
| Turista registrado | Reservar, pagar, consultar sus reservas y valorar experiencias en las que participó. |
| Prestador turístico | Registrar su emprendimiento, publicar actividades, organizar fechas y cupos, y consultar sus reservas. Puede ser una empresa o una persona particular. |
| Administrador de UM | Mantener los datos comunes y de configuración, como tipos de actividad y equipamiento. No gestiona las actividades en nombre de los prestadores. |

## Funcionalidades previstas

### Portal para turistas

- Consulta pública del catálogo sin necesidad de una cuenta.
- Búsqueda y filtrado por destino, tipo de actividad, fecha, precio y otros criterios definidos durante el análisis.
- Detalle de cada experiencia: emprendimiento responsable, ubicación, descripción, duración aproximada, precio, características y condiciones.
- Información sobre requisitos de edad, equipamiento necesario y equipamiento proporcionado.
- Consulta de las próximas fechas y horarios disponibles.
- Registro e inicio de sesión para efectuar reservas.
- Selección de fecha, horario y cantidad de participantes, considerando los cupos disponibles.
- Pago mediante una API de un proveedor externo.
- Consulta de reservas y de sus estados.
- Publicación de comentarios y valoraciones de **1 a 5 puntos** después de participar en la actividad.

Las actividades podrán asociarse a una o varias categorías, como naturaleza, trekking, aventura, gastronomía, actividades acuáticas, recorridos históricos, enoturismo, cabalgatas o senderismo.

### Espacio para prestadores turísticos

- Registro de los datos de identificación, contacto, ubicación y descripción del emprendimiento.
- Gestión de las actividades ofrecidas, sus precios y condiciones.
- Organización de múltiples fechas y horarios para una misma actividad.
- Definición de la capacidad de participantes de cada instancia, cuando corresponda.
- Configuración de la anticipación mínima permitida para reservar.
- Consulta de las reservas de sus actividades y de la información necesaria para organizar el servicio.

### Administración

- Alta, consulta, modificación y eliminación de tipos de actividad turística.
- Mantenimiento de los tipos de equipamiento.
- Gestión de otras entidades de configuración que se definan durante el análisis.

## Reglas de negocio principales

1. Explorar el catálogo no requiere registro; realizar una reserva sí requiere una cuenta autenticada.
2. Una actividad puede tener múltiples instancias, cada una con fecha, horario y, cuando corresponda, cupos propios.
3. La reserva debe considerar la cantidad de participantes y la disponibilidad de la instancia seleccionada.
4. El prestador establece cuántos minutos antes del inicio se cierra la posibilidad de reservar. Si configura **0 minutos**, el límite es la hora de inicio.
5. Cuando corresponda un pago, no se debe confirmar la operación si este no fue procesado satisfactoriamente.
6. Los errores, rechazos y la indisponibilidad temporal del proveedor de pagos deben manejarse de forma coherente.
7. Los comentarios y las valoraciones se habilitan después de que el turista haya participado en la actividad y forman parte de su información pública.
8. Los datos comunes son administrados por UM; la oferta y sus disponibilidades son responsabilidad de cada prestador.

Los estados de reservas y pagos, las políticas de cancelación y otras excepciones deberán definirse y validarse con el cliente.

## Integraciones y API REST

### Servicios externos

| Integración | Alcance |
| --- | --- |
| Servicio de pagos | Obligatorio. Procesamiento a través de una API externa, con manejo de resultados y fallos. |
| API pública de clima | Integración conveniente según la propuesta, para mostrar el pronóstico del lugar y la fecha de la actividad. Su incorporación deberá evaluarse. |

### API propia

La plataforma deberá exponer operaciones para:

- Listar todos los emprendimientos.
- Listar las actividades de un día para un departamento o localidad.
- Consultar las tres actividades mejor puntuadas del mes por departamento.
- Verificar un emprendimiento.
- Buscar actividades por palabra clave.

Los endpoints, parámetros, respuestas, permisos y criterios de cálculo deberán especificarse en la documentación de la API. El significado y alcance de la verificación de emprendimientos se acordarán durante el relevamiento.

## Tecnologías y arquitectura

| Área | Requerimiento de la propuesta |
| --- | --- |
| Backend | Java, Spring y Spring Boot. |
| Gestión del proyecto | Maven (el docente aceptó usar Gradle). |
| Frontend | A elección del equipo: HTML, CSS, Thymeleaf, JavaScript, React u otro framework. |
| Persistencia | Base de datos SQL: PostgreSQL o MySQL. |
| Arquitectura | Patrón MVC y organización en capas. |
| Integración | API REST con endpoints documentados. |
| Pruebas | Tests unitarios de los principales casos de uso. |
| Control de versiones | Git. |
| Convenciones | Nombres en inglés y estilo acorde al lenguaje. |

El frontend definitivo, el motor de base de datos y las versiones de las herramientas están pendientes de definición. La infraestructura podrá ser local o utilizar Docker, Google Cloud o AWS, según lo que determine el equipo.

## Modelo de dominio

El análisis deberá contemplar las relaciones entre:

- Turistas y prestadores turísticos.
- Emprendimientos y actividades.
- Categorías y tipos de equipamiento.
- Instancias de actividades: fechas, horarios y cupos.
- Reservas y participantes.
- Pagos.
- Comentarios y valoraciones.

El MER y el diagrama de clases documentarán el modelo definitivo.

## Documentación y entregables

- Acta fundacional de IATU-SA, con misión y visión.
- Documento de visión.
- Especificación de requerimientos y casos de uso.
- Diseño del sistema: MER, diagramas de secuencia y flujo, arquitectura, mockups y diagrama de clases UML.
- Actas de reunión con el cliente.
- Manuales de usuario para turistas, prestadores y administradores.
- Documentación de la API REST y sus endpoints.
- Presentación comercial y presentación técnica.
- Código fuente de la solución.

La documentación y los enlaces correspondientes se incorporarán a medida que se elaboren y validen los entregables.

## Ejecución y pruebas

### Requisitos

- Java 25 (el proyecto usa una *toolchain* de Gradle, que lo descarga si hace falta).
- PostgreSQL en el puerto **5433** con una base vacía llamada `proytic2026r`.
- No hace falta instalar Gradle: se usa el *wrapper* incluido (`gradlew` / `gradlew.bat`).

### Configuración de la base de datos

Por defecto la aplicación se conecta a `jdbc:postgresql://localhost:5433/proytic2026r` con usuario y contraseña `postgres`. Se puede cambiar sin tocar el código con las variables de entorno `DB_URL`, `DB_USER` y `DB_PASSWORD`.

Las tablas **se crean solas** al arrancar (Hibernate, `spring.jpa.hibernate.ddl-auto=update`): no hay que ejecutar ningún script SQL.

### Cómo ejecutar

```
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

Luego abrir <http://localhost:8080/>. El frontend (`src/main/resources/static/index.html`) lo sirve la propia aplicación.

### Cuentas de prueba (ya creadas)

Al arrancar con la base **vacía**, la aplicación carga automáticamente datos de ejemplo (clase `DatosIniciales`, que actúa como script de carga). Si la base ya tiene usuarios, no modifica nada.

| Rol | Email | Contraseña |
| --- | --- | --- |
| Administrador | `admin@iatu.uy` | `admin123` |
| Turista | `ana@iatu.uy` | `ana123` |
| Prestador | `martin@iatu.uy` | `martin123` |

También carga dos emprendimientos aprobados, cinco actividades con horarios para los próximos días, categorías y tipos de equipamiento, y una reserva pasada confirmada de la turista (sirve para probar las reseñas). Se puede desactivar con `app.datos-iniciales=false`.

Estas contraseñas son solo de demostración.

### Pagos (Mercado Pago)

- **Sin configurar nada**, los pagos usan un **simulador interno** (`/mock-mercadopago`). Tarjetas de prueba: `4242 4242 4242 4242` (aprobada), `4000 0000 0000 0002` (rechazada), `4000 0000 0000 9995` (fondos insuficientes), `4000 0000 0000 0077` (en revisión) y `4000 0000 0000 0119` (servicio no disponible).
- **Con Mercado Pago de pruebas**: definir las variables de entorno `MP_ACCESS_TOKEN` y `MP_PUBLIC_KEY`, o crear un archivo `application-local.properties` (puede copiarse de `application-local.properties.example`). Ese archivo está en `.gitignore` y **no debe subirse al repositorio**.

### Pruebas automáticas

```
.\gradlew.bat test
```

Son pruebas unitarias de los servicios (JUnit 5 y Mockito) sobre reservas, pagos, reseñas, categorías, equipamiento, actividades y la API pública. El test `Proytic2026rApplicationTests` arranca el contexto de Spring y necesita PostgreSQL levantado.

### API pública (solo lectura, sin sesión)

| Endpoint | Descripción |
| --- | --- |
| `GET /api/public/providers` | Emprendimientos verificados |
| `GET /api/public/activities?date=AAAA-MM-DD&department=&locality=` | Actividades con lugares libres de un día |
| `GET /api/public/top-rated?month=AAAA-MM` | Las tres actividades mejor valoradas del mes por departamento |
| `GET /api/public/providers/{id}/verification` | Estado de verificación de un emprendimiento |
| `GET /api/public/activities/search?q=texto` | Búsqueda por palabra clave |

### Estado de los requisitos

**Implementado:** catálogo público sin sesión, registro e inicio de sesión con tres roles, alta y aprobación de prestadores, publicación de actividades con categoría y equipamiento, múltiples fechas y horarios con cupos, reservas con datos de participantes y plazo de pago de 24 horas, pago con Mercado Pago (o simulador) con manejo de rechazos y de servicio no disponible, cancelación con reembolso, reseñas de 1 a 5, administración de categorías y tipos de equipamiento, API pública y pruebas unitarias.

**No implementado:**

- Integración con una API pública de clima (se evaluó y se decidió no incluirla).
- Modificar una actividad ya publicada.
- Envío de email de confirmación.
- Autenticación con Spring Security y contraseñas con hash (el inicio de sesión actual es básico).

### Decisiones técnicas

- **Gradle** en lugar de Maven (confirmado por el docente).
- Frontend en HTML, CSS y JavaScript puro, en un único archivo.

## Metodología de trabajo

El equipo abordará todas las etapas de ingeniería de software: relevamiento, análisis, diseño, implementación y pruebas. Los requisitos se validarán mediante entrevistas y reuniones con el cliente; el diseño visual y la usabilidad requerirán su aceptación.

Todos los integrantes deberán desempeñar los distintos roles del proyecto. Al finalizar el curso, el equipo presentará y defenderá el producto ante el cliente docente.

## Contexto académico

Proyecto desarrollado en el marco de **Proyectos TIC 2026**, tomando como referencia la consigna *Proyecto TIC 2026*.

La producción del proyecto corresponde a los estudiantes. Se deberá reconocer y citar el trabajo de terceros, respetando los criterios de honestidad académica establecidos por el curso.
