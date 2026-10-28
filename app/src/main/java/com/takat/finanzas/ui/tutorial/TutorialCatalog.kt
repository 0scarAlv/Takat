package com.takat.finanzas.ui.tutorial

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.WavingHand
import com.takat.finanzas.ui.navigation.Routes

/** Every guide in the app, in the order Ajustes > Guías lists them. */
object TutorialCatalog {
    const val WELCOME = "welcome"
    const val FIRST_ACCOUNT = "first-account"
    const val MOVEMENTS = "movements"
    const val TRANSFERS = "transfers"
    const val FIXED_EXPENSES = "fixed-expenses"
    const val EDIT_OR_DELETE = "edit-or-delete"

    private val welcome = TutorialGuide(
        id = WELCOME,
        title = "Bienvenida",
        summary = "Qué significa cada número del inicio",
        icon = Icons.Filled.WavingHand,
        followUpId = FIRST_ACCOUNT,
        steps = listOf(
            TutorialStep(
                screen = Routes.HOME,
                title = "¡Bienvenido a Takat!",
                body = "Takat te ayuda a saber cuánta plata tenés de verdad y cuánto podés gastar sin quedarte corto. " +
                    "Primero te explico qué significa cada número de esta pantalla."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_HERO,
                title = "Disponible",
                body = "Es el número más importante: la plata que realmente podés usar. Se calcula así:\n\n" +
                    "Capital total − Deuda total − Gasto fijo pendiente\n\n" +
                    "Si sale negativo, debés más de lo que tenés. Al principio todo está en 0: se llena cuando creás " +
                    "tus cuentas. Con el ojito ocultás los montos si alguien está mirando."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_CAPITAL,
                title = "Capital total",
                body = "La suma de todo lo que TENÉS: efectivo, tarjeta de débito y cualquier cuenta marcada como " +
                    "\"Tengo\". Solo cuentan las cuentas que tienen encendido \"Sumar al resumen\"."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_FIXED,
                optional = true,
                title = "Gasto fijo este período",
                body = "Lo que todavía te falta pagar este mes o quincena de tus gastos fijos (alquiler, luz, " +
                    "internet, cuotas). Takat lo aparta de tu Disponible antes de que lo pagues, para que no lo gastes " +
                    "en otra cosa. Esta línea solo aparece cuando tenés gastos fijos pendientes."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_DEBT,
                title = "Deuda total",
                body = "Todo lo que DEBÉS en cuentas marcadas como \"Debo\", como la tarjeta de crédito. Acá se muestra " +
                    "como el monto que debés; en tu lista de cuentas la tarjeta aparece en negativo, y es normal. " +
                    "Si no debés nada, queda en 0."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_NAV,
                title = "Tres pantallas",
                body = "Presupuesto te dice cuánto podés gastar por día, Inicio es este resumen y Estadísticas muestra " +
                    "en qué se te va la plata. También podés deslizar a los costados."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_FAB,
                title = "El botón +",
                body = "Desde acá registrás gastos, ingresos y transferencias entre tus cuentas."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_SETTINGS,
                title = "Ajustes",
                body = "Respaldo automático, gastos fijos, categorías y el tema de la app. En Ajustes › Ayuda › Guías " +
                    "podés repetir estos tutoriales cuando quieras."
            ),
            TutorialStep(
                screen = Routes.HOME,
                title = "Empecemos",
                body = "Para que Takat sirva necesitás tus cuentas: el efectivo, las tarjetas y el ahorro. " +
                    "¿Las armamos juntos?",
                primaryLabel = "Crear mis cuentas"
            )
        )
    )

    private val firstAccount = TutorialGuide(
        id = FIRST_ACCOUNT,
        title = "Tus cuentas",
        summary = "Las 4 cuentas básicas y cómo crearlas",
        icon = Icons.Filled.AccountBalanceWallet,
        followUpId = MOVEMENTS,
        steps = listOf(
            TutorialStep(
                screen = Routes.HOME,
                title = "Las cuentas básicas",
                body = "Una cuenta es cualquier lugar donde tenés (o debés) plata. Te recomendamos empezar con 4:\n\n" +
                    "• Efectivo\n• Tarjeta de débito\n• Tarjeta de crédito\n• Ahorro\n\n" +
                    "Te explico cada una y después creamos la primera juntos."
            ),
            TutorialStep(
                screen = Routes.HOME,
                title = "Efectivo y tarjeta de débito",
                body = "Las dos son plata tuya, así que van como \"Tengo\". Arrancan con lo que tenés hoy: lo que hay " +
                    "en tu billetera y lo que tenés en el banco. Cuando pagás con débito, el gasto se registra en esa cuenta."
            ),
            TutorialStep(
                screen = Routes.HOME,
                title = "Tarjeta de crédito",
                body = "Es plata que le debés al banco, así que va como \"Debo\". Arranca en 0 si hoy no debés nada " +
                    "(o con lo que debés, si ya la usaste).\n\n" +
                    "Cada compra con la tarjeta la hace más negativa: por ser deuda SIEMPRE la vas a ver en negativo, y " +
                    "eso es lo que le debés al banco. Cuando la pagás, hacé una Transferencia desde tu débito hacia la " +
                    "tarjeta y vuelve hacia 0.\n\n" +
                    "¿Tu tarjeta tiene un límite, por ejemplo $500? Ponelo al crearla y Takat te muestra cuánto llevás " +
                    "gastado y cuánto te queda. Solo suma lo que compraste con ella: no calcula intereses."
            ),
            TutorialStep(
                screen = Routes.HOME,
                title = "Ahorro",
                body = "Es plata tuya (\"Tengo\"), pero te recomendamos apagarle \"Sumar al resumen\": así no se " +
                    "mezcla con tu Disponible y no la gastás sin darte cuenta. La seguís viendo en tu lista de cuentas."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_ADD_ACCOUNT,
                advance = Advance.TapAnchor,
                title = "Creemos la primera",
                body = "Empecemos por el Efectivo. Tocá el + de Cuentas."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_NEW,
                anchor = TutorialAnchors.ACCOUNT_NAME,
                touchable = true,
                title = "Ponele nombre",
                body = "Escribí \"Efectivo\" (o el nombre de la cuenta que estés creando). Tocá el campo, escribilo y " +
                    "después Siguiente."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_NEW,
                anchor = TutorialAnchors.ACCOUNT_DEBT,
                touchable = true,
                title = "¿Tengo o Debo?",
                body = "\"Tengo\" para efectivo, débito y ahorro. \"Debo\" solo para la tarjeta de crédito o un " +
                    "préstamo: su saldo se resta de lo que tenés."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_NEW,
                anchor = TutorialAnchors.ACCOUNT_INCLUDE,
                touchable = true,
                title = "Sumar al resumen",
                body = "Dejalo encendido para efectivo, débito y tarjeta de crédito, así cuentan en tu Disponible. " +
                    "Apagalo para el Ahorro."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_NEW,
                anchor = TutorialAnchors.ACCOUNT_AMOUNT,
                touchable = true,
                title = "¿Con cuánto arranca?",
                body = "Escribí cuánto tiene hoy esa cuenta; si no sabés exacto, poné un aproximado y lo corregís " +
                    "después. Para la tarjeta de crédito poné 0, o lo que debés escrito en positivo: Takat la pone en " +
                    "negativo solo por ser \"Debo\"."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_NEW,
                anchor = TutorialAnchors.ACCOUNT_LIMIT,
                touchable = true,
                optional = true,
                title = "Límite de la tarjeta",
                body = "Si es tu tarjeta de crédito, poné su límite (por ejemplo 500). Debajo de la cuenta vas a ver " +
                    "una barra con lo que llevás gastado y cuánto te queda. Si no tiene límite o no lo sabés, dejalo vacío."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_NEW,
                anchor = TutorialAnchors.ACCOUNT_SAVE,
                advance = Advance.Event(TutorialEvents.ACCOUNT_CREATED),
                title = "Guardala",
                body = "Tocá Guardar para crear la cuenta."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_ACCOUNTS,
                title = "¡Primera cuenta lista!",
                body = "Ahora creá las demás con el mismo + de Cuentas: Tarjeta de débito, Tarjeta de crédito y Ahorro. " +
                    "Tocá cualquier cuenta para ver sus movimientos o editarla.\n\n" +
                    "Ahora te muestro cómo registrar lo que gastás.",
                primaryLabel = "Seguir: movimientos"
            )
        )
    )

    private val movements = TutorialGuide(
        id = MOVEMENTS,
        title = "Movimientos",
        summary = "Registrar un gasto o un ingreso",
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        minAccounts = 1,
        followUpId = TRANSFERS,
        steps = listOf(
            TutorialStep(
                screen = Routes.HOME,
                title = "¿Qué es un movimiento?",
                body = "Cada vez que entra o sale plata de una de tus cuentas: un gasto (comida, pasaje, una compra " +
                    "con la tarjeta) o un ingreso (tu sueldo, un pago que te hicieron). Si lo registrás siempre, el " +
                    "saldo de Takat coincide con la realidad."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_FAB,
                advance = Advance.TapAnchor,
                title = "El botón +",
                body = "Tocá el + para ver las opciones."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_FAB_MOVEMENT,
                advance = Advance.TapAnchor,
                title = "Movimiento",
                body = "Tocá \"Movimiento\"."
            ),
            TutorialStep(
                screen = Routes.TRANSACTION_NEW,
                anchor = TutorialAnchors.TX_TYPE,
                touchable = true,
                title = "¿Gasto o ingreso?",
                body = "\"Gasto\" si salió plata, \"Ingreso\" si te entró."
            ),
            TutorialStep(
                screen = Routes.TRANSACTION_NEW,
                anchor = TutorialAnchors.TX_ACCOUNT,
                touchable = true,
                title = "Cuenta",
                body = "De dónde salió (o a dónde entró) la plata. Si pagaste con la tarjeta de crédito, elegí la " +
                    "tarjeta: así ves cuánto llevás gastado en ella y cuánto te queda de su límite."
            ),
            TutorialStep(
                screen = Routes.TRANSACTION_NEW,
                anchor = TutorialAnchors.TX_AMOUNT,
                touchable = true,
                title = "Monto",
                body = "Cuánto fue. Si la cuenta no tiene suficiente, o te pasás del límite de la tarjeta, Takat te avisa."
            ),
            TutorialStep(
                screen = Routes.TRANSACTION_NEW,
                anchor = TutorialAnchors.TX_CATEGORY,
                touchable = true,
                title = "Categoría",
                body = "En qué se fue la plata. Sirve para que Estadísticas te muestre en qué gastás más. Si no está " +
                    "la que querés, creala con el +. Abajo podés agregar una nota o la foto del comprobante."
            ),
            TutorialStep(
                screen = Routes.TRANSACTION_NEW,
                anchor = TutorialAnchors.TX_SAVE,
                advance = Advance.Event(TutorialEvents.TRANSACTION_CREATED),
                title = "Guardalo",
                body = "Tocá Guardar."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_ACCOUNTS,
                title = "¡Registrado!",
                body = "El saldo de la cuenta ya cambió y el movimiento aparece abajo, en Movimientos, agrupado por " +
                    "día. Si te equivocaste, tocalo para editarlo o eliminarlo.",
                primaryLabel = "Seguir: transferencias"
            )
        )
    )

    private val transfers = TutorialGuide(
        id = TRANSFERS,
        title = "Transferencias",
        summary = "Mover plata entre tus cuentas y pagar la tarjeta",
        icon = Icons.Filled.SwapHoriz,
        minAccounts = 2,
        followUpId = FIXED_EXPENSES,
        steps = listOf(
            TutorialStep(
                screen = Routes.HOME,
                title = "¿Qué es una transferencia?",
                body = "Mover plata de una cuenta tuya a otra. NO es un gasto: la plata sigue siendo tuya, solo " +
                    "cambia de lugar, por eso no aparece en Estadísticas. Por ejemplo:\n\n" +
                    "• Sacar del cajero: de Tarjeta de débito a Efectivo.\n" +
                    "• Ahorrar: de Tarjeta de débito a Ahorro.\n" +
                    "• Pagar la tarjeta de crédito: de Tarjeta de débito a Tarjeta de crédito."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_FAB,
                advance = Advance.TapAnchor,
                title = "El botón +",
                body = "Tocá el + otra vez."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_FAB_TRANSFER,
                advance = Advance.TapAnchor,
                title = "Transferencia",
                body = "Tocá \"Transferencia\"."
            ),
            TutorialStep(
                screen = Routes.TRANSFER_NEW,
                anchor = TutorialAnchors.TRANSFER_FROM,
                touchable = true,
                title = "Desde",
                body = "La cuenta de donde sale la plata."
            ),
            TutorialStep(
                screen = Routes.TRANSFER_NEW,
                anchor = TutorialAnchors.TRANSFER_TO,
                touchable = true,
                title = "Hacia",
                body = "La cuenta a donde llega. Para pagar la tarjeta de crédito, elegila acá: lo que le debés baja " +
                    "y vuelve hacia 0."
            ),
            TutorialStep(
                screen = Routes.TRANSFER_NEW,
                anchor = TutorialAnchors.TRANSFER_AMOUNT,
                touchable = true,
                title = "Monto",
                body = "Cuánto movés."
            ),
            TutorialStep(
                screen = Routes.TRANSFER_NEW,
                anchor = TutorialAnchors.TRANSFER_SAVE,
                advance = Advance.Event(TutorialEvents.TRANSFER_CREATED),
                title = "Transferí",
                body = "Tocá Transferir."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_ACCOUNTS,
                title = "¡Listo!",
                body = "Una cuenta bajó y la otra subió lo mismo, pero tu Disponible no cambió: la plata sigue " +
                    "siendo tuya.\n\nLo último que conviene entender son los gastos fijos (alquiler, luz, cuotas), " +
                    "porque sí cambian tu Disponible.",
                primaryLabel = "Seguir: gastos fijos"
            )
        )
    )

    private val fixedExpenses = TutorialGuide(
        id = FIXED_EXPENSES,
        title = "Gastos fijos",
        summary = "Qué son, por qué bajan tu Disponible y cómo pagarlos",
        icon = Icons.Filled.EventRepeat,
        minAccounts = 1,
        steps = listOf(
            TutorialStep(
                screen = Routes.HOME,
                title = "¿Qué es un gasto fijo?",
                body = "Algo que pagás siempre, cada mes o cada quincena: alquiler, luz, agua, internet, el celular, " +
                    "una cuota. Lo registrás una sola vez y Takat lo tiene en cuenta en cada período."
            ),
            TutorialStep(
                screen = Routes.HOME,
                title = "Por qué baja tu Disponible",
                body = "Esto es lo que más confunde: apenas empieza el período, Takat resta el gasto fijo de tu " +
                    "Disponible aunque todavía no lo hayas pagado.\n\n" +
                    "No es que la plata se gastó: la está apartando para que no la uses en otra cosa. Cuando lo pagás, " +
                    "la plata sale de tu cuenta y el gasto deja de estar pendiente, así que tu Disponible no baja dos veces."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_SETTINGS,
                advance = Advance.TapAnchor,
                title = "Vamos a crear uno",
                body = "Los gastos fijos se administran desde Ajustes. Tocá el engranaje."
            ),
            TutorialStep(
                screen = Routes.SETTINGS,
                anchor = TutorialAnchors.SETTINGS_FIXED,
                advance = Advance.TapAnchor,
                title = "Gastos fijos",
                body = "Tocá \"Gastos fijos\"."
            ),
            TutorialStep(
                screen = Routes.FIXED_EXPENSES,
                anchor = TutorialAnchors.FIXED_ADD,
                advance = Advance.TapAnchor,
                title = "Nuevo gasto fijo",
                body = "Acá ves todos tus gastos fijos. Tocá el + para agregar uno."
            ),
            TutorialStep(
                screen = Routes.FIXED_EXPENSE_FORM,
                anchor = TutorialAnchors.FIXED_NAME,
                touchable = true,
                title = "Nombre",
                body = "Por ejemplo \"Alquiler\", \"Luz\" o \"Internet\"."
            ),
            TutorialStep(
                screen = Routes.FIXED_EXPENSE_FORM,
                anchor = TutorialAnchors.FIXED_KIND,
                touchable = true,
                title = "Tipo",
                body = "• Normal: un pago que se repite siempre, como el alquiler o internet.\n" +
                    "• Cuotas: una deuda que pagás en partes iguales hasta terminarla, como un préstamo.\n" +
                    "• Crédito: algo que compraste a cuotas con la tarjeta, como un celular a 12 meses.\n\n" +
                    "Si no estás seguro, dejá Normal."
            ),
            TutorialStep(
                screen = Routes.FIXED_EXPENSE_FORM,
                anchor = TutorialAnchors.FIXED_AMOUNT,
                touchable = true,
                title = "Monto",
                body = "Cuánto pagás cada vez. Si cambia un poco cada mes (como la luz), poné un promedio: al pagarlo " +
                    "registrás lo que realmente pagaste."
            ),
            TutorialStep(
                screen = Routes.FIXED_EXPENSE_FORM,
                anchor = TutorialAnchors.FIXED_ACCOUNT,
                touchable = true,
                title = "Cuenta",
                body = "De qué cuenta sale la plata cuando lo pagás, por ejemplo tu tarjeta de débito."
            ),
            TutorialStep(
                screen = Routes.FIXED_EXPENSE_FORM,
                anchor = TutorialAnchors.FIXED_FREQUENCY,
                touchable = true,
                title = "Frecuencia",
                body = "Mensual: una vez al mes, el día que elijas abajo. Quincenal: dos veces al mes, en la primera y " +
                    "en la segunda quincena."
            ),
            TutorialStep(
                screen = Routes.FIXED_EXPENSE_FORM,
                anchor = TutorialAnchors.FIXED_NOTIFY,
                touchable = true,
                title = "Recordatorio",
                body = "Encendelo si querés que Takat te avise cuando se acerca el día de pago."
            ),
            TutorialStep(
                screen = Routes.FIXED_EXPENSE_FORM,
                anchor = TutorialAnchors.FIXED_SAVE,
                advance = Advance.Event(TutorialEvents.FIXED_EXPENSE_CREATED),
                title = "Guardalo",
                body = "Tocá Guardar."
            ),
            TutorialStep(
                screen = Routes.FIXED_EXPENSES,
                title = "¿Y cómo lo pago?",
                body = "En Inicio vas a ver una tarjeta \"Gastos fijos\" con lo que tenés pendiente. Cuando lo pagués, " +
                    "tocá \"Pagar\": se registra el gasto en la cuenta y deja de estar pendiente.\n\n" +
                    "Si un mes no te toca pagarlo, apagá su interruptor en esa tarjeta: se salta solo ese período y " +
                    "deja de restarse de tu Disponible.",
                primaryLabel = "Terminar"
            )
        )
    )

    private val editOrDelete = TutorialGuide(
        id = EDIT_OR_DELETE,
        title = "Corregir o eliminar",
        summary = "Editar un movimiento, una cuenta o borrarlos",
        icon = Icons.Filled.Edit,
        minAccounts = 1,
        steps = listOf(
            TutorialStep(
                screen = Routes.HOME,
                title = "Todo se puede corregir",
                body = "Si te equivocaste en un monto, una cuenta o una categoría, no hace falta empezar de nuevo. " +
                    "Te muestro dónde se arregla cada cosa."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_FIRST_MOVEMENT,
                optional = true,
                title = "Movimientos",
                body = "Tocando cualquier movimiento se abre su detalle, con Editar y Eliminar. Las transferencias " +
                    "solo se pueden eliminar: si una quedó mal, eliminala y hacela de nuevo."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_ACCOUNTS,
                advance = Advance.TapAnchor,
                title = "Cuentas",
                body = "Para corregir una cuenta, tocala."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_DETAIL,
                anchor = TutorialAnchors.ACCOUNT_DETAIL_EDIT,
                advance = Advance.TapAnchor,
                title = "Editar cuenta",
                body = "Acá ves el saldo y todos sus movimientos. Tocá el lápiz para editarla."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_EDIT,
                anchor = TutorialAnchors.ACCOUNT_AMOUNT,
                title = "Saldo inicial",
                body = "Podés cambiar nombre, tipo, color o el límite de la tarjeta. Si el saldo no coincide con la " +
                    "realidad, corregí el Saldo inicial: todos los movimientos se recalculan a partir de él."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_EDIT,
                anchor = TutorialAnchors.ACCOUNT_DELETE,
                title = "Eliminar",
                body = "La papelera elimina la cuenta; Takat te pide confirmación antes. Si la cuenta ya tenía " +
                    "movimientos, deja de aparecer pero esos movimientos se mantienen en tu historial.",
                primaryLabel = "Terminar"
            )
        )
    )

    val guides: List<TutorialGuide> = listOf(welcome, firstAccount, movements, transfers, fixedExpenses, editOrDelete)

    fun byId(id: String): TutorialGuide? = guides.firstOrNull { it.id == id }
}

/** Ids passed to [tutorialAnchor]; kept in one place so a typo can't silently break a guide. */
object TutorialAnchors {
    const val HOME_HERO = "home.hero"
    const val HOME_CAPITAL = "home.capital"
    const val HOME_FIXED = "home.fixed"
    const val HOME_DEBT = "home.debt"
    const val HOME_NAV = "home.nav"
    const val HOME_FAB = "home.fab"
    const val HOME_SETTINGS = "home.settings"
    const val HOME_ADD_ACCOUNT = "home.addAccount"
    const val HOME_ACCOUNTS = "home.accounts"
    const val ACCOUNT_NAME = "account.name"
    const val ACCOUNT_DEBT = "account.debt"
    const val ACCOUNT_INCLUDE = "account.include"
    const val ACCOUNT_AMOUNT = "account.amount"
    const val ACCOUNT_SAVE = "account.save"
    const val ACCOUNT_LIMIT = "account.limit"
    const val ACCOUNT_DELETE = "account.delete"
    const val ACCOUNT_DETAIL_EDIT = "accountDetail.edit"
    const val HOME_FAB_MOVEMENT = "home.fab.movement"
    const val HOME_FAB_TRANSFER = "home.fab.transfer"
    const val HOME_FIRST_MOVEMENT = "home.firstMovement"
    const val TX_TYPE = "tx.type"
    const val TX_ACCOUNT = "tx.account"
    const val TX_AMOUNT = "tx.amount"
    const val TX_CATEGORY = "tx.category"
    const val TX_SAVE = "tx.save"
    const val TRANSFER_FROM = "transfer.from"
    const val TRANSFER_TO = "transfer.to"
    const val TRANSFER_AMOUNT = "transfer.amount"
    const val TRANSFER_SAVE = "transfer.save"
    const val SETTINGS_FIXED = "settings.fixed"
    const val FIXED_ADD = "fixed.add"
    const val FIXED_NAME = "fixed.name"
    const val FIXED_KIND = "fixed.kind"
    const val FIXED_AMOUNT = "fixed.amount"
    const val FIXED_ACCOUNT = "fixed.account"
    const val FIXED_FREQUENCY = "fixed.frequency"
    const val FIXED_NOTIFY = "fixed.notify"
    const val FIXED_SAVE = "fixed.save"
}
