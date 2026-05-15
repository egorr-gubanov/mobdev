package lab3.egor.chat.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Chats : Screen("chats")
    object Messages : Screen("messages/{channel}") {
        fun createRoute(channel: String) = "messages/$channel"
    }
    object FullImage : Screen("image/{link}") {
        fun createRoute(link: String) = "image/$link"
    }
}
