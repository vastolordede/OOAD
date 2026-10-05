# Common Backend Foundation (F05-F10)

- F05: package structure (`controller`, `service`, `repository`, `entity`, `dto`)
- F06: common `ApiResponse<T>`
- F07: `GlobalExceptionHandler` + common exceptions
- F08: Bean Validation dependency + validation exception handling
- F09: common `PageResponse<T>`
- F10: Swagger/OpenAPI configuration

After the backend is running:
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

JWT security is not implemented in this cluster yet. The OpenAPI bearer scheme is declared now so later auth work does not require redesigning the API documentation.
