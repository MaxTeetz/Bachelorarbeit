package com.example.foldAR.kotlin.constants

object ObjectCoords {
    val positions: List<Triple<Double, Double, Double>> = listOf(
        //Left/Right, Front/Back, Height
        //First one is always to make the user get used to the scenario. Do NOT include into Data analysis
        Triple(-1.0, -0.0, -0.0),

        //Vorne
        Triple(0.0, -3.0, 1.0),
        //hinten
        Triple(0.0, 3.0, 1.0),
        //links
        Triple(-3.0, 0.0, 1.0),
        //rechts
        Triple(3.0, 0.0, 1.0),
        //vorne links
        Triple(-3.0, -3.0, 1.0),
        //vorne rechts
        Triple(3.0, -3.0, 1.0),
        //hinten links
        Triple(-3.0, 3.0, 1.0),
        //hinten rechts
        Triple(3.0, 3.0, 1.0),


        //Maximum -1 in der Höhe und Plus 1
        //Vorne
        Triple(0.0, -3.0, -1.0),
        //hinten
        Triple(0.0, 3.0, -1.0),
        //links
        Triple(-3.0, 0.0, -1.0),
        //rechts
        Triple(3.0, 0.0, -1.0),
        //vorne links
        Triple(-3.0, -3.0, -1.0),
        //vorne rechts
        Triple(3.0, -3.0, -1.0),
        //hinten links
        Triple(-3.0, 3.0, -1.0),
        //hinten rechts
        Triple(3.0, 3.0, -1.0),

        //Vorne rechts
        Triple(2.0, -3.0, -0.0),
        //hinten links
        Triple(-2.0, 3.0, -0.0),
        //vorne links
        Triple(-2.0, -3.0, -0.0),
        //hinten rechts
        Triple(2.0, 3.0, -0.0),
    )
}