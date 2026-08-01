# Tutorial de Takat

## Instalación

1. Descargá el archivo `Takat.v1.2.0.apk`.
2. En tu teléfono Android, abrí el archivo descargado.
3. Si el sistema pide permiso para instalar apps de "origen desconocido", aceptalo (es normal al instalar fuera de Play Store).
4. Tocá "Instalar" y esperá a que termine.

Si ya tenías una versión anterior instalada con otra firma, el sistema puede pedirte desinstalarla antes de poder instalar esta.

## Estructura general

La app tiene una pantalla principal (Home) con tres secciones a las que se accede deslizando el dedo a los costados (o tocando los puntitos de arriba):

- **Presupuesto** (izquierda): ingresos vs. gastos del mes y el presupuesto diario.
- **Home** (centro, pantalla de inicio): tus totales, tus cuentas y tus últimas transacciones.
- **Estadísticas** (derecha): gastos por categoría del mes, con gráfico de torta.

Abajo de todo hay tres botones fijos: **Cuenta**, **Movimiento** y **Transferencia**. Arriba a la derecha hay un ícono de engranaje que abre **Ajustes**.

## Cuentas

### Crear una cuenta

Tocá "Cuenta" abajo. Completá:

- **Nombre**.
- **¿Es una deuda?**: "Tengo" para cuentas normales (efectivo, banco), "Debo" para deudas (tarjeta de crédito, préstamo). Si elegís "Debo", el saldo se resta de tu patrimonio total en vez de sumarse.
- **Sumar al resumen de arriba**: si lo apagás, la cuenta sigue viendo en la lista pero no cuenta para Disponible/Capital/Deuda (útil para cuentas que querés llevar aparte).
- **Con cuánto arranca**: el saldo inicial.
- **Color**: para identificarla en la lista.

### Ver y editar una cuenta

Tocando una cuenta desde Home entrás a su detalle: ves el saldo actual y la lista de movimientos de esa cuenta. El lápiz arriba a la derecha permite editar nombre, tipo, saldo y color; el botón "+" flotante abre directamente "Nuevo movimiento" con esa cuenta preseleccionada.

Al editar una cuenta también podés eliminarla desde el ícono de basura (esto borra también todos sus movimientos y transferencias asociados; la app pide confirmación antes).

## Movimientos (ingresos y gastos)

Tocá "Movimiento" abajo (o el "+" dentro de una cuenta). Completá:

- **Gasto o Ingreso** (selector arriba).
- **Cuenta** a la que pertenece.
- **Monto**.
- **Categoría**: elegí una existente tocando su emoji/nombre, o creá una nueva con el botón de agregar (nombre + emoji).
- **Nota** (opcional).
- **Comprobante** (opcional): podés adjuntar una foto tomada en el momento, una imagen de la galería, o un archivo PDF/JSON.

Tocá "Guardar" para registrarlo.

## Transferencias

Tocá "Transferencia" abajo. Elegí cuenta de origen ("Desde"), cuenta de destino ("Hacia"), el monto, un motivo opcional (categoría) y una nota. Tocá "Transferir". El monto se descuenta de la cuenta de origen y se suma a la de destino.

## Ver y borrar un movimiento o transferencia

Tocando cualquier fila en la lista de movimientos (en Home o dentro de una cuenta) se abre el detalle, con la opción de eliminarlo.

## Presupuesto (página izquierda)

- **Ingresos vs gastos**: balance del mes seleccionado, con navegación mes a mes (flechas junto al nombre del mes), y barras comparando ingresos y gastos.
- **Presupuesto diario**: activá el switch para habilitarlo.
  1. Elegí si tu ciclo de pago es "Quincena" (paga siempre el 15 y el último día del mes) o "Mes" (elegís vos el día; si ese día no existe en un mes dado, se usa el último día de ese mes).
  2. Elegí si el cálculo se hace sobre "Disponible" (capital menos deuda) o "Capital total".
  3. La app muestra cuánto podés gastar por día y cuántos días faltan para el próximo pago. Se recalcula solo cada vez que abrís la pantalla, no hace falta reiniciar nada.
  4. Tocando "Configuración" podés ocultar o volver a mostrar las opciones de período y base de cálculo una vez que ya las configuraste.

## Home (página central)

- Tarjeta de totales: **Disponible**, **Capital total** y **Deuda total**, sumando solo las cuentas que tienen activado "Sumar al resumen".
- Lista de tus cuentas con su saldo (tocá cualquiera para ver su detalle).
- Lista de "Transacciones": tus movimientos y transferencias más recientes de todas las cuentas.
- Tocando el título de arriba ("Takat"/"Presupuesto"/"Estadísticas") 5 veces seguidas aparece un mensaje secreto ("Esteregg").

## Estadísticas (página derecha)

- Gastos por categoría del mes seleccionado (con el mismo navegador de mes que Presupuesto).
- Gráfico de torta con el porcentaje de cada categoría (las categorías menores se agrupan en "Otros" si hay muchas).
- Lista de categorías con su monto y porcentaje; tocando una categoría se abre el detalle con todos los gastos de esa categoría en ese mes.

## Ajustes

Se accede desde el ícono de engranaje arriba a la derecha en Home.

- **Exportar datos**: genera un archivo .zip con todas tus cuentas, categorías, movimientos, transferencias y comprobantes adjuntos, para guardarlo como respaldo o pasarlo a otro teléfono.
- **Importar datos**: carga un .zip exportado antes. Las cuentas y categorías que ya existan con el mismo nombre se reutilizan, pero los movimientos y transferencias siempre se agregan como nuevos (si importás el mismo archivo dos veces, se duplican).
- Número de versión de la app y derechos reservados, al final de la pantalla.

## Widget de inicio

Podés agregar el widget de Takat a tu pantalla de inicio de Android (mantené presionado un espacio vacío de la pantalla, elegí "Widgets" y buscá "Takat"). Muestra Líquido, Deuda y Actual, y tiene dos íconos para cargar directamente un movimiento nuevo o una transferencia nueva sin tener que abrir la app entera. Tocando los números del widget se abre la app directo en Home.
