// Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

class CreateEventNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.navigate(AppScreens.EventOverview.name) }
}
