# DonutPay Tracker

Mod de Fabric **solo cliente** para Minecraft **1.21.11** que registra tus pagos `/pay` en DonutSMP.
Inspirado en Gambler Plus, con una interfaz propia y bids donde **los pagos no se apilan**.

## Funciones
- Registro de cada `/pay` enviado y recibido, cada uno como entrada separada.
- Ventana con pestañas Historial, Jugadores (neto + rakeback), Bids y Sesiones. Se abre con la tecla **G** (configurable en Controles) o con `/dpt`.
- Sesiones: `/dpt session start` · `/dpt session stop`
- Rakeback por jugador: `/dpt rakeback <porcentaje>`
- HUD en vivo: `/dpt hud`
- Confirmación de `/pay` grandes: `/dpt confirm 500k` (o `off`), y respondes con `/dpt yes` / `/dpt no`
- Bids: `/dpt bid start` · `/dpt bid top` · `/dpt bid stop`.
  Los pagos del mismo jugador **no se suman**: su puja válida es su pago individual más alto.

Los datos se guardan en `config/donutpay-tracker.json` y `config/donutpay-config.json`.
El mod solo lee mensajes del servidor (no el chat de jugadores) y no envía nada por su cuenta, salvo el `/pay` que tú confirmas.

## Requisitos
- Minecraft 1.21.11, Fabric Loader 0.18+, Fabric API, Java 21

## Compilar
Con Gradle instalado:

    gradle build

El jar queda en `build/libs/`. Si prefieres el wrapper, ejecuta `gradle wrapper` una vez y haz commit de los archivos que genera.

También se compila solo en GitHub: cada push ejecuta la acción **build** y deja el jar en *Actions → build → Artifacts*.

## Configuración pendiente
Los patrones de `PaymentParser.java` son de ejemplo. Ajústalos al formato real de los mensajes de pago de DonutSMP.

## Licencia
MIT
