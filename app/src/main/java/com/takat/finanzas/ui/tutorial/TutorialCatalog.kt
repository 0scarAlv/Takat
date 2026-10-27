package com.takat.finanzas.ui.tutorial

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.WavingHand
import com.takat.finanzas.ui.navigation.Routes

/** Every guide in the app, in the order Ajustes > Guías lists them. */
object TutorialCatalog {
    const val WELCOME = "welcome"
    const val FIRST_ACCOUNT = "first-account"

    private val welcome = TutorialGuide(
        id = WELCOME,
        title = "Bienvenida",
        summary = "Lo básico de Takat en un minuto",
        icon = Icons.Filled.WavingHand,
        followUpId = FIRST_ACCOUNT,
        steps = listOf(
            TutorialStep(
                screen = Routes.HOME,
                title = "¡Bienvenido a Takat!",
                body = "Takat te ayuda a saber cuánta plata tenés de verdad y cuánto podés gastar sin quedarte corto. " +
                    "Te muestro lo básico en un minuto."
            ),
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_HERO,
                title = "Tu resumen",
                body = "\"Disponible\" es la plata con la que contás de verdad: lo que tenés, menos lo que debés y los " +
                    "gastos fijos que todavía no pagaste. Con el ojito ocultás los montos si alguien está mirando."
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
                body = "Respaldo automático, gastos fijos, categorías y el tema de la app. En Ajustes › Guías podés " +
                    "repetir estos tutoriales cuando quieras."
            ),
            TutorialStep(
                screen = Routes.HOME,
                title = "Empecemos",
                body = "Para que Takat sirva necesitás al menos una cuenta: tu efectivo, el banco o una tarjeta. " +
                    "¿La creamos juntos?",
                primaryLabel = "Crear mi cuenta"
            )
        )
    )

    private val firstAccount = TutorialGuide(
        id = FIRST_ACCOUNT,
        title = "Tu primera cuenta",
        summary = "Crear una cuenta paso a paso",
        icon = Icons.Filled.AccountBalanceWallet,
        steps = listOf(
            TutorialStep(
                screen = Routes.HOME,
                anchor = TutorialAnchors.HOME_ADD_ACCOUNT,
                advance = Advance.TapAnchor,
                title = "Creá una cuenta",
                body = "Una cuenta es cualquier lugar donde tenés (o debés) plata: la billetera, el banco, una " +
                    "tarjeta. Tocá el + para crear una."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_NEW,
                anchor = TutorialAnchors.ACCOUNT_NAME,
                title = "Ponele nombre",
                body = "Por ejemplo \"Efectivo\" o \"Banco\". Tocá el campo, escribilo y después Siguiente."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_NEW,
                anchor = TutorialAnchors.ACCOUNT_DEBT,
                title = "¿Tengo o Debo?",
                body = "\"Tengo\" es plata tuya, como efectivo o ahorros. \"Debo\" es una deuda, como la tarjeta de " +
                    "crédito o un préstamo: su saldo se resta de lo que tenés."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_NEW,
                anchor = TutorialAnchors.ACCOUNT_INCLUDE,
                title = "Sumar al resumen",
                body = "Dejalo encendido para que la cuenta cuente en tu Disponible. Apagalo para plata que querés " +
                    "llevar aparte, como un ahorro que no pensás tocar."
            ),
            TutorialStep(
                screen = Routes.ACCOUNT_NEW,
                anchor = TutorialAnchors.ACCOUNT_AMOUNT,
                title = "¿Con cuánto arranca?",
                body = "Escribí cuánto tiene hoy esa cuenta. Si no sabés exacto, poné un aproximado: lo podés " +
                    "corregir cuando quieras."
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
                title = "¡Listo!",
                body = "Acá vas a ver tus cuentas con su saldo. Tocá una para ver sus movimientos o editarla. " +
                    "Cuando gastes algo, registralo con el botón + de abajo.",
                primaryLabel = "Terminar"
            )
        )
    )

    val guides: List<TutorialGuide> = listOf(welcome, firstAccount)

    fun byId(id: String): TutorialGuide? = guides.firstOrNull { it.id == id }
}

/** Ids passed to [tutorialAnchor]; kept in one place so a typo can't silently break a guide. */
object TutorialAnchors {
    const val HOME_HERO = "home.hero"
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
}
