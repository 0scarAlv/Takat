# Takat

App de finanzas personales para Android. Cuentas, movimientos, transferencias, gastos fijos y presupuesto diario, con estadísticas y acceso opcional desde la PC — todo local, sin nube ni cuenta de por medio.

No está en Google Play: se distribuye como APK firmada, directo desde los [Releases](https://github.com/0scarAlv/Takat/releases) de este repo.

## Características

- **Cuentas**: normales o de deuda, con saldo inicial, color y opción de excluirlas del resumen.
- **Movimientos y transferencias**: categorías con emoji, nota, fecha elegible, comprobante adjunto (foto, PDF o JSON) — y editables después si te equivocaste en algo.
- **Gastos fijos**: pagos recurrentes (quincenales o mensuales), con pagos parciales, recordatorios y planes de deuda a cuotas.
- **Presupuesto diario**: reparte el saldo disponible entre los días que faltan para el próximo pago.
- **Estadísticas**: gasto por categoría (dona), por día del mes (barras) y tendencia de los últimos 6 meses (líneas).
- **Saldo a la fecha**: desde cualquier movimiento, una página con el saldo antes y después, cómo fue cambiando, ingresos/gastos acumulados y por categoría hasta ese momento — como un estado de cuenta.
- **Respaldo**: exportar/importar todo en un .zip, más respaldo automático diario a una carpeta elegida.
- **Acceso desde PC**: panel web servido por el propio teléfono en la red local, vinculado por QR y cifrado de punta a punta — sin servidor externo.
- **Widget** de inicio con acceso directo a cargar un movimiento o transferencia.
- **Buscador de actualizaciones** in-app contra los Releases de este repo (no depende de ninguna tienda de apps).

## Stack técnico

- Kotlin + Jetpack Compose (Material 3) + Room + WorkManager + Glance (widget).
- Navegación con Navigation Compose, serialización con kotlinx.serialization.
- Panel de PC: servidor embebido con Ktor, front-end en HTML/JS vanilla, cifrado E2E con `@noble/ciphers` y `@noble/curves`.
- Vinculación por QR: CameraX + ML Kit Barcode Scanning + ZXing.
- `minSdk` 26, `targetSdk` 36.

## Compilar desde el código

```bash
git clone git@github.com:0scarAlv/Takat.git
cd Takat
./gradlew assembleDebug   # o installDebug con un dispositivo/emulador conectado
```

Para un build de release firmado hace falta un `keystore.properties` (no versionado) con el keystore propio — sin eso, `assembleRelease`/`installRelease` fallan por diseño.

## Documentación de uso

[`TUTORIAL.md`](TUTORIAL.md) explica cómo usar la app pantalla por pantalla. El historial completo de cambios está en [`RELEASE_NOTES_*.md`](.) y en los [Releases de GitHub](https://github.com/0scarAlv/Takat/releases); dentro de la app, Ajustes → Novedades muestra lo mismo.

## Licencia

Todos los derechos reservados © Oscar Alvarado 2026.
