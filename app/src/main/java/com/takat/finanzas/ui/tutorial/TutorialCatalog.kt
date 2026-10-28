package com.takat.finanzas.ui.tutorial

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.WavingHand
import com.takat.finanzas.ui.navigation.Routes

/** Every guide in the app, in the order Ajustes > Guías lists them. */
object TutorialCatalog {
    const val WELCOME = "welcome"
    const val FIRST_ACCOUNT = "first-account"
    const val FIXED_EXPENSES = "fixed-expenses"

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
        followUpId = FIXED_EXPENSES,
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
                    "tarjeta y vuelve hacia 0."
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
                    "Lo último que conviene entender son los gastos fijos (alquiler, luz, cuotas), porque cambian tu " +
                    "Disponible. ¿Te los explico ahora?",
                primaryLabel = "Ver gastos fijos"
            )
        )
    )

    private val fixedExpenses = TutorialGuide(
        id = FIXED_EXPENSES,
        title = "Gastos fijos",
        summary = "Qué son, por qué bajan tu Disponible y cómo pagarlos",
        icon = Icons.Filled.EventRepeat,
        requiresAccount = true,
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

    val guides: List<TutorialGuide> = listOf(welcome, firstAccount, fixedExpenses)

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
