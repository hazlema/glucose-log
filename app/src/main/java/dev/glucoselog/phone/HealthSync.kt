package dev.glucoselog.phone
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
object HealthSync {
 val permissions=setOf(HealthPermission.getWritePermission(BloodGlucoseRecord::class))
 private val mutex=Mutex()
 suspend fun run(context:Context):SyncResult=withContext(Dispatchers.IO) { mutex.withLock {
  val store=EntryStore.get(context)
  try {
   if(HealthConnectClient.getSdkStatus(context)!=HealthConnectClient.SDK_AVAILABLE) return@withLock SyncResult(store.pending().size,"Install or update Health Connect to sync.")
   val client=HealthConnectClient.getOrCreate(context)
   if(!client.permissionController.getGrantedPermissions().containsAll(permissions)) return@withLock SyncResult(store.pending().size,"Connect Health Connect to send your readings.")
   SyncEngine(store,object:HealthSink {
    override suspend fun write(e:Entry) { client.insertRecords(listOf(HealthRecords.record(e))) }
    override suspend fun delete(id:String) { client.deleteRecords(BloodGlucoseRecord::class,recordIdsList=emptyList(),clientRecordIdsList=listOf(id)) }
   }).run()
  } catch(ex:CancellationException) { throw ex }
  catch(ex:Exception) { SyncResult(store.pending().size,"Sync paused. Open Health Connect to check permissions, then retry.") }
 } }
}
