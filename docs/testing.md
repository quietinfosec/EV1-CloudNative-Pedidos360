# Estrategia de Pruebas y Control de Calidad — Pedidos360

Este documento describe la suite de pruebas automatizadas implementadas para la plataforma **Pedidos360**.

---

## 1. Cobertura de Pruebas Backend

| Módulo | Pruebas | Frameworks | Estrategia de Aislamiento |
|---|---|---|---|
| `bff-service` | 18 | JUnit 5, MockMvc, Spring Security Test, Mockito | Mocks de RestClient para microservicios y JwtDecoder en memoria |
| `pedidos-service` | 30 | JUnit 5, DataJpaTest, MockMvc, Mockito | Base de datos H2 en memoria con compatibilidad PostgreSQL |
| `productos-service` | 28 | JUnit 5, DataJpaTest, MockMvc, Mockito | Base de datos H2 en memoria con compatibilidad PostgreSQL |
| **Total Backend** | **76** | **100% Pasadas (0 Fallos)** | **Cero dependencias de servicios cloud activos** |

---

## 2. Ejecución de Pruebas

Para ejecutar la suite completa en local:

```bash
# Probar BFF
cd backend/bff-service && ./mvnw test

# Probar Pedidos
cd backend/pedidos-service && ./mvnw test

# Probar Productos
cd backend/productos-service && ./mvnw test

# Build completo frontend y backend
powershell -ExecutionPolicy Bypass -File scripts/build-all.ps1
```
