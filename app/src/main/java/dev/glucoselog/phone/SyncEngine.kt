package dev.glucoselog.phone
import kotlinx.coroutines.CancellationException
interface SyncStore { fun pending():List<Entry>; fun acknowledge(id:String,revision:Long) }
interface HealthSink { suspend fun write(e:Entry); suspend fun delete(id:String) }
data class SyncResult(val remaining:Int,val error:String?=null)
class SyncEngine(private val store:SyncStore,private val sink:HealthSink) {
 suspend fun run():SyncResult {
  var error:String?=null
  for(e in store.pending()) {
   try {
    if(e.deleted) sink.delete(e.id) else sink.write(e)
    store.acknowledge(e.id,e.revision)
   } catch(ex:CancellationException) { throw ex }
   catch(ex:Exception) { error=ex.message ?: "Health Connect could not be updated." }
  }
  return SyncResult(store.pending().size,error)
 }
}
