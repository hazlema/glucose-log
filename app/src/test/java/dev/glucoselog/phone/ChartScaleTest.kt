package dev.glucoselog.phone
import org.junit.Assert.*
import org.junit.Test
class ChartScaleTest {
 @Test fun singlePointAndNarrowWeekHaveMinimumSixtyPointSpan() {
  for(values in listOf(listOf(99.0),listOf(92.0,98.0,105.0))) {
   val axis=ChartScale.bounds(values,GlucoseUnit.MG)
   assertEquals(60.0,axis.high-axis.low,0.000001)
   assertTrue(axis.low<values.min() && axis.high>values.max())
  }
 }
 @Test fun wideRangeExpandsAndRetainsAllValues() {
  val axis=ChartScale.bounds(listOf(90.0,240.0),GlucoseUnit.MG)
  assertTrue(axis.high-axis.low>150.0)
  assertTrue(axis.low<90.0 && axis.high>240.0)
 }
 @Test fun lowerBoundNeverNegativeAndSpanIsPreserved() {
  val axis=ChartScale.bounds(listOf(1.0,5.0),GlucoseUnit.MG)
  assertEquals(0.0,axis.low,0.0); assertEquals(60.0,axis.high-axis.low,0.000001)
 }
 @Test fun unitsShowEquivalentBounds() {
  val mg=ChartScale.bounds(listOf(92.0,105.0),GlucoseUnit.MG)
  val mmol=ChartScale.bounds(listOf(92.0/18,105.0/18),GlucoseUnit.MMOL)
  assertEquals(mg.low/18,mmol.low,0.000001); assertEquals(mg.high/18,mmol.high,0.000001)
 }
}
