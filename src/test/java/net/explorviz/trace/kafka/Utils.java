package net.explorviz.trace.kafka;

public class Utils {

  public static KafkaConfig testKafkaConfigs() {
    KafkaConfig config = new KafkaConfig();
    config.inTopic = "in";
    config.outTopic = "out";
    config.schemaRegistryUrl = "localhost:8081";
    config.bootstrapServers = "localhost:9092";
    config.applicationId = "id";
    config.commitIntervalMs = 2000;
    return  config;
  }
}