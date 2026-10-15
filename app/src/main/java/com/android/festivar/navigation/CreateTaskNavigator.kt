// Co-authored-by: Claude Sonnet 5.5 <noreply@anthropic.com>
package com.android.festivar.navigation

import androidx.navigation.NavHostController

class CreateTaskNavigator(private val navController: NavHostController) {
  val onBack: () -> Unit = { navController.popBackStack() }

  val onTaskCreated: () -> Unit = { navController.popBackStack() }
}
