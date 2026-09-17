package dev.glucoselog.phone

import java.util.Locale

enum class GlucoseUnit(val label:String, val maximum:Double, val initial:String) {
 MG("mg/dL",900.0,"100"), MMOL("mmol/L",50.0,"5.6");
 fun parse(text:String):Double {
  val value=text.trim().replace(',','.').toDoubleOrNull()
  require(value!=null && value.isFinite() && value>0 && value<=maximum) { "Enter a value above 0 and up to ${format(maximum)} $label." }
  return value
 }
 fun toMg(value:Double)=if(this==MG) value else value*18.0
 fun fromMg(value:Double)=if(this==MG) value else value/18.0
 fun format(value:Double):String = String.format(Locale.US,if(this==MG) "%.1f" else "%.2f",value).trimEnd('0').trimEnd('.')
}
data class Entry(val id:String,val value:Double,val unit:GlucoseUnit,val time:Long,val offset:Int,val context:Int,val note:String,val revision:Long,val deleted:Boolean,val synced:Boolean) {
 fun requireRevision(current:Long) { require(revision==current) { "This reading changed or was deleted. Reopen it from History." } }
 fun validate(now:Long) {
  unit.parse(value.toString())
  require(time in 1..now) { "Choose a time that isn't in the future." }
  require(offset in -64800..64800 && context in 0..4 && revision>=0 && id.isNotBlank()) { "Invalid reading details." }
  require(note.length<=2000) { "Keep notes under 2,000 characters." }
 }
 companion object { val contexts=listOf("No label","Fasting","Before meal","After meal","Bedtime") }
}
