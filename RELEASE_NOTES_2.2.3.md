# Takat v2.2.3

## Nuevo
- **Ocultar montos**: ícono de ojo en Inicio para ocultar/mostrar Disponible, Capital total, Gasto fijo, Deuda total y el saldo de cada cuenta.
- Nuevo gráfico de barras en Estadísticas: gasto por día del mes, con el día de mayor gasto marcado — doble toque en una barra para ver el detalle de ese día, con todos sus movimientos.
- Antes de instalar una actualización, Takat ahora revisa el respaldo automático: si está activo, hace una copia de seguridad antes de aplicar el cambio; si no está activo, te pregunta si querés activarlo (y te dice dónde) o seguir sin él.

## Mejoras
- Eliminar una cuenta con movimientos guardados ya no borra también sus movimientos y transferencias: quedan intactos, aunque la cuenta desaparezca.
- Al cargar un gasto o una transferencia, si la cuenta de origen no es de deuda y el monto supera lo que tiene disponible, ahora te avisa (sin bloquearte).
- La nota opcional de movimientos y transferencias ahora empieza siempre con mayúscula.
- Los desplegables de cuenta en movimientos y transferencias ahora muestran el color de la cuenta, para no confundirte al elegir.

## Arreglos
- Eliminar una cuenta con movimientos ya no la deja archivada con opción de restaurarla — se elimina directo, y sus movimientos y transferencias siguen intactos igual que antes.
- En el gráfico de gasto por día, un toque solo resalta la barra en vez de navegar de una — se hacía molesto con un solo toque accidental.
