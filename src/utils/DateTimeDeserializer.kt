package com.vilikin.utils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import java.lang.reflect.Type


class DateTimeDeserializer : JsonDeserializer<DateTime?>,
    JsonSerializer<DateTime?> {

    @Throws(JsonParseException::class)
    override fun deserialize(
        je: JsonElement, type: Type?,
        jdc: JsonDeserializationContext?
    ): DateTime? {
        val formatter: DateTimeFormatter = ISODateTimeFormat.dateTimeParser()
        val asString = je.asString

        return if (asString.isEmpty()) null else formatter.parseDateTime(
            asString
        )
    }

    override fun serialize(
        src: DateTime?, typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val formatter: DateTimeFormatter = ISODateTimeFormat.dateTime()

        return JsonPrimitive(
            if (src == null) "" else formatter.print(src)
        )
    }
}
