package com.vilikin.utils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import java.lang.reflect.Type


class LocalDateDeserializer : JsonDeserializer<LocalDate?>,
    JsonSerializer<LocalDate?> {

    @Throws(JsonParseException::class)
    override fun deserialize(
        je: JsonElement, type: Type?,
        jdc: JsonDeserializationContext?
    ): LocalDate? {
        val asString = je.asString

        return if (asString.isEmpty()) null else LocalDate.parse(asString)
    }

    override fun serialize(
        src: LocalDate?, typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        val formatter: DateTimeFormatter = ISODateTimeFormat.date()

        return JsonPrimitive(
            if (src == null) "" else formatter.print(src)
        )
    }
}
