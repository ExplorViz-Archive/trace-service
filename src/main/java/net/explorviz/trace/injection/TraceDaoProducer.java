package net.explorviz.trace.injection;

import com.datastax.oss.quarkus.runtime.api.session.QuarkusCqlSession;
import io.smallrye.mutiny.Uni;
import java.time.Duration;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import net.explorviz.trace.persistence.dao.TraceDaoReactive;
import net.explorviz.trace.persistence.dao.TraceMapper;
import net.explorviz.trace.persistence.dao.TraceMapperBuilder;

/**
 * Factory / Producer for the {@link TraceDaoReactive}.
 */
public class TraceDaoProducer {

  private static final long CQL_TIMEOUT_SECONDS = 5;

  private final TraceDaoReactive spanDynamicDaoReactive;

  @Inject
  public TraceDaoProducer(final Uni<QuarkusCqlSession> session) {

    // create a mapper
    final TraceMapper mapper = new TraceMapperBuilder(session.await().atMost(
        Duration.ofSeconds(CQL_TIMEOUT_SECONDS))).build();

    // instantiate our Daos
    this.spanDynamicDaoReactive = mapper.traceDaoReactive();
  }

  @Produces
  @ApplicationScoped
    /* default */ TraceDaoReactive produceSpanDynamicDaoReactive() { // NOPMD
    return this.spanDynamicDaoReactive;
  }

}
