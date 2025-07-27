package com.pjariwala.util;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeConverter implements DynamoDBTypeConverter<String, LocalDateTime> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  @Override
  public String convert(LocalDateTime dateTime) {
    return dateTime != null ? dateTime.format(FORMATTER) : null;
  }

  @Override
  public LocalDateTime unconvert(String stringValue) {
    return stringValue != null ? LocalDateTime.parse(stringValue, FORMATTER) : null;
  }
}
