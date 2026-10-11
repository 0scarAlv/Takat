# Takat

App de finanzas personales para Android: cuentas, movimientos, gastos fijos y presupuesto diario, con estadísticas y acceso opcional desde la PC. Todo queda en el teléfono, sin nube ni cuenta de por medio.

![Versión](https://img.shields.io/badge/versión-3.1.0-amber) ![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84) ![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF)

---

## Contenido

- [Instalación](#instalación)
- [Características](#características)
- [Cómo se usa](#cómo-se-usa)
- [Stack técnico](#stack-técnico)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Compilar desde el código](#compilar-desde-el-código)
- [Publicar una versión](#publicar-una-versión)
- [Licencia](#licencia)

---

## Instalación

Takat no está en Google Play. Se distribuye como APK firmada desde los [Releases](https://github.com/0scarAlv/Takat/releases) de este repo.

1. Descargá el `Takat-vX.Y.Z.apk` del último Release.
2. Abrilo en el teléfono y aceptá instalar apps de origen desconocido si te lo pide.
3. Para actualizar, instalá la APK nueva encima de la anterior: tus datos se conservan.

La app avisa sola cuando hay una versión nueva (Ajustes → Buscar actualizaciones).

## Características

| Área | Qué hace |
|---|---|
| **Cuentas** | Normales o de deuda, con saldo inicial, color y opción de excluirlas del resumen. |
| **Movimientos y transferencias** | Categorías con emoji, nota, fecha elegible y comprobante adjunto (foto, PDF o JSON). Editables después. |
| **Gastos fijos** | Pagos quincenales o mensuales de tipo Normal, Cuotas o Crédito (con tasa de interés), pagos parciales y recordatorios. |
| **Presupuesto diario** | Reparte el saldo disponible entre los días que faltan para el próximo pago. |
| **Estadísticas** | Gasto por categoría, gasto por día o semana y tendencia de los últimos 6 meses. |
| **Saldo a la fecha** | Desde cualquier movimiento: saldo antes y después, evolución, acumulados y gasto por categoría hasta ese momento. |
| **Respaldo** | Exportar/importar en `.zip` (agregar lo que falta, reemplazar o duplicar) y respaldo automático diario a una carpeta. |
| **Acceso desde PC** | Panel web servido por el propio teléfono en la red local, vinculado por QR y cifrado de punta a punta. |
| **Widgets y atajos** | Widgets de saldo, presupuesto diario y acciones rápidas, más un tile de Ajustes rápidos para cargar un movimiento. |
| **Apariencia** | Diseño Nuevo o Clásico, modo claro/oscuro y opción de ocultar montos. |

## Cómo se usa

**Inicio** tiene tres páginas que se recorren deslizando:

- **Presupuesto** (izquierda): ingresos contra gastos del mes y el presupuesto diario. Se configura el ciclo de pago (quincena o mes) y si se calcula sobre el disponible o el capital total.
- **Inicio** (centro): la tarjeta de saldo disponible, tus cuentas, los gastos fijos pendientes y los movimientos agrupados por día.
- **Estadísticas** (derecha): gráficas del mes elegido. Tocando una categoría se ven todos sus gastos.

Los botones de abajo crean una **cuenta**, un **movimiento** o una **transferencia**. Tocando cualquier cuenta o movimiento se abre su detalle para editarlo, borrarlo o ver su **saldo a la fecha**.

En **Ajustes** están el respaldo, el acceso desde PC, la apariencia, las novedades de cada versión y la búsqueda de actualizaciones.

## Stack técnico

- **App:** Kotlin, Jetpack Compose (Material 3), Navigation Compose, kotlinx.serialization.
- **Datos:** Room, con WorkManager para respaldos, recordatorios y refresco de widgets.
- **Widgets:** Glance.
- **Panel de PC:** servidor embebido con Ktor, anunciado por mDNS, front-end en HTML/JS vanilla y cifrado E2E con `@noble/ciphers` y `@noble/curves`.
- **Vinculación por QR:** CameraX, ML Kit Barcode Scanning y ZXing.
- **SDK:** `minSdk` 26, `targetSdk` 36, Java 11.

## Estructura del proyecto

```
app/src/main/java/com/takat/finanzas/
├── data/           Room: entidades, DAOs, repositorios, adjuntos y CSV
├── ui/             Pantallas Compose por funcionalidad (home, stats, fixedexpense…)
│   ├── classic/    Diseño Clásico, conservado tal cual para el selector de diseño
│   └── theme/      Colores, tipografía y formas
├── backup/         Exportar/importar y respaldo automático diario
├── network/        Servidor local para la PC, vinculación, cifrado y actualizaciones
├── notifications/  Recordatorios de gastos fijos
├── widget/         Widgets de inicio (Glance)
├── tile/           Tile de Ajustes rápidos
└── util/           Dinero, fechas, presupuesto diario, changelog in-app
```

## Compilar desde el código

```bash
git clone git@github.com:0scarAlv/Takat.git
cd Takat
./gradlew assembleDebug   # o installDebug con un dispositivo conectado
```

Para un build de release firmado hace falta un `keystore.properties` con el keystore propio (no se versiona). Sin él, `assembleRelease` e `installRelease` fallan a propósito.

## Publicar una versión

1. Subir `versionCode` y `versionName` en `app/build.gradle.kts`.
2. Agregar la entrada de la versión en `util/Changelog.kt` (es lo que muestra Ajustes → Novedades).
3. Compilar con `./gradlew assembleRelease`.
4. Crear un **Release** en GitHub con la APK adjunta como `Takat-vX.Y.Z.apk` y las notas de la versión. El buscador de actualizaciones solo detecta Releases publicados con APK, no tags sueltos.

El historial de cambios vive en los [Releases](https://github.com/0scarAlv/Takat/releases) y dentro de la app, en Ajustes → Novedades.

## Licencia

Todos los derechos reservados © Oscar Alvarado 2026.
