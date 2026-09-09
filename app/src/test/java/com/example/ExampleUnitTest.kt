package com.example

import com.example.model.VideoAnalysis
import com.example.util.GeminiVideoAnalyzer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testVideoAnalysisModelFields() {
    val analysis = VideoAnalysis(
      id = 1,
      title = "Remate x3 Metros",
      strokeType = "Remate por 3 Metros",
      score = 88,
      levelTier = "Avanzado Competitivo (Playtomic 4.8)",
      positiveFeedback = "Buena aceleración vertical.",
      improvementFeedback = "1. Ajustar arqueo lumbar. 2. Acompañar pronación.",
      technicalBreakdown = "Pies: 85 | Impacto: 90",
      recommendedDrill = "Drill Smash con cono a 1 metro de pared.",
      videoUriOrSample = "remate",
      footworkScore = 85,
      impactScore = 90,
      followThroughScore = 88,
      tacticsScore = 89,
      timestamp = 1700000000000L
    )

    assertEquals(88, analysis.score)
    assertEquals("Remate por 3 Metros", analysis.strokeType)
    assertTrue(analysis.improvementFeedback.contains("arqueo lumbar"))
    assertTrue(analysis.score in 0..100)
    assertTrue(analysis.footworkScore in 0..100)
  }
}

