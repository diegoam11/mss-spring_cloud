# Circuit Breaker + Retry: department-service → employee-service

`department-service` llama a `employee-service` vía Feign (`EmployeeClient`) para traer los empleados de un departamento (`GET /api/v1/departments/{id}/employees`). Esa llamada está protegida con `@CircuitBreaker` + `@Retry` de Resilience4j en `EmployeeIntegrationService`.

## Orden de los decoradores

Por defecto, Resilience4j aplica `@Retry` por fuera de `@CircuitBreaker`, lo que hace que cada intento individual de un retry cuente como una llamada separada para el circuit breaker (fallos inflados). En este proyecto se invirtió el orden explícitamente en `department-service.yml`:

```yaml
resilience4j:
  circuitbreaker:
    circuit-breaker-aspect-order: 1 # CircuitBreaker por fuera
  retry:
    retry-aspect-order: 2 # Retry por dentro
```

Así, el `CircuitBreaker` envuelve al `Retry`: toda la secuencia de reintentos de una petición cuenta como **una sola llamada** en la ventana del circuit breaker.

## Configuración actual

- **Circuit breaker** (`employeeService`): ventana de 10 llamadas, abre si el 50% falla, permanece abierto 10s, 3 llamadas de prueba en half-open, transición automática a half-open.
- **Retry** (`employeeService`): hasta 3 intentos, espera base 500ms con backoff exponencial x2 (500ms → 1000ms).

## Flujo cuando employee-service falla

### 1. Circuito `CLOSED` (operación normal)

Llega una petición a `GET /api/v1/departments/{id}/employees`:

1. El `CircuitBreaker` está `CLOSED`, permite la llamada y se la pasa al `Retry`.
2. `Retry` intenta la llamada real. Si falla:
   - Intento 1 falla → espera 500ms → Intento 2 falla → espera 1000ms → Intento 3 falla → se acaban los intentos.
3. Esa secuencia completa (los 3 intentos) se reporta al `CircuitBreaker` como **una sola llamada fallida**, no como 3 fallos separados.
4. El `CircuitBreaker` suma ese fallo a su ventana de 10 y dispara el `fallbackMethod` → el controller responde `503` con lista vacía.
5. Latencia de esa petición: ~1.5s (500ms + 1000ms de espera) antes de responder.

### 2. Se abre el circuito

Si 5 de las últimas 10 llamadas (contadas "una por request", no por intento interno) fallaron, el `CircuitBreaker` pasa a `OPEN`.

### 3. Circuito `OPEN`

Mientras está abierto, las peticiones nuevas ni siquiera llegan al `Retry`: el `CircuitBreaker` las rechaza al instante (sin red, sin reintentos, sin espera) y va directo al fallback → `503` casi inmediato. Esto dura `wait-duration-in-open-state: 10s`.

### 4. Transición a `HALF_OPEN`

Pasados los 10s, el circuito cambia solo a `HALF_OPEN` (visible en `/actuator/circuitbreakers`), pero no hace nada hasta que llegue tráfico real. Ahí deja pasar hasta 3 peticiones de prueba — cada una puede volver a disparar su propio ciclo interno de reintentos con backoff si falla.

- Si 2 de esas 3 pasan (employee-service ya respondió bien) → circuito vuelve a `CLOSED`, operación normal.
- Si fallan la mayoría → vuelve a `OPEN` por otros 10s.

## Resumen

El `Retry` amortigua fallos cortos/puntuales sin que el `CircuitBreaker` se entere (una recuperación en el intento 2 o 3 cuenta como éxito). El `CircuitBreaker` solo ve "esta petición completa funcionó o no", y cuando detecta que el servicio está realmente caído, corta el tráfico de inmediato para no seguir generando reintentos ni latencia innecesaria.

## Cómo inspeccionar el estado

Actuator está expuesto directo en `department-service` (puerto 8083, no pasa por el gateway):

```bash
curl http://localhost:8083/actuator/circuitbreakers
curl http://localhost:8083/actuator/health
```
