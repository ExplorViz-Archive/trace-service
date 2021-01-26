package net.explorviz.trace.persistence.cassandra;

import com.datastax.oss.driver.api.core.AllNodesFailedException;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.ExecutionInfo;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.servererrors.QueryExecutionException;
import com.datastax.oss.driver.api.core.servererrors.QueryValidationException;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.explorviz.avro.SpanDynamic;
import net.explorviz.avro.Trace;
import net.explorviz.trace.persistence.PersistingException;
import net.explorviz.trace.persistence.SpanRepository;
import net.explorviz.trace.service.TimestampHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cassandra-backed repository to access and save {@link net.explorviz.avro.Trace} entities.
 */
@ApplicationScoped
public class CassandraSpanRepository implements SpanRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(CassandraSpanRepository.class);

  private final DbHelper db;

  /**
   * Create a new repository for accessing {@link Trace} object.
   *
   * @param db the backing Casandra db
   */
  @Inject
  public CassandraSpanRepository(final DbHelper db) {
    this.db = db;
    db.initialize();
  }



  @Override
  public void insert(final SpanDynamic span) throws PersistingException {
    try {
      // Try to append span to the set of spans corresponding the trace id and token
      // This fails if the row does not exists
      if (!this.appendSpan(span)) {
        // Row for this trace id and token does not exist, create it with the single span
        this.createNew(span);
      }
    } catch (final AllNodesFailedException e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Failed to insert new record: Database unreachable");
      }
      throw new PersistingException(e);
    } catch (QueryExecutionException | QueryValidationException e) {
      if (LOGGER.isErrorEnabled()) {
        LOGGER.error("Failed to insert new record: {0}", e.getCause());
      }
      throw new PersistingException(e);
    }
  }

  private SimpleStatement buildInsertTraceStatement(final Trace trace) {
    final long timestamp = TimestampHelper.toInstant(trace.getStartTime()).toEpochMilli();
    final Set<SpanDynamic> spanSet = new HashSet<>(trace.getSpanList());
    return QueryBuilder.insertInto(DbHelper.KEYSPACE_NAME, DbHelper.TABLE_SPANS)
        .value(DbHelper.COL_TOKEN, QueryBuilder.literal(trace.getLandscapeToken()))
        .value(DbHelper.COL_TIMESTAMP, QueryBuilder.literal(timestamp))
        .value(DbHelper.COL_TRACE_ID, QueryBuilder.literal(trace.getTraceId()))
        .value(DbHelper.COL_SPANS, QueryBuilder.literal(spanSet, this.db.getCodecRegistry()))
        .build();
  }

  @Override
  public void saveTrace(final Trace trace) throws PersistingException {
    final SimpleStatement insertStmt = this.buildInsertTraceStatement(trace);
    this.db.getSession().execute(insertStmt);
  }

  @Override
  public CompletionStage<AsyncResultSet> saveTraceAsync(final Trace trace)
      throws PersistingException {
    final SimpleStatement insertStmt = this.buildInsertTraceStatement(trace);
    return this.db.getSession().executeAsync(insertStmt);
  }


  /**
   * Creates a new entry.
   *
   * @param span the span to add
   */
  private void createNew(final SpanDynamic span) {

    final long timestamp = TimestampHelper.toInstant(span.getStartTime()).toEpochMilli();
    final SimpleStatement insertStmt =
        QueryBuilder.insertInto(DbHelper.KEYSPACE_NAME, DbHelper.TABLE_SPANS)
            .value(DbHelper.COL_TOKEN, QueryBuilder.literal(span.getLandscapeToken()))
            .value(DbHelper.COL_TIMESTAMP, QueryBuilder.literal(timestamp))
            .value(DbHelper.COL_TRACE_ID, QueryBuilder.literal(span.getTraceId()))
            .value(DbHelper.COL_SPANS,
                QueryBuilder.literal(Set.of(span), this.db.getCodecRegistry()))
            .build();
    this.db.getSession().execute(insertStmt);
  }

  /**
   * Appends a span to the (existing) set of all spans of the very trace id and landscape token.
   *
   * @param span span to append to set of spans with the same trace id
   * @return true iff the update was successful, false if the corresponding row does not exists
   */
  private boolean appendSpan(final SpanDynamic span) {
    final SimpleStatement updateStmt =
        QueryBuilder.update(DbHelper.KEYSPACE_NAME, DbHelper.TABLE_SPANS)
            .appendSetElement(DbHelper.COL_SPANS,
                QueryBuilder.literal(span, this.db.getCodecRegistry()))
            .whereColumn(DbHelper.COL_TOKEN)
            .isEqualTo(QueryBuilder.literal(span.getLandscapeToken()))
            .whereColumn(DbHelper.COL_TRACE_ID).isEqualTo(QueryBuilder.literal(span.getTraceId()))
            .ifExists()
            .build();

    return this.db.getSession().execute(updateStmt).wasApplied();

  }

  @Override
  public Optional<Collection<SpanDynamic>> getSpans(final String landscapeToken,
      final String traceId) {

    final SimpleStatement findStmt =
        QueryBuilder.selectFrom(DbHelper.KEYSPACE_NAME, DbHelper.TABLE_SPANS)
            .column(DbHelper.COL_SPANS)
            .whereColumn(DbHelper.COL_TOKEN).isEqualTo(QueryBuilder.literal(landscapeToken))
            .whereColumn(DbHelper.COL_TRACE_ID).isEqualTo(QueryBuilder.literal(traceId))
            .build().setTracing(true);
    final ResultSet queryResult = this.db.getSession().execute(findStmt); // NOPMD
    final ExecutionInfo exInfo = queryResult.getExecutionInfo();

    final Row result = queryResult.one();
    if (result == null) {
      return Optional.empty();
    } else {
      final Set<SpanDynamic> spans = result.getSet(DbHelper.COL_SPANS, SpanDynamic.class);
      exInfo.getQueryTraceAsync().thenAccept(t -> {
        final Duration d = Duration.of(t.getDurationMicros(), ChronoUnit.MICROS);
        LOGGER.info("Fetched {} spans in {}ms", spans.size(), d.toMillis());
      });
      return Optional.ofNullable(spans);
    }
  }

  @Override
  public List<Set<SpanDynamic>> getAllInRange(final String landscapeToken, final Instant from,
      final Instant to) {
    final SimpleStatement findAllStmt =
        QueryBuilder.selectFrom(DbHelper.KEYSPACE_NAME, DbHelper.TABLE_SPANS)
            .column(DbHelper.COL_SPANS)
            .whereColumn(DbHelper.COL_TOKEN).isEqualTo(QueryBuilder.literal(landscapeToken))
            .whereColumn(DbHelper.COL_TIMESTAMP).isGreaterThanOrEqualTo(QueryBuilder.literal(from))
            .whereColumn(DbHelper.COL_TIMESTAMP).isLessThanOrEqualTo(QueryBuilder.literal(to))
            .allowFiltering()
            .build();
    final ResultSet queryResults = this.db.getSession().execute(findAllStmt); // NOPMD
    final ExecutionInfo exInfo = queryResults.getExecutionInfo();

    final List<Set<SpanDynamic>> traces = new ArrayList<>();
    queryResults.forEach(r -> {
      final Optional<Set<SpanDynamic>> possibleSpans =
          Optional.ofNullable(r.getSet(DbHelper.COL_SPANS, SpanDynamic.class));
      possibleSpans.ifPresent(traces::add);
    });
    exInfo.getQueryTraceAsync().thenAccept(t -> {
      final Duration d = Duration.of(t.getDurationMicros(), ChronoUnit.MICROS);
      final long count = traces.stream().mapToInt(Set::size).count();
      LOGGER.info("Fetched {} spans ({} traces) in {}ms", count, traces.size(), d.toMillis());
    });
    return traces;
  }

  @Override
  public void deleteAll(final String landscapeToken) {
    final String deletionQuery =
        QueryBuilder.deleteFrom(DbHelper.KEYSPACE_NAME, DbHelper.TABLE_SPANS)
            .whereColumn(DbHelper.COL_TOKEN).isEqualTo(QueryBuilder.literal(landscapeToken))
            .asCql();
    this.db.getSession().execute(deletionQuery);
  }


}
