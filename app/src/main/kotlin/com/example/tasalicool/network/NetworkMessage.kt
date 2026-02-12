data class NetworkMessage(
    val playerId: String,
    val gameType: String,
    val action: GameAction,
    val data: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
