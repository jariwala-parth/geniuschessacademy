package com.pjariwala.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomLocalTimeDeserializer extends JsonDeserializer<LocalTime> {

  private static final DateTimeFormatter[] FORMATTERS = {
    DateTimeFormatter.ofPattern("HH:mm:ss"), // 18:30:00
    DateTimeFormatter.ofPattern("HH:mm"), // 18:30
    DateTimeFormatter.ofPattern("H:mm"), // 6:30
    DateTimeFormatter.ofPattern("H.mm"), // 6.30
    DateTimeFormatter.ofPattern("HH.mm"), // 18.30
  };

  @Override
  public LocalTime deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    String timeString = parser.getText().trim();

    if (timeString == null || timeString.isEmpty()) {
      return null;
    }

    // Try each formatter until one works
    for (DateTimeFormatter formatter : FORMATTERS) {
      try {
        LocalTime time = LocalTime.parse(timeString, formatter);
        log.debug("evt=custom_time_deserialize input={} output={}", timeString, time);
        return time;
      } catch (DateTimeParseException e) {
        // Continue to next formatter
      }
    }

    // If no formatter worked, log error and throw exception
    log.error("evt=custom_time_deserialize_error input={} msg=unsupported_format", timeString);
    throw new IOException(
        String.format(
            "Unable to parse time '%s'. Supported formats: HH:mm, H:mm, H.mm, HH.mm (e.g., '18:30',"
                + " '6:30', '6.30')",
            timeString));
  }
}
