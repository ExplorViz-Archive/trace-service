package net.explorviz.trace.kafka;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.quarkus.arc.profile.IfBuildProfile;
import java.io.IOException;
import java.util.Map;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import net.explorviz.avro.Trace;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Returns an injectable {@link SpecificAvroSerde}.
 */
@Dependent
public class MockSerdeTraceProducer {

  @ConfigProperty(name = "explorviz.kafka-streams.topics.in")
  /* default */ String inTopicStructure;

  @Inject
  /* default */ SchemaRegistryClient registry;

  @Produces
  @IfBuildProfile("test")
  public SpecificAvroSerde<Trace> produceMockSpecificAvroSerde()
      throws IOException, RestClientException {

    this.registry.register(this.inTopicStructure + "-value", new AvroSchema(Trace.SCHEMA$));

    final SpecificAvroSerde<Trace> valueSerde = new SpecificAvroSerde<>(this.registry);
    valueSerde.configure(
        Map.of(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://registry:1234"),
        false);
    return valueSerde;
  }
}

