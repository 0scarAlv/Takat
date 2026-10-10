# Takat v3.1.0

_Incluye todos los cambios desde v2.4.1 (el último Release publicado en GitHub)._

## v3.1.0 — Saldo a la fecha
- **Nuevo: Saldo a la fecha.** Abrí cualquier movimiento y tocá **"Saldo a la fecha"** para ver cómo estaban tus cuentas justo después de ese movimiento, como el saldo de un estado de cuenta del banco.
- Es una página propia con:
  - El **saldo antes y después** del movimiento, y cuánto cambió.
  - Una **gráfica de cómo fue cambiando el saldo** día a día hasta ese momento (tocala para ver el saldo de cualquier día).
  - **Ingresos y gastos acumulados** desde el inicio, con la diferencia entre ambos.
  - **Ingresos contra gastos por mes** hasta esa fecha.
  - **En qué categorías se fue el dinero** hasta ese momento.
- Podés verlo **por cuenta** o de **todas las cuentas juntas** (capital, deuda y neto de ese momento).
- Las **transferencias entre cuentas** se muestran aparte (recibidas, enviadas y cuánto se movió en total), porque no son ingreso ni gasto: solo cambian el dinero de cuenta.
- El **botón atrás del teléfono** en Presupuesto o Estadísticas ahora te lleva a Inicio en vez de cerrar la app.
- Arreglo: tocar la flecha de atrás dos veces rápido ya no deja la pantalla en blanco.

## v3.0.1
- Los textos de los botones ya no se parten en dos líneas ni se cortan en pantallas angostas o con letra grande: se achican para caber.
- Los botones Foto, Galería y Archivo al adjuntar comprobantes ahora entran bien los tres en una fila.
- En Estadísticas, **Gasto por día** se ordenó mejor: el selector Mes / Semana va arriba, y la fecha y el monto debajo sin partirse. Los números de los días ya no se cortan.

## v3.0.0 — Diseño nuevo
- **Takat se ve distinto:** tipografía nueva, esquinas más suaves y un inicio rediseñado de punta a punta.
- El **saldo disponible** ahora va en una tarjeta grande arriba de todo, con el capital, el gasto fijo del período y la deuda debajo. Si quedás en negativo, la tarjeta entera se pone roja (salvo con los montos ocultos, para no delatarlo).
- Tus **cuentas** aparecen juntas en un solo bloque, cada una con su color y su inicial.
- Los **movimientos** se agrupan por día: Hoy, Ayer, Lunes 28 de septiembre…
- **Gastos fijos** te dice de un vistazo cuántos quedan pendientes y cuánto suman, sin tener que desplegarlo. Los pagos parciales muestran una barra de progreso.
- **Ajustes reorganizado:** todo en grupos cortos con una línea de resumen (por ejemplo, cuándo fue tu último respaldo). El respaldo, el acceso desde PC y la depuración tienen su propia pantalla.
- **¿Preferís cómo se veía antes?** En Ajustes → Apariencia → Diseño podés elegir entre **Nuevo** y **Clásico** cuando quieras.

## v2.5.2
- Colores más legibles: los montos en verde, rojo y ámbar ahora se leen bien en modo claro, y en modo oscuro usan su versión clara.
- Se acabaron los toques de lila en los botones de opción, chips y barra de navegación: todo usa la paleta de Takat.
- En gastos fijos hay un botón "Pagar" aparte del interruptor, para que no se abra el pago por error al tocar la fila.
- Con los montos ocultos, el color ya no delata si estás en negativo.
- Botones más fáciles de tocar (ocultar montos, colores de cuenta, ver comprobante) y números alineados en columnas.

## v2.5.1
- Al registrar el sueldo de fin de mes, los gastos fijos mensuales pasan al mes siguiente en ese momento (igual que los quincenales), sin esperar al día 1.
- Cada gasto mensual respeta su quincena: uno que pagás en la primera (ej. día 6) aparece en cuanto cobrás; uno de la segunda (ej. día 20) espera al siguiente sueldo o al día 16.

## v2.5.0
- Nuevo: gastos fijos de tipo **Crédito**. Cargá una compra con su monto, número de cuotas y tasa de interés: lo que debés se muestra aparte como "Deuda de crédito", y a tus gastos y tu disponible solo les afecta la cuota de cada período.
- Los tipos de gasto fijo ahora son Normal, Cuotas (deudas sencillas en partes iguales) y Crédito.
- La cuota se calcula sola con la tasa, y podés cambiarla por la que te cobra el banco.
- Nuevo ajuste: "Restar compras a crédito del disponible", por si preferís que se descuente todo lo que falta pagar.
- Restaurar una copia de seguridad ahora te deja elegir: agregar solo lo que falta (sin duplicar), reemplazar todo en limpio o agregar todo aunque se duplique. Si la restauración falla, tus datos quedan intactos.
- Las copias de seguridad ahora guardan qué pagos corresponden a cada gasto fijo, así al restaurar no se pierde cuánto llevás pagado de una deuda.

---

**Instalación:** descargá `Takat-v3.1.0.apk` de abajo e instalalo encima de la versión que tengas; tus datos se conservan.
