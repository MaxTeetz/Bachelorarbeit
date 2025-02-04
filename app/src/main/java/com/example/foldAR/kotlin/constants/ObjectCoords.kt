package com.example.foldAR.kotlin.constants

object ObjectCoords {
    val positions: List<Triple<Double, Double, Double>> = listOf(
        //Left/Right, Front/Back, Height
        //First one is always to make the user get used to the scenario. Do NOT include into Data analysis
        Triple(-1.0, -0.0, -0.0),

        //Vorne
        Triple(0.0, -3.0, 0.0),
        //hinten
        Triple(0.0, 3.0, 0.0),
        //hinten links
        Triple(-2.0, 3.0, 0.5),
        //links
        Triple(-3.0, 0.0, 0.0),
        //links
        Triple(-3.0, 0.0, -0.5),
        //rechts
        Triple(3.0, 0.0, 0.0),
        //Vorne rechts
        Triple(2.0, -3.0, 0.5),
        //vorne links
        Triple(-3.0, -3.0, 0.0),
        //hinten rechts
        Triple(3.0, 3.0, -0.5),
        //hinten rechts
        Triple(2.0, 3.0, 0.5),
        //rechts
        Triple(3.0, 0.0, -0.5),
        //vorne links
        Triple(-2.0, -3.0, 0.5),
        //vorne rechts
        Triple(3.0, -3.0, 0.0),
        //Vorne
        Triple(0.0, -3.0, -0.5),
        //hinten links
        Triple(-3.0, 3.0, 0.0),
        //hinten rechts
        Triple(3.0, 3.0, 0.0),
        //hinten
        Triple(0.0, 3.0, -0.5),
        //vorne links
        Triple(-3.0, -3.0, -0.5),
        //vorne rechts
        Triple(3.0, -3.0, -0.5),
        //hinten links
        Triple(-3.0, 3.0, -0.5),


    )
}