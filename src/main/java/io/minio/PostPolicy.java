/*
 * MinIO Java SDK for Amazon S3 Compatible Cloud Storage, (C) 2015 MinIO, Inc.
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

import com.google.common.io.BaseEncoding;

import org.joda.time.DateTime;

import io.minio.credentials.Credentials;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Post policy information to be used to generate presigned post policy form-data. Condition
 * elements and respective condition for Post policy is available <a
 * href="https://docs.aws.amazon.com/AmazonS3/latest/API/sigv4-HTTPPOSTConstructPolicy.html#sigv4-PolicyConditions">here</a>.
 */
public class PostPolicy {
  private static final List<String> RESERVED_ELEMENTS =
      Arrays.asList(
          new String[] {
            "bucket",
            "x-amz-algorithm",
            "x-amz-credential",
            "x-amz-date",
            "policy",
            "x-amz-signature"
          });
  private static final String ALGORITHM = "AWS4-HMAC-SHA256";
  private static final String EQ = "eq";
  private static final String STARTS_WITH = "starts-with";

  private String bucketName;
  private DateTime expiration;
  private Map<String, Map<String, String>> conditions;
  private Integer lowerLimit = null;
  private Integer upperLimit = null;

  public PostPolicy(@Nonnull String bucketName, @Nonnull DateTime expiration) {
    if (bucketName.isEmpty()) {
      throw new IllegalArgumentException("bucket name cannot be empty");
    }

    Map<String, Map<String, String>> conditions = new LinkedHashMap<>();
    conditions.put(EQ, new LinkedHashMap<>());
    conditions.put(STARTS_WITH, new LinkedHashMap<>());
    this.bucketName = bucketName;
    this.expiration = expiration;
    this.conditions = conditions;
  }

  private String trimDollar(String element) {
    return element.startsWith("$") ? element.substring(1, element.length()) : element;
  }

  /** Add equals condition of an element and value. */
  public void addEqualsCondition(@Nonnull String element, @Nonnull String value) {
    if (element.isEmpty()) {
      throw new IllegalArgumentException("condition element cannot be empty");
    }

    element = trimDollar(element);

    if ("success_action_redirect".equals(element)
        || "redirect".equals(element)
        || "content-length-range".equals(element)) {
      throw new IllegalArgumentException(element + " is unsupported for equals condition");
    }

    if (RESERVED_ELEMENTS.contains(element)) {
      throw new IllegalArgumentException(element + " cannot be set");
    }

    conditions.get(EQ).put(element, value);
  }

  /** Remove previously set equals condition of an element. */
  public void removeEqualsCondition(@Nonnull String element) {
    if (element.isEmpty()) {
      throw new IllegalArgumentException("condition element cannot be empty");
    }

    conditions.get(EQ).remove(trimDollar(element));
  }

  /**
   * Add starts-with condition of an element and value. Value set to empty string does matching any
   * content condition.
   */
  public void addStartsWithCondition(@Nonnull String element, @Nonnull String value) {
    if (element.isEmpty()) {
      throw new IllegalArgumentException("condition element cannot be empty");
    }

    element = trimDollar(element);

    if ("success_action_status".equals(element)
        || "content-length-range".equals(element)
        || (element.startsWith("x-amz-") && !element.startsWith("x-amz-meta-"))) {
      throw new IllegalArgumentException(element + " is unsupported for starts-with condition");
    }

    if (RESERVED_ELEMENTS.contains(element)) {
      throw new IllegalArgumentException(element + " cannot be set");
    }

    conditions.get(STARTS_WITH).put(element, value);
  }

  /** Remove previously set starts-with condition of an element. */
  public void removeStartsWithCondition(String element) {
    if (element.isEmpty()) {
      throw new IllegalArgumentException("condition element cannot be empty");
    }

    conditions.get(STARTS_WITH).remove(trimDollar(element));
  }

  /** Add content-length-range condition with lower and upper limits. */
  public void addContentLengthRangeCondition(int lowerLimit, int upperLimit) {
    if (lowerLimit < 0) {
      throw new IllegalArgumentException("lower limit cannot be negative number");
    }

    if (upperLimit < 0) {
      throw new IllegalArgumentException("upper limit cannot be negative number");
    }

    if (lowerLimit > upperLimit) {
      throw new IllegalArgumentException("lower limit cannot be greater than upper limit");
    }

    this.lowerLimit = lowerLimit;
    this.upperLimit = upperLimit;
  }

  /** Remove previously set content-length-range condition. */
  public void removeContentLengthRangeCondition() {
    this.lowerLimit = null;
    this.upperLimit = null;
  }

  private String quote(String value) {
    return "\"" + value + "\"";
  }

  private StringBuilder addCondition(
      StringBuilder sb, String condition, String element, String value, boolean isEnd) {
    sb.append("    [")
        .append(quote(condition))
        .append(", ")
        .append(quote("$" + element))
        .append(", ")
        .append(quote(value))
        .append("]");
    return isEnd ? sb : sb.append(",\n");
  }

  /**
   * Return form-data of this post policy. The returned map contains x-amz-algorithm,
   * x-amz-credential, x-amz-security-token, x-amz-date, policy and x-amz-signature.
   */
  public Map<String, String> formData(@Nonnull Credentials creds, @Nonnull String region)
      throws NoSuchAlgorithmException, InvalidKeyException {
    if (creds == null) {
      throw new IllegalArgumentException("credentials cannot be null");
    }

    if (region.isEmpty()) {
      throw new IllegalArgumentException("region cannot be empty");
    }

    if (!conditions.get(EQ).containsKey("key") && conditions.get(STARTS_WITH).containsKey("key")) {
      throw new IllegalArgumentException("key condition must be set");
    }

    StringBuilder sb = new StringBuilder().append("{\n");
    sb.append("  ")
        .append(quote("expiration"))
        .append(": ")
        .append(quote(Time.EXPIRATION_DATE_FORMAT.print(expiration)))
        .append(",\n");
    sb.append("  ").append(quote("conditions")).append(": [\n");
    addCondition(sb, "eq", "bucket", bucketName, false);
    for (Map.Entry<String, Map<String, String>> condition : conditions.entrySet()) {
      for (Map.Entry<String, String> entry : condition.getValue().entrySet()) {
        addCondition(sb, condition.getKey(), entry.getKey(), entry.getValue(), false);
      }
    }
    if (lowerLimit != null && upperLimit != null) {
      sb.append("    [")
          .append(quote("content-length-range"))
          .append(", ")
          .append(lowerLimit.toString())
          .append(", ")
          .append(upperLimit.toString())
          .append("],\n");
    }

    DateTime utcNow = DateTime.now(Time.UTC);
    String credential = Signer.credential(creds.accessKey(), utcNow, region);
    String amzDate = Time.AMZ_DATE_FORMAT.print(utcNow);

    addCondition(sb, "eq", "x-amz-algorithm", ALGORITHM, false);
    addCondition(sb, "eq", "x-amz-credential", credential, false);
    if (creds.sessionToken() != null) {
      addCondition(sb, "eq", "x-amz-security-token", creds.sessionToken(), false);
    }
    addCondition(sb, "eq", "x-amz-date", amzDate, true);
    sb.append("  ]\n");
    sb.append("}");

    String policy = BaseEncoding.base64().encode(sb.toString().getBytes(StandardCharsets.UTF_8));
    String signature = Signer.postPresignV4(policy, creds.secretKey(), utcNow, region);

    Map<String, String> formData = new HashMap<>();
    formData.put("x-amz-algorithm", ALGORITHM);
    formData.put("x-amz-credential", credential);
    if (creds.sessionToken() != null) {
      formData.put("x-amz-security-token", creds.sessionToken());
    }
    formData.put("x-amz-date", amzDate);
    formData.put("policy", policy);
    formData.put("x-amz-signature", signature);
    return formData;
  }

  public String bucket() {
    return this.bucketName;
  }
}
