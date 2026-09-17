package dev.glucoselog.phone
import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
class RationaleActivity:Activity() {
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  val body=Ui.column(this).apply { setPadding(32,64,32,48) }
  body.addView(Ui.text(this,"Your data",28f,true))
  body.addView(Ui.text(this,"Glucose Log stores readings and notes in a private database on this phone. There is no account, server, advertising, analytics, or Internet permission.\n\nWith your permission, glucose value, measurement time, and supported meal labels are written to Health Connect as manual entries. Notes stay in this app. We request only blood glucose write permission, not access to your health history.\n\nEdits update this app's Health Connect record. Deletes are queued until Health Connect confirms removal. Other apps such as Cronometer control their own imports and may retain copies. Sync retries when you open the app and periodically when Android allows it.\n\nAndroid may display glucose in mmol/L even when you enter mg/dL; the reading is converted without changing its meaning.\n\nLocal backup is disabled. Uninstalling or clearing this app removes its local history and unsynced changes; previously exported Health Connect records may remain. Manage permissions and records in Health Connect settings.\n\nThis app logs readings; it does not measure glucose, estimate A1c, or recommend treatment."))
  body.addView(Ui.button(this,"Back") { finish() }); setContentView(ScrollView(this).apply { addView(body) })
 }
}
