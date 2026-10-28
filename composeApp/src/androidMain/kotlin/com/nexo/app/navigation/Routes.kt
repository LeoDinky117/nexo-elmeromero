package com.nexo.app.navigation

import nexo.composeapp.generated.resources.Res
import nexo.composeapp.generated.resources.icon_calendar
import nexo.composeapp.generated.resources.icon_concepto
import nexo.composeapp.generated.resources.icon_gastos
import nexo.composeapp.generated.resources.icon_ingreso
import nexo.composeapp.generated.resources.icon_metas
import org.jetbrains.compose.resources.DrawableResource

object Routes {

    const val HOME = "home"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val MAIN_HOME = "main_home"
    const val MOVIMIENTOS = "movimientos"
    const val CALENDARIO = "calendario"
    const val PRESUPUESTO = "presupuesto"
    const val METAS = "metas"
    const val PERFIL = "perfil"
    const val ADD_MOVIMIENTO = "add_movimiento"
    const val SUCCESS = "success"

}
sealed class BottomBarItem(val route: String, val title: String, val icon: DrawableResource){
    object Inicio:
        BottomBarItem(Routes.MAIN_HOME, "Inicio",Res.drawable.icon_ingreso)
    object Movimientos:
        BottomBarItem(Routes.MOVIMIENTOS,"Movimientos",Res.drawable.icon_gastos)
    object Calendario:
        BottomBarItem(Routes.CALENDARIO,"Calendario",Res.drawable.icon_calendar)
    object Presupuesto:
        BottomBarItem(Routes.PRESUPUESTO,"Presupuesto",Res.drawable.icon_concepto)
    object Metas:
        BottomBarItem(Routes.METAS,"Metas", Res.drawable.icon_metas)
    object Perfil:
        BottomBarItem(Routes.PERFIL,"Perfil", Res.drawable.icon_metas)
}
