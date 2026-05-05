# TuxGuitar Converter API

This folder contains a small Dockerized HTTP API that wraps TuxGuitar's format conversion support and always produces MusicXML output.

## Endpoints

### `POST /api/convert/request`

Multipart form fields:

- `inputFile`: uploaded score file.
- `compressedOutput`: `true` for `.mxl`, `false` for `.musicxml`.
- `callbackUrl`: optional URL called with a JSON `POST` once conversion succeeds.

Response:

```json
{
  "jobId": "uuid"
}
```

### `GET /api/convert/result/{jobId}`

- Returns `202` with JSON while the job is queued or processing.
- Returns `500` with JSON if conversion failed.
- Returns the converted `.musicxml` or `.mxl` file as an attachment when complete.

Each job is stored under `jobs/<jobId>/` with both the original input file and the generated output file.

## Local Build

```sh
./converter-api/build.sh
java -jar converter-api/target/tuxguitar-converter-api-9.99-SNAPSHOT.jar
```

## Docker

```sh
docker build -f converter-api/Dockerfile -t tuxguitar-converter-api .
docker run --rm -p 8080:8080 -v $(pwd)/converter-api-data:/data tuxguitar-converter-api
```

## Callback payload

Successful callbacks use `POST` with `application/json`:

```json
{
  "jobId": "uuid",
  "status": "COMPLETED",
  "fileName": "uuid.musicxml"
}
```