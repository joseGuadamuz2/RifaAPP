# Rifador — App de Gestión de Rifas (LotteryApp)

Aplicación Android nativa (**Kotlin + Jetpack Compose + Room**) para gestionar la venta de boletos de rifas, orientada al mercado costarricense. Permite crear rifas de 100 números, venderlos o apartarlos, hacer seguimiento de cobros, notificar por WhatsApp, registrar ganadores y generar reportes.

---

## 1. Descripción general

La app cubre el ciclo de vida completo de una rifa:

1. **Creación** de la rifa (nombre, premio, foto opcional del premio, precio, fecha del sorteo, tipo de sorteo y modalidad).
2. **Venta/apartado** de boletos sobre una cuadrícula de 100 números (00–99).
3. **Administración de ventas**: cobros, reenvío de recibos, recordatorios de pago y directorio de clientes.
4. **Cierre del sorteo** y **registro de ganadores**.
5. **Difusión**: generación de flyer con los números disponibles y reportes PDF/WhatsApp.

Cada rifa genera siempre **100 boletos numerados de `00` a `99`**, en una de dos modalidades:

| Modalidad | Descripción |
|---|---|
| **Sencilla** | 100 boletos individuales. |
| **Por grupos** | Los 100 números se mezclan aleatoriamente y se agrupan en bloques de 2, 4, 5, 10, 20, 25 o 50 números; la venta es por grupo. |

Los estados de un boleto son: `AVAILABLE` (libre), `RESERVED` (apartado) y `SOLD` (vendido/pagado).

---

## 2. Arquitectura

Patrón **MVVM** con capas claras, sin framework de inyección de dependencias (se usan *Factories* manuales):

```
LotteryApp (Application) ──► RaffleRepository ──► Room (AppDatabase + DAOs)
        │
        └──► ViewModels (StateFlow) ──► Compose Screens
```

- **`LotteryApp.kt`** — `Application` que expone el `RaffleRepository` (inicializa la BD Room).
- **`MainActivity.kt`** — contenedor de la UI; define la navegación con **Navigation Compose**.
- **`ui/`** — paquetes por pantalla (`home`, `raffle`, `winners`) con su `Screen` + `ViewModel`.
- **`data/`** — entidades Room, DAOs y `AppDatabase` (con migraciones versionadas).
- **`repository/RaffleRepository.kt`** — única puerta de acceso a datos y reglas de negocio.
- **`util/`** — utilidades de integración: WhatsApp, generación de flyer, reporte PDF y fechas.

### Navegación (rutas)

| Ruta | Pantalla |
|---|---|
| `home` | Lista de rifas (Activas / Finalizadas) con búsqueda |
| `createRaffle` | Crear nueva rifa |
| `editRaffle/{raffleId}` | Editar rifa existente |
| `ticketGrid/{raffleId}` | Cuadrícula de 100 boletos / grupos (panel de venta) |
| `soldTickets/{raffleId}` | Administrador de ventas y directorio de clientes |
| `winners/{raffleId}` | Registro y gestión de ganadores |

---

## 3. Modelo de datos

### Entidades (Room, versión 5)

| Entidad | Tabla | Campos clave | Observaciones |
|---|---|---|---|
| `Raffle` | `raffles` | id, name, prizeName, prizeDescription, prizePhotoPath, prizeValue, ticketPrice, drawDate, source, modality, groupSize, winningNumber, status | `source`: LOTERIA_NACIONAL / CHANCES / SORTEO / MANUAL / OTRO. `status`: ACTIVE / CLOSED. `winningNumber` se actualiza al registrar un ganador. |
| `Ticket` | `tickets` | id, raffleId (FK cascade), number, buyerName, buyerPhone, status, groupId | `groupId` agrupa boletos vendidos/creados por grupo (con índice). |
| `CancellationHistory` | `cancellation_history` | id, ticketId (FK cascade), previousBuyerName, previousBuyerPhone, cancellationDate | Historial de liberaciones (cancelaciones). |
| `Winner` | `winners` | id, raffleId (FK cascade), winningNumber, buyerName, buyerPhone, prizeName, prizeAmount, registeredAt, notified | `prizeName` y `prizeAmount` se copian automáticamente de la rifa al registrar el ganador. |

### Relaciones

- `Raffle` 1 ── N `Ticket` (borrado en cascada).
- `Ticket` 1 ── N `CancellationHistory` (borrado en cascada).
- `Raffle` 1 ── N `Winner` (borrado en cascada).
- Los grupos se modelan a través del campo `Ticket.groupId` (no hay tabla separada).

### Migraciones

- **1→2**: añade `modality` y `groupSize` a `raffles`.
- **2→3**: crea la tabla `winners` + índice.
- **3→4**: corrige el nombre del enum `MANUAl` → `MANUAL` en los datos existentes; elimina la columna `imageSent` de `tickets` (recreando la tabla) y añade índice sobre `groupId`.
- **4→5**: añade `prizeName` a `winners` (el premio del ganador se registra con el nombre del premio de la rifa).
- El esquema está versionado con `exportSchema = true` (`app/schemas`).

---

## 4. Reglas de negocio (lógica clave)

### 4.1 Creación de rifa (`RaffleRepository.createRaffle`)
- Inserta la rifa y genera 100 boletos `00–99`.
- En modalidad **grupos**: mezcla los números (`shuffled()`) y los agrupa en bloques del tamaño elegido; cada bloque recibe un `groupId` (UUID).

### 4.2 Venta / apartado (`sellOrReserveGroup`)
- Ejecuta la validación y las escrituras dentro de una **transacción Room** (`withTransaction`), evitando ventas duplicadas ante concurrencia.
- Valida **disponibilidad** de cada boleto (debe estar `AVAILABLE`).
- En modo grupos conserva el `groupId` original si todos los boletos comparten uno; en sencilla crea un `groupId` nuevo (permite agrupar la venta).
- Guarda comprador, teléfono y estado (`SOLD` o `RESERVED`).

### 4.3 Liberación / cancelación (`cancelTicket` / `cancelTickets`)
- Registra un `CancellationHistory` con los datos previos del comprador.
- Devuelve el/los boletos a `AVAILABLE`, limpiando comprador/teléfono (conserva el `groupId`). Ambas operaciones son transaccionales.

### 4.4 Ganadores
- El **premio se precarga automáticamente** desde la rifa (nombre del premio + valor económico si existe) y queda **editable** en el diálogo por si hay que corregirlo.
- `registerWinner`: valida que el número no esté ya registrado como ganador de la rifa, inserta el ganador, guarda `winningNumber` en la rifa y la **cierra** (`CLOSED`). Todo en una transacción.
- `updateWinner`: valida duplicados (excluyéndose a sí mismo).
- `deleteWinner`: si no quedan ganadores, **reabre** la rifa (`ACTIVE`).
- El registro de ganadores se habilita **a partir del día del sorteo** (`WinnersScreen`).

### 4.5 Cierre automático por fecha (`HomeViewModel`)
- Al abrir la app, las rifas cuyo día de sorteo ya pasó se cierran automáticamente. Se permite registrar ventas durante el mismo día del sorteo (cierra al día siguiente).

### 4.6 Estados y pantallas
- `changeTicketStatus` permite alternar `SOLD ↔ RESERVED` (confirmar pago o revertirlo).
- `HomeViewModel` calcula por rifa: progreso de venta, cantidad vendida y **monto recaudado** (`soldCount × ticketPrice`), ajustando la modalidad por grupos (grupos vendidos = unidad).

### 4.7 Rendimiento
- La agrupación de ventas por grupo se resuelve **en memoria** a partir del flujo completo de boletos (sin queries N+1), tanto en el administrador de ventas como en el buscador de ganadores.
- El directorio de clientes agrupa por **nombre + teléfono** para no fusionar compradores homónimos.

---

## 5. Integraciones y utilidades

| Utilidad | Función |
|---|---|
| `WhatsAppSender` | Envía recibos de venta, recordatorios de cobro y notificaciones de ganador mediante `https://wa.me/` (agrega código país `506` a números de 8 dígitos). Retorna `false` (y las pantallas muestran aviso) si el dispositivo no puede abrir WhatsApp. |
| `ImageSharingHelper` | Genera un **flyer** (bitmap) con el premio, datos y cuadrícula de números disponibles; lo comparte como imagen + texto. Persiste la foto del premio en almacenamiento interno. |
| `PdfReportHelper` | Genera y comparte un **reporte de ventas en PDF** (recaudado, por cobrar, boletos vendidos/pendientes, detalle por cliente). |
| `DateUtils` | Compara fechas del `DatePicker` (UTC-midnight) contra el día local en formato `YYYYMMDD`. |
| `PhoneFieldWithContacts` | Campo de teléfono con selector de contactos del dispositivo (filtra solo dígitos, máx. 8). |

---

## 6. Stack tecnológico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose (Material 3) + Navigation Compose
- **Persistencia:** Room (SQLite) con KSP
- **Imágenes:** Coil (`AsyncImage`)
- **Iconos:** Material Icons Extended
- **minSdk:** 24 · **target/compile SDK:** 36/37
- **Sin tests unitarios ni de instrumentación en uso** (solo dependencias declaradas).

---

## 7. Estado actual del proyecto

- Funcionalidad principal **completa**: creación, venta, apartado, cancelación, administración de ventas, ganadores, flyer, WhatsApp y reportes.
- La app está orientada a Costa Rica (colones ₡, código país 506, jerga "apartado/vender").
- Mejoras aplicadas: transacciones atómicas, validación de ganadores duplicados, manejo de ausencia de WhatsApp, corrección de `MANUAl`→`MANUAL`, eliminación de código muerto (`Buyer`, `Draw`, `imageSent`), consultas sin N+1 y directorio agrupado por nombre+teléfono.
- Pendientes sugeridos: extraer strings a recursos, tests unitarios, limpieza de fotos huérfanas y colores fijos que rompen el dark mode.