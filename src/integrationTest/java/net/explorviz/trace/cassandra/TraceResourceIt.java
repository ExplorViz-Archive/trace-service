package net.explorviz.trace.cassandra;

import com.datastax.oss.quarkus.test.CassandraTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.explorviz.trace.helper.TraceHelper;
import net.explorviz.trace.persistence.ReactiveTraceService;
import net.explorviz.trace.persistence.dao.Trace;
import net.explorviz.trace.service.TraceConverter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTestResource(CassandraTestResource.class)
public class TraceResourceIt {

  // private static final Logger LOGGER = LoggerFactory.getLogger(TraceResourceIt.class);

  @Inject
  ReactiveTraceService reactiveTraceService;

  @Test
  public void shouldSaveAndRetrieveEntityById() throws InterruptedException {

    final Trace expected = TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5));

    this.reactiveTraceService.insert(expected).await().indefinitely();

    final Trace actual =
        this.reactiveTraceService.getByTraceId(expected.getLandscapeToken(), expected.getTraceId())
            .collect().first().await().indefinitely();

    Assertions.assertEquals(expected, actual);
  }

  @Test
  public void shouldSaveAndRetrieveMultipleEntitiesById() throws InterruptedException {

    final Trace expected1 = TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5));
    final Trace expected2 = TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5));
    final Trace expected3 = TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5));
    final Trace expected4 = TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5));
    final Trace expected5 = TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5));

    this.reactiveTraceService.insert(expected1).await().indefinitely();
    this.reactiveTraceService.insert(expected2).await().indefinitely();
    this.reactiveTraceService.insert(expected3).await().indefinitely();
    this.reactiveTraceService.insert(expected4).await().indefinitely();
    this.reactiveTraceService.insert(expected5).await().indefinitely();

    final Trace actual1 = this.reactiveTraceService
        .getByTraceId(expected1.getLandscapeToken(), expected1.getTraceId()).collect().first()
        .await().indefinitely();

    final Trace actual2 = this.reactiveTraceService
        .getByTraceId(expected2.getLandscapeToken(), expected2.getTraceId()).collect().first()
        .await().indefinitely();

    final Trace actual3 = this.reactiveTraceService
        .getByTraceId(expected3.getLandscapeToken(), expected3.getTraceId()).collect().first()
        .await().indefinitely();

    final Trace actual4 = this.reactiveTraceService
        .getByTraceId(expected4.getLandscapeToken(), expected4.getTraceId()).collect().first()
        .await().indefinitely();

    final Trace actual5 = this.reactiveTraceService
        .getByTraceId(expected5.getLandscapeToken(), expected5.getTraceId()).collect().first()
        .await().indefinitely();

    Assertions.assertEquals(expected1, actual1);
    Assertions.assertEquals(expected2, actual2);
    Assertions.assertEquals(expected3, actual3);
    Assertions.assertEquals(expected4, actual4);
    Assertions.assertEquals(expected5, actual5);
  }

  @Test
  public void shouldSaveAndRetrieveAllEntities() throws InterruptedException {

    final String landscapeToken = RandomStringUtils.random(32, true, true);

    final Trace expected1 =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken));
    final Trace expected2 =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken));
    final Trace expected3 =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken));
    final Trace expected4 =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken));
    final Trace expected5 =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken));

    final List<Trace> expectedList = new ArrayList<>();
    expectedList.add(expected1);
    expectedList.add(expected2);
    expectedList.add(expected3);
    expectedList.add(expected4);
    expectedList.add(expected5);

    this.reactiveTraceService.insert(expected1).await().indefinitely();
    this.reactiveTraceService.insert(expected2).await().indefinitely();
    this.reactiveTraceService.insert(expected3).await().indefinitely();
    this.reactiveTraceService.insert(expected4).await().indefinitely();
    this.reactiveTraceService.insert(expected5).await().indefinitely();

    final List<Trace> actualTraceList = this.reactiveTraceService.getAllAsync(landscapeToken)
        .collect().asList().await().indefinitely();

    Assertions.assertTrue(expectedList.size() == actualTraceList.size());

    Assertions.assertTrue(actualTraceList.contains(expected1));
    Assertions.assertTrue(actualTraceList.contains(expected2));
    Assertions.assertTrue(actualTraceList.contains(expected3));
    Assertions.assertTrue(actualTraceList.contains(expected4));
    Assertions.assertTrue(actualTraceList.contains(expected5));
  }

  @Test
  public void shouldSaveAndRetrieveEntitiesByTimestamp() throws InterruptedException {

    final String landscapeToken = RandomStringUtils.random(32, true, true);

    final long fromSeconds1 = 1605700800000L;
    final long toSeconds1 = 1605700810000L;

    final long fromSeconds2 = 1605700811000L;
    final long toSeconds2 = 1605700821000L;

    final Trace expected1 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken, fromSeconds1, toSeconds1));
    final Trace expected2 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken, fromSeconds1, toSeconds1));
    final Trace expected3 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken, fromSeconds1, toSeconds1));
    final Trace remainder4 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken, fromSeconds2, toSeconds2));
    final Trace remainder5 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken, fromSeconds2, toSeconds2));

    long filteringKey = Math.min(expected1.getStartTime(), expected2.getStartTime());

    filteringKey = Math.min(filteringKey, expected3.getStartTime());

    final List<Trace> expectedList = new ArrayList<>();
    expectedList.add(expected1);
    expectedList.add(expected2);
    expectedList.add(expected3);

    this.reactiveTraceService.insert(expected1).await().indefinitely();
    this.reactiveTraceService.insert(expected2).await().indefinitely();
    this.reactiveTraceService.insert(expected3).await().indefinitely();
    this.reactiveTraceService.insert(remainder4).await().indefinitely();
    this.reactiveTraceService.insert(remainder5).await().indefinitely();

    final List<Trace> actualTraceList = this.reactiveTraceService
        .getByStartTimeAndEndTime(landscapeToken, filteringKey, filteringKey + 1000).collect()
        .asList().await().indefinitely();

    Assertions.assertEquals(expectedList.size(), actualTraceList.size());

    Assertions.assertTrue(actualTraceList.contains(expected1));
    Assertions.assertTrue(actualTraceList.contains(expected2));
    Assertions.assertTrue(actualTraceList.contains(expected3));
  }

  @Test
  public void cloneToken() throws InterruptedException {

    final String landscapeToken = RandomStringUtils.random(32, true, true);

    final long fromSeconds1 = 1605700800000L;
    final long toSeconds1 = 1605700810000L;

    final Trace expected1 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken, fromSeconds1, toSeconds1));
    final Trace expected2 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken, fromSeconds1, toSeconds1));
    final Trace expected3 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken, fromSeconds1, toSeconds1));

    final List<Trace> expectedList = new ArrayList<>();
    expectedList.add(expected1);
    expectedList.add(expected2);
    expectedList.add(expected3);

    this.reactiveTraceService.insert(expected1).await().indefinitely();
    this.reactiveTraceService.insert(expected2).await().indefinitely();
    this.reactiveTraceService.insert(expected3).await().indefinitely();

    final String newLandscapeToken = RandomStringUtils.random(32, true, true);
    final var newToken = this.reactiveTraceService.getAllAsync(newLandscapeToken).collect().asList()
        .await().indefinitely();
    final var clonedToken = this.reactiveTraceService.getAllAsync(landscapeToken).collect().asList()
        .await().indefinitely();

    Assertions.assertEquals(0, newToken.size());
    Assertions.assertEquals(3, clonedToken.size());

    this.reactiveTraceService.cloneAllAsync(newLandscapeToken, landscapeToken).collect().asList()
        .await().indefinitely();

    final var result = this.reactiveTraceService.getAllAsync(newLandscapeToken).collect().asList()
        .await().indefinitely();

    Assertions.assertEquals(3, result.size());
  }


}
