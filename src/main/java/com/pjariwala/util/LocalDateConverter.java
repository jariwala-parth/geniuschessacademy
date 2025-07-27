package com.pjariwala.util;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateConverter implements DynamoDBTypeConverter<String, LocalDate> {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  @Override
  public String convert(LocalDate date) {
    return date != null ? date.format(FORMATTER) : null;
  }

  @Override
  public LocalDate unconvert(String stringValue) {
    return stringValue != null ? LocalDate.parse(stringValue, FORMATTER) : null;
  }
}
