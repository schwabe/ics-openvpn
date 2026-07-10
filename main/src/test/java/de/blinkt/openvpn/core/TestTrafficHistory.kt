/*
 * Copyright (c) 2012-2026 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core

import de.blinkt.openvpn.core.TrafficHistory.TrafficDatapoint
import org.junit.Assert
import org.junit.Test

class TestTrafficHistory {
    @Test
    fun testSimple() {

        val history = TrafficHistory()

        history.add(100, 500, 10000)
        history.add(300, 700, 11000)
        history.add(500, 900, 13000)
        history.add(700, 2000, 15000)

        val lastdiff = history.getLastDiff(null)

        Assert.assertTrue(lastdiff.`in` == 700L)
        Assert.assertTrue(lastdiff.`out` == 2000L)
        Assert.assertTrue(lastdiff.diffIn == 0L)
        Assert.assertTrue(lastdiff.diffOut == 0L)

        history.add(2500, 2900, 23000)
        history.add(3700, 5000, 35000)

        val tdp = TrafficDatapoint(4000, 7000, 40000)

        val lastdiff2 = history.getLastDiff(tdp)

        Assert.assertTrue(lastdiff2.`in` == 4000L)
        Assert.assertTrue(lastdiff2.`out` == 7000L)
        Assert.assertTrue(lastdiff2.diffIn == 300L)
        Assert.assertTrue(lastdiff2.diffOut == 2000L)
    }

    @Test
    fun testHistoryArraySizes() {
        val history = TrafficHistory()
        for (i in 1L..358) {
            history.add(i * 100, i * 1000, i * 1000)
        }
        Assert.assertTrue(history.seconds.size == 358)
        Assert.assertTrue(history.minutes.size == 6)

        history.add(359 * 100, 360 * 1000, 359 * 1000)
        history.add(360 * 100, 361 * 1000, 360 * 1000)
        Assert.assertTrue(history.seconds.size == 360)

        history.add(361 * 100, 362 * 1000, 361 * 1000)
        Assert.assertTrue(history.seconds.size == 300)
        history.add(362 * 100, 363 * 1000, 362 * 1000)
        Assert.assertTrue(history.seconds.size == 301)

        for (i in 363L..421) {
            history.add(i * 100, i * 1000, i * 1000)
        }
        Assert.assertTrue(history.seconds.size == 360)
        history.add(422 * 100, 421 * 1000, 422 * 1000)
        Assert.assertTrue(history.seconds.size == 300)
    }


    @Test
    fun testHistoryAveragesSimple() {
        val history = TrafficHistory()
        for (i in 1L..(6L * 3700) step 23) {
            history.add(i * 100, i * 1000, i * 1000)

            Assert.assertTrue(history.seconds.size <= 359)
            Assert.assertTrue(history.minutes.size <= 359)

            Assert.assertTrue(i < 500 || history.seconds.last().timestamp - history.seconds.first().timestamp  >= 200000)
            Assert.assertTrue(i < 7 * 3600 || history.minutes.last().timestamp - history.minutes.first().timestamp  >= 3 * 60 * 1000*1000)

        }

        Assert.assertTrue(history.hours.size == 7)


    }
}