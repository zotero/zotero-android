package org.zotero.android.sync

object DelayIntervals {
   val  sync: List<Double> = createSyncIntervals()
    val retry = listOf(0, 10000, 20000, 40000, 60000, 120000, 240000, 300000)

    private fun createSyncIntervals(): List<Double> {
        val hourIntervals = listOf(0.5, 1.0, 4.0, 16.0, 16.0, 16.0, 16.0, 16.0, 16.0, 16.0, 64.0)
        return hourIntervals.map{ it * 60 * 60 * 1000 }
    }
}
