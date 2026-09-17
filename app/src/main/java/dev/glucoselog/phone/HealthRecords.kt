package dev.glucoselog.phone
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import java.time.Instant
import java.time.ZoneOffset
object HealthRecords {
 fun record(e:Entry)=BloodGlucoseRecord(
  time=Instant.ofEpochSecond(e.time),zoneOffset=ZoneOffset.ofTotalSeconds(e.offset),
  metadata=Metadata.manualEntry(clientRecordId=e.id,clientRecordVersion=e.revision),
  level=if(e.unit==GlucoseUnit.MG) BloodGlucose.milligramsPerDeciliter(e.value) else BloodGlucose.millimolesPerLiter(e.value),
  specimenSource=BloodGlucoseRecord.SPECIMEN_SOURCE_UNKNOWN,mealType=MealType.MEAL_TYPE_UNKNOWN,
  relationToMeal=when(e.context) {1->BloodGlucoseRecord.RELATION_TO_MEAL_FASTING;2->BloodGlucoseRecord.RELATION_TO_MEAL_BEFORE_MEAL;3->BloodGlucoseRecord.RELATION_TO_MEAL_AFTER_MEAL;else->BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN})
}
