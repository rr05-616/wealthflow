package com.wealthflow;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
@SpringBootApplication
public class WealthflowApplication {
 public static void main(String[] args){
  if(Boolean.parseBoolean(System.getenv().getOrDefault("WEALTHFLOW_EMBEDDED_KAFKA","false"))){
   var broker=new EmbeddedKafkaKraftBroker(1,1,"soa.s08.t24.events");
   broker.afterPropertiesSet();
   System.setProperty("spring.kafka.bootstrap-servers",broker.getBrokersAsString());
   Runtime.getRuntime().addShutdownHook(new Thread(broker::destroy,"embedded-kafka-shutdown"));
   System.out.println("Local Kafka broker ready at "+broker.getBrokersAsString());
  }
  SpringApplication.run(WealthflowApplication.class,args);
 }
}