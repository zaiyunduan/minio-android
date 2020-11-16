/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage,
 * (C) 2020 MinIO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.minio;

import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Locale;

/** Time formatters for S3 APIs. */
public class Time {
  public static final DateTimeZone UTC = DateTimeZone.UTC;

  public static final DateTimeFormatter AMZ_DATE_FORMAT =
          DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss'Z'").withLocale(Locale.US).withZone(UTC);

  public static final DateTimeFormatter RESPONSE_DATE_FORMAT =
          DateTimeFormat.forPattern("yyyy-MM-dd'T'HH':'mm':'ss'.'SSS'Z'").withLocale(Locale.US).withZone(UTC);

  // Formatted string is convertible to LocalDate only, not to LocalDateTime or ZonedDateTime.
  // Below example shows how to use this to get ZonedDateTime.
  // LocalDate.parse("20200225", SIGNER_DATE_FORMAT).atStartOfDay(UTC);
  public static final DateTimeFormatter SIGNER_DATE_FORMAT =
          DateTimeFormat.forPattern("yyyyMMdd").withLocale(Locale.US).withZone(UTC);

  public static final DateTimeFormatter HTTP_HEADER_DATE_FORMAT =
          DateTimeFormat.forPattern("EEE',' dd MMM yyyy HH':'mm':'ss 'GMT'").withLocale(Locale.US).withZone(UTC);

  public static final DateTimeFormatter EXPIRATION_DATE_FORMAT = RESPONSE_DATE_FORMAT;

  private Time() {}
}
