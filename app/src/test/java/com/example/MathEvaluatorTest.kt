package com.example

import com.example.util.MathEvaluator
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for MathEvaluator adhering to Phase 6 testing guidelines.
 */
class MathEvaluatorTest {

    @Test
    fun testBasicAddition() {
        assertEquals("5", MathEvaluator.evaluate("2 + 3"))
    }

    @Test
    fun testMultiplicationAndOrder() {
        assertEquals("14", MathEvaluator.evaluate("2 + 3 * 4"))
    }

    @Test
    fun testParentheses() {
        assertEquals("20", MathEvaluator.evaluate("(2 + 3) * 4"))
    }

    @Test
    fun testSquareRoot() {
        assertEquals("4", MathEvaluator.evaluate("sqrt(16)"))
    }

    @Test
    fun testInvalidExpression() {
        assertEquals("", MathEvaluator.evaluate("abc + "))
    }
}
