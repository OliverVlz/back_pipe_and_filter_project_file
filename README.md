# file-pipeline-processor (Pipe & Filter)

Este proyecto implementa el **Backend 2** del sistema: consume eventos desde RabbitMQ, recupera el archivo desde el MySQL del **Backend 1** (solo lectura), aplica criptografía (cifrar + descifrar para validar) y guarda resultados en **MinIO**.

## Qué hace HOY (implementado)

Pipeline (orden real):

1. **RabbitMQ Consumer**: consume mensajes de la cola `file.processing.queue`.
2. **DTO + Validación**: parsea JSON a un DTO y valida campos obligatorios.
3. **MySQL (mysql-files) READ ONLY**: hace `SELECT file_data FROM file_documents WHERE uuid = ?`.
4. **Criptografía híbrida**:
	 - Datos: **AES-256-GCM**
	 - Clave: **RSA-OAEP (SHA-256)** para envolver la clave AES
5. **Validación**: descifra y compara **SHA-256** (original vs decrypted).
6. **MinIO**: sube 3 objetos:
	 - `original/{fileUuid}.bin`
	 - `encrypted/{fileUuid}.enc`
	 - `metadata/{fileUuid}.json` (incluye `ivBase64` y `encryptedAesKeyBase64`)

## Qué NO hace todavía (no es tu responsabilidad)

- No publica mensajes en RabbitMQ (solo consume).
- No escribe en MySQL del Backend 1 (ni hash, ni status, ni metadata).
- No usa Mongo hoy (se deja infraestructura para trabajo futuro del equipo).

## Contrato del mensaje (RabbitMQ)

El mensaje **NO contiene el archivo**. Solo referencia/metadata.

El formato esperado (compatible con `FileUploadMessage` del Backend 1) es:

```json
{
	"fileId": 123,
	"fileUuid": "7d6a0e63-1b5c-4cbb-a4b2-3c2b2b7a1c2f",
	"fileName": "documento.pdf",
	"contentType": "application/pdf"
}
```

## Configuración (application.yaml)

Variables más importantes:

### RabbitMQ

- `RABBITMQ_HOST` (default `localhost`)
- `RABBITMQ_PORT` (default `5672`)
- `RABBITMQ_USER` (default `admin`)
- `RABBITMQ_PASSWORD` (default `admin123`)
- Cola (default): `FILE_SERVICE_RABBITMQ_QUEUE=file.processing.queue`

### MySQL del Backend 1 (mysql-files) – SOLO LECTURA

- `FILESDB_URL` (default `jdbc:mysql://localhost:3307/file_service_db`)
- `FILESDB_USER` (default `fileuser`)
- `FILESDB_PASSWORD` (default `filepass123`)

Tabla consultada:

- `file_documents(uuid, file_data, ...)`

### MinIO

- `MINIO_URL` (default `http://localhost:9000`)
- `MINIO_ACCESS_KEY` (default `minioadmin`)
- `MINIO_SECRET_KEY` (default `minioadmin`)
- `MINIO_BUCKET` (default `file-pipeline`)

## Cómo levantar y probar (Windows)

### 1) Levanta infraestructura

**Backend 1** (infra principal):

- Levanta `mysql-files` + `rabbitmq` usando el `docker-compose.yml` del Backend 1.
- Verifica RabbitMQ UI: http://localhost:15672 (credenciales según el compose del Backend 1)

**Este proyecto (Backend 2)**:

En la carpeta de este proyecto:

```bash
docker compose up -d
```

Esto levanta **MinIO** (y deja MySQL/Mongo disponibles para trabajo futuro).

Verifica MinIO Console: http://localhost:9001 (default `minioadmin/minioadmin`).

### 2) Ejecuta el servicio

Opción A (VS Code): ejecutar la clase principal `FilePipelineProcessorApplication`.

Opción B (terminal):

```bash
./mvnw.cmd spring-boot:run
```

### 3) Genera un evento real (forma recomendada)

Usa la API del **Backend 1** para subir un archivo.

Eso debe:

1) Guardar el archivo en `file_documents.file_data`
2) Publicar el mensaje a RabbitMQ

### 4) Qué debes ver (visualización)

**En logs del Backend 2** (este proyecto):

- `[RabbitMQ] Mensaje recibido...`
- `[Pipeline] Iniciando...`
- `[Pipeline] OK...`

**En MinIO Console**:

- Bucket `file-pipeline`
- Objetos:
	- `original/{fileUuid}.bin`
	- `encrypted/{fileUuid}.enc`
	- `metadata/{fileUuid}.json`

## Troubleshooting rápido

- Error “Archivo no encontrado en mysql-files”: revisa que `FILESDB_URL` apunte al MySQL del Backend 1 y que el `fileUuid` exista en `file_documents`.
- No llegan mensajes: revisa en RabbitMQ UI que exista la cola `file.processing.queue` y que el exchange/binding esté correcto.
- MinIO no muestra bucket: entra a http://localhost:9001 y revisa credenciales/puertos.
