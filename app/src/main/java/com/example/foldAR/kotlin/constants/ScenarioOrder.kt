package com.example.foldAR.kotlin.constants

class ScenarioOrder {
    val orderList: List<Triple<Scenarios, Scenarios, Scenarios>> = listOf(
        Triple(Scenarios.FOLDAR, Scenarios.FOLDAROPEN, Scenarios.FOLDAR),
        Triple(Scenarios.FOLDAR, Scenarios.STATEOFTHEART, Scenarios.FOLDAROPEN),
        Triple(Scenarios.FOLDAROPEN, Scenarios.FOLDAR, Scenarios.STATEOFTHEART),
        Triple(Scenarios.FOLDAROPEN, Scenarios.STATEOFTHEART, Scenarios.FOLDAR),
        Triple(Scenarios.STATEOFTHEART, Scenarios.FOLDAR, Scenarios.FOLDAROPEN),
        Triple(Scenarios.STATEOFTHEART, Scenarios.FOLDAROPEN, Scenarios.FOLDAR)
    )
}