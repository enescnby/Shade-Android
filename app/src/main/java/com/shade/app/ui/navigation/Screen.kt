package com.shade.app.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object Chat : Screen("chat/{chatId}/{chatName}") {
        fun createRoute(chatId: String, chatName: String) = "chat/$chatId/$chatName"
    }

    object Profile: Screen("profile/{shadeId}") {
        fun createRoute(shadeId: String) = "profile/$shadeId"
    }
}