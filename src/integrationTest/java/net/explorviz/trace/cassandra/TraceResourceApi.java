package net.explorviz.trace.cassandra;

import static io.restassured.RestAssured.given;
import com.datastax.oss.quarkus.test.CassandraTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import net.explorviz.trace.persistence.ReactiveTraceService;
import net.explorviz.trace.persistence.dao.Trace;
import net.explorviz.trace.service.TraceConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@QuarkusTest
@QuarkusTestResource(KafkaTestResource.class)
@QuarkusTestResource(CassandraTestResource.class)
public class TraceResourceApi {

  // private static final Logger LOGGER = Logger.getLogger(TraceResourceIt.class);

  // Tests
  // - insert and retrieve single trace with 5 spans
  // - insert and retrieve 5 traces with 5 spans each
  // - filter by timestamp
  // - get by trace id

  @Inject
  ReactiveTraceService reactiveTraceService;

  @Test
  public void shouldSaveAndRetrieveEntityById() throws InterruptedException {

    final Trace expected = TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5));

    this.reactiveTraceService.insert(expected).await().indefinitely();

    final Response response = given().pathParam("landscapeToken", expected.getLandscapeToken())
        .when().get("/v2/landscapes/{landscapeToken}/dynamic");

    final net.explorviz.trace.persistence.dao.Trace[] body =
        response.getBody().as(net.explorviz.trace.persistence.dao.Trace[].class);

    Assertions.assertEquals(expected, body[0]);
  }

  @Test
  public void shouldSaveAndRetrieveAllEntitiesById() throws InterruptedException {

    final String landscapeToken1 = "a";
    final String landscapeToken2 = "b";

    final Trace expected1 =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken1));
    final Trace expected2 =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken1));
    final Trace expected3 =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken1));
    final Trace remainder4 =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken2));
    final Trace remainder5 =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken2));

    this.reactiveTraceService.insert(expected1).await().indefinitely();
    this.reactiveTraceService.insert(expected2).await().indefinitely();
    this.reactiveTraceService.insert(expected3).await().indefinitely();
    this.reactiveTraceService.insert(remainder4).await().indefinitely();
    this.reactiveTraceService.insert(remainder5).await().indefinitely();

    final Response response = given().pathParam("landscapeToken", landscapeToken1).when()
        .get("/v2/landscapes/{landscapeToken}/dynamic");

    final net.explorviz.trace.persistence.dao.Trace[] body =
        response.getBody().as(net.explorviz.trace.persistence.dao.Trace[].class);

    final List<Trace> actualTraceList = Arrays.asList(body);

    Assertions.assertTrue(actualTraceList.size() == 3);

    Assertions.assertTrue(actualTraceList.contains(expected1));
    Assertions.assertTrue(actualTraceList.contains(expected2));
    Assertions.assertTrue(actualTraceList.contains(expected3));
  }

  @Test
  public void retrieveMultipleEntitiesByTimestamp() throws InterruptedException {

    final String landscapeToken1 = "c";
    final String landscapeToken2 = "d";

    final long fromEpoch1 = 1605700800000L;
    final long toEpoch1 = 1605700810000L;

    final long fromEpoch2 = 1605700811000L;
    final long toEpoch2 = 1605700821000L;

    final Trace expected1 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken1, fromEpoch1, toEpoch1));
    final Trace expected2 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken1, toEpoch1, fromEpoch2));
    final Trace remainder3 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken1, fromEpoch2, toEpoch2));
    final Trace remainder4 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken2, fromEpoch1, toEpoch1));
    final Trace remainder5 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken2, fromEpoch2, toEpoch2));

    final long from = expected1.getStartTime();
    final long to = expected2.getStartTime();

    this.reactiveTraceService.insert(expected1).await().indefinitely();
    this.reactiveTraceService.insert(expected2).await().indefinitely();
    this.reactiveTraceService.insert(remainder3).await().indefinitely();
    this.reactiveTraceService.insert(remainder4).await().indefinitely();
    this.reactiveTraceService.insert(remainder5).await().indefinitely();

    final Response response =
        given().pathParam("landscapeToken", landscapeToken1).queryParam("from", from)
            .queryParam("to", to).when().get("/v2/landscapes/{landscapeToken}/dynamic");

    final net.explorviz.trace.persistence.dao.Trace[] body =
        response.getBody().as(net.explorviz.trace.persistence.dao.Trace[].class);

    final List<Trace> actualTraceList = Arrays.asList(body);

    Assertions.assertTrue(actualTraceList.size() == 2);

    Assertions.assertTrue(actualTraceList.contains(expected1));
    Assertions.assertTrue(actualTraceList.contains(expected2));
  }

  @Test
  public void testOutOfRangeByTimestamp() throws InterruptedException {

    final String landscapeToken1 = "c";
    final String landscapeToken2 = "d";

    final long fromEpoch1 = 1605700800000L;
    final long toEpoch1 = 1605700810000L;

    final long fromEpoch2 = 1605700811000L;
    final long toEpoch2 = 1605700821000L;

    final long outOfRangeFromMilli2 = 1605700822000L;

    final Trace remainder1 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken1, fromEpoch1, toEpoch1));
    final Trace remainder2 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken1, toEpoch1, fromEpoch2));
    final Trace remainder3 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken1, fromEpoch2, toEpoch2));
    final Trace remainder4 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken2, fromEpoch1, toEpoch1));
    final Trace remainder5 = TraceConverter
        .convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken2, fromEpoch2, toEpoch2));

    this.reactiveTraceService.insert(remainder1).await().indefinitely();
    this.reactiveTraceService.insert(remainder2).await().indefinitely();
    this.reactiveTraceService.insert(remainder3).await().indefinitely();
    this.reactiveTraceService.insert(remainder4).await().indefinitely();
    this.reactiveTraceService.insert(remainder5).await().indefinitely();

    final Response response = given().pathParam("landscapeToken", landscapeToken1)
        .queryParam("from", outOfRangeFromMilli2).when()
        .get("/v2/landscapes/{landscapeToken}/dynamic");

    final net.explorviz.trace.persistence.dao.Trace[] body =
        response.getBody().as(net.explorviz.trace.persistence.dao.Trace[].class);

    Assertions.assertTrue(body.length == 0);
  }

  @Test
  public void getSingleTraceByTraceId() throws InterruptedException {

    final String landscapeToken = "e";

    final Trace expected =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken));
    final Trace remainder =
        TraceConverter.convertTraceToDao(TraceHelper.randomTrace(5, landscapeToken));

    this.reactiveTraceService.insert(expected).await().indefinitely();
    this.reactiveTraceService.insert(remainder).await().indefinitely();

    final Response response = given().pathParam("landscapeToken", expected.getLandscapeToken())
        .pathParam("traceId", expected.getTraceId()).when()
        .get("/v2/landscapes/{landscapeToken}/dynamic/{traceId}");

    final net.explorviz.trace.persistence.dao.Trace[] body =
        response.getBody().as(net.explorviz.trace.persistence.dao.Trace[].class);

    Assertions.assertTrue(body.length == 1);

    Assertions.assertEquals(expected, body[0]);
  }

}
