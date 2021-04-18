package nu.mine.terranz.callstocalendarsync.entity

class PhoneCall(
    val missed: Boolean,
    val incoming: Boolean,
    val startDate: Long,
    val length: Long,
    val number: String
) {
    fun genId(): Int {
        return (missed.toString() + incoming.toString() + startDate.toString() + length.toString() + number).hashCode()
    }

    override fun toString(): String {
        return "PhoneCall(missed=$missed, incoming=$incoming, startDate=$startDate, length=$length, number='$number')"
    }


}