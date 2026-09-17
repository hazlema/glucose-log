package dev.glucoselog.phone
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.widget.*
object Ui {
 val ink=Color.rgb(21,50,74); val teal=Color.rgb(8,126,139); val muted=Color.rgb(82,105,122); val pale=Color.rgb(234,245,245)
 fun dp(c:Context,v:Int)=(v*c.resources.displayMetrics.density).toInt()
 fun background(color:Int,radius:Float=16f)=GradientDrawable().apply { setColor(color); cornerRadius=radius }
 fun text(c:Context,value:String,size:Float=16f,bold:Boolean=false)=TextView(c).apply { text=value; textSize=size; setTextColor(ink); if(bold) setTypeface(typeface,Typeface.BOLD); setPadding(0,dp(c,6),0,dp(c,6)) }
 fun button(c:Context,label:String,primary:Boolean=false,action:()->Unit)=Button(c).apply {
  text=label; isAllCaps=false; textSize=16f; minHeight=dp(c,50); setTextColor(if(primary) Color.WHITE else ink)
  background=background(if(primary) teal else pale,dp(c,12).toFloat()); setPadding(dp(c,12),dp(c,6),dp(c,12),dp(c,6)); setOnClickListener { action() }
  layoutParams=LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(c,8); bottomMargin=dp(c,4) }
 }
 fun column(c:Context)=LinearLayout(c).apply { orientation=LinearLayout.VERTICAL }
}
