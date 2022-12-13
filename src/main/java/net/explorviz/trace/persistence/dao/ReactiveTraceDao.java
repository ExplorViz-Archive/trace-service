package net.explorviz.trace.persistence.dao;


import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Delete;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Select;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

/**
 * Datastax Dao for a {@link Trace}.
 */
@Dao
public interface ReactiveTraceDao {

  @Insert
  Uni<Void> insertAsync(Trace trace);

  @Delete(customWhereClause = "landscape_token = :id", entityClass = Trace.class)
  Uni<Void> deleteAsync(String id);

  @Select(customWhereClause = "landscape_token = :id")
  Multi<Trace> getAllAsync(String id);

  @Select(customWhereClause = "landscape_token = :id and start_time >= :startTime and "
      + "start_time <= :endTime")
  Multi<Trace> getByStartTimeAndEndTime(String id, long startTime, long endTime);

  @Select(customWhereClause = "landscape_token = :id and trace_id = :traceId")
  Multi<Trace> getByTraceId(String id, String traceId);
}

