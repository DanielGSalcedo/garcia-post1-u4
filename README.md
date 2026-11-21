# Post-contenido | Unidad 4: Patrones de comportamiento en ComprasUDES

## Descripción

Proyecto Spring Boot para aprobar solicitudes de compra por niveles, ejecutar y deshacer dos operaciones independientes, notificar cambios de estado y aplicar las reglas del ciclo de vida. El modelo, el controlador de evaluación y los servicios y clientes provistos se conservan tal como aparecen en el enunciado.

| Paquete | Responsabilidad |
|---|---|
| `aprobacion` | Componer y evaluar niveles de aprobación |
| `ejecucion` | Ejecutar y deshacer comandos, y exponer su historial |
| `estado` | Delegar operaciones al estado actual y emitir eventos |
| `notificacion` | Reaccionar a los eventos mediante correo, tablero y auditoría |

## Cómo ejecutar

Requiere JDK 17 o superior y Maven 3.8 o superior. Desde la raíz:

```sh
mvn clean package
mvn test
mvn spring-boot:run
```

Evaluación mediante el controlador dado:

```sh
curl -X POST http://localhost:8080/api/solicitudes/evaluar \
  -H 'Content-Type: application/json' \
  -d '{"id":"S-001","solicitanteEmail":"ana@example.com","monto":2500000,"categoria":"SOFTWARE","centroCosto":"CC-100"}'
```

El ejemplo devuelve HTTP 200 y `Gerente de Área`. El controlador conserva HTTP 422 para un resultado de rechazo. El manejo de errores devuelve HTTP 409 si el estado actual impide reevaluar la solicitud.

La ejecución y la consulta del historial se exponen mediante el servicio Java `EjecucionSolicitudes`. Tras evaluar una solicitud, los consumidores de ese servicio usan la misma instancia de `Solicitud`:

```java
var reserva = ejecucion.reservarPresupuesto(solicitud);
var orden = ejecucion.generarOrden(solicitud, "Proveedor A");
ejecucion.deshacer(solicitud, orden.id());
var historial = ejecucion.historial(solicitud.getId());
```

El historial se guarda en memoria durante la ejecución y las integraciones dadas escriben en consola. La reversión se demuestra antes de cualquier contacto externo. El laboratorio no incluye persistencia de expedientes ni un proceso real de contacto con proveedores.

## Decisiones de diseño

### Necesidad 1. Chain of Responsibility para los niveles de aprobación

`CadenaAprobacion` recibe una lista ordenada de manejadores `NivelAprobacion`. Cada nivel puede producir el resultado o llamar a la continuación que representa el siguiente eslabón. El supervisor tiene autoridad hasta $2.000.000 inclusive, el gerente hasta $10.000.000 inclusive y el director atiende los montos válidos restantes. `ConfiguracionNiveles` define el orden mediante `@Order`. Es posible agregar un bean nuevo o reordenar la composición sin cambiar el controlador ni el código de los otros manejadores.

`RevisorCumplimiento` deja pasar las solicitudes nacionales sin revisión adicional. Para una solicitud internacional exige un centro de costo como regla de demostración, y, si lo tiene, continúa al nivel por monto. Si la cadena aprueba, el resultado registra al revisor como pide el caso del enunciado y conserva el decisor por monto en el detalle. Un rechazo corta la cadena en el nivel que lo determina. Se descartó Command porque encapsular acciones reversibles no decide quién tiene autoridad para resolver la solicitud ni cómo escalarla.

### Necesidad 2. Command para ejecutar y deshacer operaciones

`ReservarPresupuesto` y `GenerarOrdenCompra` guardan los datos necesarios para ejecutar su acción e invocar la operación inversa del receptor. El segundo comando conserva el número devuelto por el servicio de órdenes. `EjecucionSolicitudes` conserva las ejecuciones exitosas en orden y expone una copia consultable con identificador, tipo, fecha de ejecución y fecha de reversión. Cualquier operación activa se puede deshacer por su identificador, aunque exista otra posterior. Una reversión conserva la entrada original y la marca como deshecha.

Con una sola operación activa la solicitud sigue aprobada. Con las dos activas pasa a ejecutada. Deshacer cualquiera de las dos devuelve el estado a aprobada y conserva el efecto de la otra operación. Se descartó la cadena porque aquí el actor elige qué acción concreta ejecutar o revertir. Buscar un decisor en una secuencia no conserva los datos que el receptor necesita para compensar la acción.

### Necesidad 3. Observer para avisar a módulos independientes

`FlujoSolicitud` publica un evento inmutable `CambioEstado` mediante `ApplicationEventPublisher` después de aplicar una transición válida que cambia el estado. La cadena de aprobación y los comandos pasan por ese mismo flujo cuando corresponde. Tres métodos `@EventListener` llaman a los clientes dados. Una prueba añade un cuarto suscriptor sin cambiar el publicador. Una orden deshecha sobre una ejecución completa dispara el aviso de retorno a aprobada. Una operación parcial que no cambia el estado no genera otro evento.

Se descartó State para este problema. Los estados deciden qué comportamiento permite la solicitud, mientras que correo, tablero y auditoría son reacciones de módulos externos una vez aplicado el cambio. Agregarlas dentro de cada estado duplicaría dependencias y reglas de notificación.

### Necesidad 4. State para las transiciones del ciclo de vida

`FlujoSolicitud` resuelve el objeto `EstadoSolicitud` asociado al estado que ya guarda `Solicitud`. Cada objeto implementa las operaciones permitidas. Las demás lanzan `OperacionNoPermitida` antes de cambiar el estado o publicar un evento.

| Estado | Operaciones permitidas |
|---|---|
| PENDIENTE, EN_APROBACION | Aprobar, rechazar o cancelar |
| APROBADA | Ejecutar o cancelar |
| EJECUTADA | Revertir la ejecución como resultado de un undo completo |
| RECHAZADA, CANCELADA | Estados terminales |

Para un estado nuevo se registra otra implementación y se ajustan solo las transiciones vecinas que deban apuntar a ella. Se descartó Strategy porque la elección no la hace un cliente que sustituye un algoritmo. La historia de la solicitud determina el objeto que recibe la operación, y ese objeto determina su siguiente estado.

### Reflexión. Iterator, Template Method y Memento

- **Iterator** permite recorrer las solicitudes de un centro de costo sin exponer la colección interna.
- **Template Method** fija el orden encabezado, cuerpo y pie, y deja que cada comprobante implemente solo su cuerpo.
- **Memento** conserva una instantánea opaca del estado de una solicitud. Los comandos implementados deshacen efectos específicos del presupuesto o de la orden, sin restaurar una copia completa del objeto.

## Herramientas

Java 17 como destino de compilación, Spring Boot 3.2, Maven, JUnit 5, Mockito y Git.

## Conclusiones

El requisito de escalar una solicitud define la cadena, mientras que la reversión independiente exige comandos con datos propios. Los eventos permiten incorporar reacciones sin modificar el ciclo de vida. Los objetos de estado reúnen las reglas de transición y evitan que se repitan en la aprobación y la ejecución. Las pruebas separan estas responsabilidades y también verifican cómo colaboran en una misma solicitud.
