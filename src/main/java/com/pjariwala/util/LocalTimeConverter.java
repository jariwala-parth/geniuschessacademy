package com.pjariwala.util;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LocalTimeConverter implements DynamoDBTypeConverter<String, LocalTime> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

  @Override
  public String convert(LocalTime time) {
    return time != null ? time.format(FORMATTER) : null;
  }

  @Override
  public LocalTime unconvert(String stringValue) {
    return stringValue != null ? LocalTime.parse(stringValue, FORMATTER) : null;
  }
}
