package com.pjariwala.constants;

/** System-wide constants for the Genius Chess Academy application */
public final class SystemConstants {

  /** System organization ID used for system-level operations */
  public static final String SYSTEM_ORGANIZATION_ID = "system";

  /** Default timezone for the application */
  public static final String DEFAULT_TIMEZONE = "Asia/Kolkata";

  /** Default subscription plan */
  public static final String DEFAULT_SUBSCRIPTION_PLAN = "BASIC";

  /** Default maximum users per organization */
  public static final int DEFAULT_MAX_USERS = 100;

  /** Private constructor to prevent instantiation */
  private SystemConstants() {
    // Utility class - prevent instantiation
  }
}
