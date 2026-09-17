package dev.glucoselog.phone
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChartView(c:Context,entries:List<Entry>,private val unit:GlucoseUnit):View(c) {
 private val end=Instant.now().epochSecond
 private val start=end-7*86400
 private val points=entries.filter { !it.deleted && it.time in start..end }.sortedBy { it.time }
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 init { minimumHeight=Ui.dp(c,210); contentDescription=if(points.isEmpty()) "No readings in the past seven days" else "Past seven days, ${points.size} readings. Values also listed below." }
 override fun onDraw(canvas:Canvas) {
  super.onDraw(canvas)
  val d=resources.displayMetrics.density; val left=52*d; val right=width-12*d; val top=24*d; val bottom=height-36*d
  paint.textSize=12*resources.displayMetrics.scaledDensity; paint.color=Ui.muted
  if(points.isEmpty()) { canvas.drawText("Your readings will appear here.",16*d,height/2f,paint); return }
  val values=points.map { unit.fromMg(it.unit.toMg(it.value)) }
  val axis=ChartScale.bounds(values,unit)
  val low=axis.low; val high=axis.high
  for(i in 0..2) {
   val y=bottom-(bottom-top)*i/2f
   paint.color=0xffd5e1e9.toInt(); paint.strokeWidth=d; canvas.drawLine(left,y,right,y,paint)
   paint.color=Ui.muted; canvas.drawText(unit.format(low+(high-low)*i/2),0f,y+4*d,paint)
  }
  val format=DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())
  canvas.drawText(format.format(Instant.ofEpochSecond(start)),left,height-10*d,paint)
  val label="Today"; canvas.drawText(label,right-paint.measureText(label),height-10*d,paint)
  paint.color=Ui.teal
  points.zip(values).forEach { (e,v) -> canvas.drawCircle(left+(right-left)*((e.time-start).toDouble()/(end-start)).toFloat(),bottom-(bottom-top)*((v-low)/(high-low)).toFloat(),5*d,paint) }
 }
}
