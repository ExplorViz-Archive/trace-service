package net.explorviz.trace.persistence.dao;

import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;
import io.smallrye.mutiny.Uni;

/**
 * Datastax Dao Mapper for {@link Trace}.
 */
@Mapper
public interface TraceMapper {

  @DaoFactory
  ReactiveTraceDao traceDaoReactiveSync();

  @DaoFactory
  Uni<ReactiveTraceDao> traceDaoReactiveUni();

}
