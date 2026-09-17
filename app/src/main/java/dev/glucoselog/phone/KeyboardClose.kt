package dev.glucoselog.phone

/** A real visible-to-hidden transition, not startup/layout/background events. */
class KeyboardClose {
 private var wasVisible=false
 fun update(visible:Boolean,eligible:Boolean,changed:Boolean,busy:Boolean):Boolean {
  val closed=wasVisible && !visible
  wasVisible=visible && eligible
  return closed && eligible && changed && !busy
 }
}
