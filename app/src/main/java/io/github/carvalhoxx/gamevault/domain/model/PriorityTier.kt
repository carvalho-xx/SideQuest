package io.github.carvalhoxx.gamevault.domain.model

enum class PriorityTier(val badge: String, val label: String) {
    TIER_S("S", "Urgente"),
    TIER_A("A", "Alta"),
    TIER_B("B", "Média"),
    TIER_C("C", "Baixa")
}
