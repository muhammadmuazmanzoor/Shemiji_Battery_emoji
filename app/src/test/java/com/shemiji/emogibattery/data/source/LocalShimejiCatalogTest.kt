package com.shemiji.emogibattery.data.source

import com.shemiji.emogibattery.data.model.ShimejiPoses
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalShimejiCatalogTest {
    @Test
    fun catalogContainsEveryPresentNumberedSpriteSheet() = runBlocking {
        val characters = LocalContentDataSource().getShimejiCharacters()

        assertEquals(23, characters.size)
        assertEquals(characters.size, characters.map { it.id }.distinct().size)
        assertTrue(characters.all { it.drawableRes != null })
    }

    @Test
    fun posePreviewsStayInsideFourByEightSheetGrid() {
        assertEquals(9, ShimejiPoses.size)
        assertEquals(ShimejiPoses.size, ShimejiPoses.map { it.id }.distinct().size)
        assertTrue(ShimejiPoses.all { it.row in 0..7 && it.column in 0..3 })
    }
}
