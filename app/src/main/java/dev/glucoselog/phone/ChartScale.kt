package dev.glucoselog.phone
import kotlin.math.max

data class ChartBounds(val low:Double,val high:Double)
object ChartScale {
 /** A display scale, not a clinical target range. Compute padding consistently across units. */
 fun bounds(values:List<Double>,unit:GlucoseUnit):ChartBounds {
  require(values.isNotEmpty() && values.all { it.isFinite() && it>=0 })
  val min=values.min(); val maxValue=values.max()
  val padding=max((maxValue-min)*0.15,unit.fromMg(10.0))
  val span=max(unit.fromMg(60.0),maxValue-min+padding*2)
  val low=max(0.0,(min+maxValue)/2-span/2)
  return ChartBounds(low,low+span)
 }
}
