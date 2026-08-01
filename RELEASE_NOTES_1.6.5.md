# Takat v1.6.5

## Nuevo
- **Compartir para crear movimiento**: ahora podés compartir una imagen, PDF o JSON desde cualquier otra app directamente a Takat para crear un nuevo movimiento con el archivo ya adjunto. Soporta elegir varias imágenes/archivos a la vez.
- **Presupuesto diario con 3 valores**: además del valor diario que se recalcula con cada movimiento, ahora se muestra un **presupuesto diario fijo** (se congela solo al pasar la medianoche, tomando el último valor observado) y **gastado hoy / disponible hoy** (con la diferencia contra ese presupuesto).
- **Widget de presupuesto diario**: nuevo widget para la pantalla de inicio con gastado hoy, presupuesto diario y disponible hoy, más los mismos accesos rápidos que el widget principal.
- **Widget de accesos rápidos**: nuevo widget compacto con solo los botones de nuevo movimiento, abrir la app y nueva transferencia.
- **Tema claro / oscuro / sistema**: nueva sección "Apariencia" en Ajustes para elegir el tema de la app.
- Los 3 widgets ahora muestran una miniatura real de sí mismos en el selector de widgets de Android (12+), en vez del ícono genérico de la app.

## Mejoras
- Home: el selector de secciones ahora son pestañas con nombre (Presupuesto / Inicio / Estadísticas) en vez de puntos.
- Gastos fijos: cada movimiento pendiente ahora indica si es **Quincenal** o **Mensual**, en vez de un total que mezclaba ambas periodicidades y confundía.
- Widgets: números centrados y más grandes, tamaño natural ajustado (4×1), y fondo más oscuro en modo claro para que los colores no se laven en pantallas OLED.
- Separadores de miles y decimales corregidos (coma para miles, punto para decimales).

## Arreglos
- El monto de un movimiento ya no se deforma cuando la nota es muy larga (se trunca con "…" en vez de empujar el monto fuera de la fila).
- "Gastado hoy" ya no cuenta los pagos de gastos fijos — ese dinero ya estaba reservado aparte del presupuesto diario, así que pagarlos no debía restar dos veces.
