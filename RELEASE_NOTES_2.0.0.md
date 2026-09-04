# Takat v2.0.0

## Nuevo
- **Takat se actualiza sola**: como la app no está en Play Store ni ninguna otra tienda, ahora se fija sola si hay una versión nueva publicada en GitHub cada vez que la abrís, y te deja descargarla e instalarla sin salir de Takat ni pedirte el link a mano.
- Cuando hay una actualización disponible, aparece un botón verde **"Actualizar"** junto al título Takat en Inicio, para instalarla cuando quieras aunque hayas cerrado el aviso automático.
- Nuevo botón **"Buscar actualizaciones"** en Ajustes → Acerca de, para revisar manualmente cuando quieras.
- **Acceso desde PC**: Takat ahora tiene un panel completo accesible desde el navegador de una PC en tu misma wifi — cuentas y saldos, disponible total, movimientos recientes agrupados por día, agregar/eliminar movimientos y transferencias, y exportar todo a CSV. Se vincula escaneando un código QR y viaja cifrado de punta a punta, sin pasar por ningún servidor externo.
- Nuevo acceso rápido en el panel de ajustes rápidos del sistema para crear un movimiento sin abrir la app ni usar un widget.

## Mejoras
- Panel de PC: rediseño para pantalla de escritorio (barra lateral con cuentas + disponible, tabla de movimientos), botón para copiar la dirección al portapapeles, revocar una PC vinculada y un apodo opcional para distinguir tu teléfono en la red.
- Ícono del acceso rápido de ajustes rápidos actualizado (flechas de movimiento en vez del "+").

## Arreglos
- Ingresos vs Gastos ya no cuenta un sueldo pagado en la segunda mitad del mes (por ejemplo el día 31) como ingreso de ese mes — ahora se atribuye al mes que realmente financia.
- Vincular una PC ya no fallaba por falta de cifrado nativo en direcciones de red local sin https.
- El interruptor de "Acceso desde PC" en Ajustes ya no aparecía apagado al volver a entrar aunque el servicio siguiera activo.
