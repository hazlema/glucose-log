package dev.glucoselog.phone
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.*
class SyncJob:JobService() {
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main)
 private var running:Job?=null
 override fun onStartJob(params:JobParameters):Boolean {
  running=scope.launch { try { withTimeout(60_000) { HealthSync.run(applicationContext) }; jobFinished(params,false) } catch(ex:TimeoutCancellationException) { jobFinished(params,false) } catch(ex:CancellationException) { throw ex } catch(ex:Exception) { jobFinished(params,false) } }
  return true
 }
 override fun onStopJob(params:JobParameters):Boolean { running?.cancel(); return false }
 override fun onDestroy() { scope.cancel(); super.onDestroy() }
 companion object {
  fun schedule(context:Context):Boolean {
   val scheduler=context.getSystemService(JobScheduler::class.java)
   if(scheduler.getPendingJob(101)!=null) return true
   return scheduler.schedule(JobInfo.Builder(101,ComponentName(context,SyncJob::class.java)).setPeriodic(15*60*1000L).setPersisted(true).build())==JobScheduler.RESULT_SUCCESS
  }
 }
}
