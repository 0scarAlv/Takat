package com.takat.finanzas.util

data class ChangelogEntry(
    val versionCode: Int,
    val versionName: String,
    val changes: List<String>
)

/**
 * Full release history, keyed by [ChangelogEntry.versionCode] (matches `versionCode` in build.gradle.kts).
 * Shown to the user as a "qué hay de nuevo" dialog after an update — see WhatsNewDialog.kt. Reconstructed
 * from git history when the feature was added, so early low-value entries are terse on purpose.
 */
object Changelog {
    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(1, "0.1", listOf(
            "Arranca el proyecto Takat."
        )),
        ChangelogEntry(2, "0.2", listOf(
            "Primera versión funcional: cuentas, movimientos, transferencias y categorías.",
            "Inicio con header compacto, navegación por mes, detalle por categoría y gráficos de ingresos/gastos y torta.",
            "Exportar e importar movimientos en CSV para respaldo o migrar de dispositivo.",
            "Arreglos de teclado tapando los campos al crear movimientos o categorías."
        )),
        ChangelogEntry(3, "0.3.5", listOf(
            "Adjuntar foto, PDF o JSON como comprobante de un movimiento.",
            "El respaldo ahora exporta todo (incluidos los comprobantes) en un solo zip.",
            "Arreglo: comprobantes que no aparecían en el detalle por categoría."
        )),
        ChangelogEntry(4, "1.1.5", listOf(
            "Widget de accesos rápidos para la pantalla de inicio.",
            "Adjuntar varias imágenes de la galería a la vez.",
            "APK firmado con clave de release en vez de la clave de debug."
        )),
        ChangelogEntry(5, "1.2.0", listOf(
            "Nuevo: Presupuesto diario, que reparte el saldo disponible entre los días que faltan para el próximo pago.",
            "Widget más grande y legible, y abre Inicio al tocarlo.",
            "Easter egg y créditos en Ajustes."
        )),
        ChangelogEntry(6, "1.5.0", listOf(
            "Nuevo módulo de Gastos fijos: registrá pagos recurrentes (quincenales o mensuales), con pagos parciales y recordatorios.",
            "Compartir una imagen, PDF o JSON desde otra app directo a Takat para crear un movimiento.",
            "Seleccionar varios archivos o imágenes a la vez al adjuntar."
        )),
        ChangelogEntry(7, "1.6.5", listOf(
            "Compartir para crear movimiento ahora soporta elegir varios archivos a la vez.",
            "Presupuesto diario con 3 valores: el valor diario (se recalcula con cada movimiento), un presupuesto diario fijo (se congela solo al pasar la medianoche) y gastado hoy / disponible hoy.",
            "Nuevo widget de presupuesto diario (gastado, presupuesto y disponible de hoy).",
            "Nuevo widget compacto de accesos rápidos.",
            "Tema claro / oscuro / sistema, elegible desde Ajustes.",
            "Home: pestañas con nombre (Presupuesto / Inicio / Estadísticas) en vez de puntos.",
            "Gastos fijos: cada pendiente indica si es Quincenal o Mensual en vez de un total mezclado.",
            "Widgets: números centrados y más grandes, y fondo más oscuro en modo claro para pantallas OLED.",
            "Arreglo: el monto de un movimiento ya no se deforma cuando la nota es muy larga.",
            "Arreglo: \"Gastado hoy\" ya no contaba dos veces los pagos de gastos fijos."
        )),
        ChangelogEntry(8, "1.7.0", listOf(
            "El gráfico de gastos ahora es una dona, y las categorías usan íconos en vez de emoji.",
            "El ícono de la notificación ahora usa el logo de Takat.",
            "Respaldo automático diario a una carpeta elegida por vos.",
            "Nuevo interruptor en Ajustes para apagar los mensajes sarcásticos."
        )),
        ChangelogEntry(9, "1.7.1", listOf(
            "Arreglo: Presupuesto diario no se actualizaba el mismo día al cambiar la base, el período o el día de pago."
        )),
        ChangelogEntry(10, "1.7.2", listOf(
            "Arreglo: Valor diario ya no le daba todo el saldo al día anterior al pago dejando el día 15 (o el último día del mes) sin nada — ahora cada día, incluido el de pago, tiene su propio colchón por si el pago llega tarde.",
            "Nuevo ícono de categoría: Plaga.",
            "Nuevo: esta pantalla de novedades, que muestra los cambios de cada actualización."
        )),
        ChangelogEntry(11, "1.8.0", listOf(
            "Nuevo: marcá una categoría de ingreso como \"Es mi salario\" para que un ingreso ahí inicie la quincena de inmediato, sin esperar a que el calendario llegue al día 16 o 1.",
            "Arreglo: el presupuesto diario a veces no se congelaba a medianoche hasta abrir la app — ahora se fuerza con una tarea en segundo plano programada justo después de medianoche, más confiable que el refresco automático del widget.",
            "Nuevo en Ajustes → Depuración: grabar un registro de la app (apagado por defecto) y compartirlo, para poder diagnosticar un problema que solo pasa en otro teléfono."
        )),
        ChangelogEntry(12, "1.8.1", listOf(
            "Arreglo: el respaldo (manual y el automático diario) ahora incluye los pagos programados (gastos fijos) — antes no se guardaban."
        )),
        ChangelogEntry(13, "1.9.0", listOf(
            "Rediseño de navegación: Presupuesto / Inicio / Estadísticas ahora son pestañas fijas abajo.",
            "Nuevo botón flotante \"+\" para agregar un movimiento o una transferencia.",
            "Agregar una cuenta ahora se hace desde el \"+\" junto a \"Cuentas\" en Inicio.",
            "Nuevo: marcá un gasto fijo como \"pago de deuda\" indicando el monto total y la cantidad de cuotas — la cuota queda fija y Takat te avisa cuánto te falta pagar en total.",
            "Nuevo: elegir la fecha de un movimiento en vez de usar siempre la de hoy.",
            "El recordatorio de un gasto fijo ahora avisa lo que realmente falta pagar ese período, no el monto completo."
        )),
        ChangelogEntry(14, "1.9.1", listOf(
            "Nuevo acceso rápido en el panel de ajustes rápidos del sistema para crear un movimiento sin abrir la app ni usar un widget."
        )),
        ChangelogEntry(15, "1.9.2", listOf(
            "El ícono del acceso rápido en ajustes rápidos ahora son las flechas de movimiento en vez del \"+\".",
            "Nuevo botón de info junto a Ajustes en Inicio para ver las novedades cuando quieras, por si el aviso automático no aparece."
        )),
        ChangelogEntry(16, "1.10.0", listOf(
            "Primer paso de Acceso desde PC: activalo en Ajustes y vinculá una PC en tu misma wifi escaneando un código QR — sin servidor externo, todo cifrado de punta a punta entre el teléfono y el navegador.",
            "Todavía en construcción: por ahora solo se puede vincular; el panel con tus cuentas y movimientos llega en una próxima actualización."
        )),
        ChangelogEntry(17, "1.10.1", listOf(
            "Arreglo: vincular una PC fallaba con \"Cannot read properties of undefined (reading 'generateKey')\" porque el navegador no da cifrado nativo en direcciones de red local sin https — ahora Takat trae su propia librería de cifrado, así que sigue funcionando igual de simple."
        )),
        ChangelogEntry(18, "1.10.2", listOf(
            "Arreglo: el interruptor de \"Acceso desde PC\" en Ajustes aparecía apagado al volver a entrar, aunque el servicio siguiera activo de verdad."
        )),
        ChangelogEntry(19, "1.11.0", listOf(
            "El panel de PC ya tiene un tablero real: cuentas y saldos, disponible total, movimientos recientes y agregar/eliminar un movimiento — todo desde el navegador, sincronizado al instante con el teléfono."
        )),
        ChangelogEntry(20, "1.11.1", listOf(
            "Panel de PC: rediseño del tablero, \"Disponible\" ahora usa el mismo cálculo que la app (antes era una suma simple e ignoraba deuda y gastos fijos pendientes), soporte para transferencias, movimientos agrupados por día.",
            "Nuevo en Ajustes: revocar una PC vinculada, y un apodo opcional para distinguir tu teléfono en la red (se anuncia por mDNS, aunque en Windows casi siempre vas a necesitar igual la dirección IP)."
        )),
        ChangelogEntry(21, "1.11.2", listOf(
            "Acceso desde PC: botón para copiar la dirección al portapapeles, para pasarla fácil por WhatsApp o donde sea."
        )),
        ChangelogEntry(22, "1.12.0", listOf(
            "Panel de PC: rediseño para pantalla de escritorio (barra lateral con cuentas + disponible, tabla de movimientos en vez de tarjetas apiladas).",
            "Nuevo: botón \"Exportar CSV\" en el panel de PC para bajar tus datos y manipularlos en Excel."
        )),
        ChangelogEntry(23, "1.12.1", listOf(
            "Panel de PC: los desplegables del formulario de nuevo movimiento ya no se ven con el estilo por defecto del navegador."
        )),
        ChangelogEntry(24, "1.12.2", listOf(
            "Panel de PC: nombre \"Takat\" más grande arriba, e ícono de Takat en la pestaña del navegador."
        )),
        ChangelogEntry(25, "1.13.0", listOf(
            "Arreglo: Ingresos vs Gastos ya no cuenta un sueldo pagado en la segunda mitad del mes (por ejemplo el día 31) como ingreso de ese mes — ahora se atribuye al mes que realmente financia, igual que ya hacía Presupuesto/Gastos fijos con la quincena."
        )),
        ChangelogEntry(26, "1.14.0", listOf(
            "Nuevo: como Takat no está en ninguna tienda de apps, ahora se fija solo si hay una versión nueva publicada en GitHub cada vez que abrís la app, y te deja descargarla e instalarla sin salir de Takat.",
            "Nuevo botón \"Buscar actualizaciones\" en Ajustes → Acerca de, para revisar manualmente cuando quieras."
        )),
        ChangelogEntry(27, "2.0.0", listOf(
            "Cuando hay una actualización disponible, ahora aparece un botón verde \"Actualizar\" junto al título Takat en Inicio, para descargarla e instalarla cuando quieras aunque hayas cerrado el aviso automático."
        )),
        ChangelogEntry(28, "2.1.0", listOf(
            "Eliminar una cuenta con movimientos guardados ya no borra también sus movimientos y transferencias: quedan intactos, aunque la cuenta desaparezca.",
            "Al cargar un gasto o una transferencia, si la cuenta de origen no es de deuda y el monto supera lo que tiene disponible, ahora te avisa (sin bloquearte).",
            "La nota opcional de movimientos y transferencias ahora empieza siempre con mayúscula.",
            "Los desplegables de cuenta en movimientos y transferencias ahora muestran el color de la cuenta, para no confundirte al elegir."
        )),
        ChangelogEntry(29, "2.1.1", listOf(
            "Ajuste: eliminar una cuenta con movimientos ya no la deja archivada con opción de restaurarla — se elimina directo, y sus movimientos y transferencias siguen intactos igual que antes."
        )),
        ChangelogEntry(30, "2.2.0", listOf(
            "Antes de instalar una actualización, Takat ahora revisa el respaldo automático: si está activo, hace una copia de seguridad antes de aplicar el cambio; si no está activo, te pregunta si querés activarlo (y te dice dónde) o seguir sin él.",
            "Nuevo gráfico de barras en Estadísticas: gasto por día del mes, con el día de mayor gasto marcado — tocá cualquier barra para ver el gasto de ese día."
        )),
        ChangelogEntry(31, "2.2.1", listOf(
            "En el gráfico de gasto por día, tocá el detalle del día seleccionado para ver todos sus movimientos, igual que ya podías hacer con una categoría."
        )),
        ChangelogEntry(32, "2.2.2", listOf(
            "En el gráfico de gasto por día, un toque solo resalta la barra; ahora hace falta doble toque para abrir el detalle del día (un toque solo se hacía molesto).",
            "Nuevo: ícono de ojo en Inicio para ocultar/mostrar Disponible, Capital total, Gasto fijo y Deuda total."
        )),
        ChangelogEntry(33, "2.2.3", listOf(
            "El ícono de ojo para ocultar montos ahora también oculta el saldo de cada cuenta en \"Cuentas\", no solo los 4 totales."
        )),
        ChangelogEntry(34, "2.3.0", listOf(
            "En la lista de movimientos, ahora la nota se muestra grande y la categoría chica (antes era al revés).",
            "Estadísticas: se sacó la lista de categorías individuales debajo del gráfico de dona (ocupaba mucho espacio) — ahora tocá una porción o su nombre en la leyenda para ver el detalle de esa categoría."
        )),
        ChangelogEntry(35, "2.3.1", listOf(
            "Nuevo gráfico en Estadísticas: gasto total de los últimos 6 meses debajo del gráfico de dona — tocá un mes para ver su detalle completo."
        )),
        ChangelogEntry(36, "2.3.2", listOf(
            "El gráfico de los últimos 6 meses ahora es de líneas en vez de barras."
        )),
        ChangelogEntry(37, "2.3.3", listOf(
            "Los desplegables de cuenta en movimientos y transferencias ahora muestran el saldo actual junto al nombre."
        )),
        ChangelogEntry(38, "2.4.0", listOf(
            "Nuevo: tocá \"Editar\" en el detalle de un movimiento para corregir la cuenta, el monto, la categoría, la fecha o la nota si te equivocaste al cargarlo."
        )),
        ChangelogEntry(39, "2.4.1", listOf(
            "Arreglo: el aviso de actualización disponible mostraba los símbolos de Markdown (##, -, etc.) tal cual en vez de texto formateado."
        ))
    )

    /** Entries strictly newer than [lastSeenVersionCode], oldest first. */
    fun entriesAfter(lastSeenVersionCode: Int): List<ChangelogEntry> =
        entries.filter { it.versionCode > lastSeenVersionCode }.sortedBy { it.versionCode }
}
