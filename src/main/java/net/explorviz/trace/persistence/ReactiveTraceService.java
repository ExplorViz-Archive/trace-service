package net.explorviz.trace.persistence;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.explorviz.trace.persistence.dao.ReactiveTraceDao;
import net.explorviz.trace.persistence.dao.Trace;

/**
 * Service that leverages the reactive DAO {{@link ReactiveTraceDao}}.
 */
@ApplicationScoped
public class ReactiveTraceService {

  @Inject
  /* default */ Uni<ReactiveTraceDao> traceDaoReactive; // NOCS

  public Uni<Void> insert(final Trace trace) {
    return this.traceDaoReactive.flatMap(dao -> dao.insertAsync(trace));
  }

  public Uni<Void> deleteByLandscapeToken(final String landscapeTokenValue) {
    return this.traceDaoReactive.flatMap(dao -> dao.deleteAsync(landscapeTokenValue));
  }

  public Multi<Trace> getAllAsync(final String landscapeToken) {
    return this.traceDaoReactive.toMulti().flatMap(dao -> dao.getAllAsync(landscapeToken));
  }

  public Multi<Trace> getByStartTimeAndEndTime(final String landscapeToken, final long startTime,
      final long endTime) {
    return this.traceDaoReactive.toMulti()
        .flatMap(dao -> dao.getByStartTimeAndEndTime(landscapeToken, startTime, endTime));
  }

  public Multi<Trace> getByTraceId(final String landscapeToken, final String traceId) {
    return this.traceDaoReactive.toMulti()
        .flatMap(dao -> dao.getByTraceId(landscapeToken, traceId));
  }

  public Multi<Trace> cloneAllAsync(final String landscapeToken,
      final String clonedLandscapeToken) {
    return this.getAllAsync(clonedLandscapeToken).invoke(x -> x.setLandscapeToken(landscapeToken))
        .call(this::insert);
  }

}
