package net.explorviz.trace.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringJoiner;
import net.explorviz.avro.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class holds the hashing function for an {@link net.explorviz.avro.Span}.
 */
public final class HashHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(HashHelper.class);

  private static final String DIGEST_ALGORITHM = "SHA3-256";

  private static final int EIGHT_BIT_CAP = 0xff;

  private HashHelper() {
    // Helper
  }

  public static String createHash(final Span span) {
    return createHash(span.getLandscapeToken(), span.getHostIpAddress(), span.getAppInstanceId(),
        span.getFullyQualifiedOperationName());
  }

  private static String createHash(final String landscapeToken, final String hostIp,
      final String appInstanceId, final String methodFqn) {

    final StringJoiner joiner = new StringJoiner(";");

    /*
     * By definition getFullyQualifiedOperationName().split("."): Last entry is method name, next to
     * last is class name, remaining elements form the package name
     */
    final String fullyQualifiedOperationName = methodFqn.replace(".", ";");

    joiner.add(landscapeToken);
    joiner.add(hostIp);
    joiner.add(appInstanceId);
    joiner.add(fullyQualifiedOperationName);

    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
    } catch (final NoSuchAlgorithmException e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Set digest algorithm is not available. Did you use 'SHA3-256'?", e);
      }
      throw new IllegalArgumentException("Not supported algorithm detected.", e);
    }

    final byte[] hashbytes = digest.digest(joiner.toString().getBytes(StandardCharsets.UTF_8));

    return bytesToHex(hashbytes);

  }

  private static String bytesToHex(final byte[] hash) {
    final StringBuilder hexString = new StringBuilder();
    for (final byte element : hash) {
      final String hex = Integer.toHexString(EIGHT_BIT_CAP & element);
      if (hex.length() == 1) {
        hexString.append('0');
      }
      hexString.append(hex);
    }
    return hexString.toString();
  }

}

