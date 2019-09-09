package io.confluent.examples.pcf.servicebroker;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.common.internals.Topic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.servicebroker.model.instance.*;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class ConfluentPlatformServiceInstanceService implements ServiceInstanceService {

    @Autowired
    private TopicCreator topicCreator;

    @Value( "${replication.factor}" )
    private short replicationFactor;

    @Autowired
    private ServiceInstanceRepository serviceInstanceRepository;

    @Override
    public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest createServiceInstanceRequest) {
        String topic = (String) createServiceInstanceRequest.getParameters().get("topic_name");
        if (topic == null || topic.isEmpty()) {
            throw new RuntimeException("topic name is missing.");
        }
        try {
            topicCreator.create(topic, 3, replicationFactor);
            serviceInstanceRepository.save(
                    TopicServiceInstance.builder().created(new Date()).topicName(topic).uuid(UUID.randomUUID()).build()
            );
        } catch (ExecutionException | InterruptedException | JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return Mono.just(
                CreateServiceInstanceResponse.builder()
                        .async(false)
                        .instanceExisted(false)
                        .build()
        );
    }

    public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
        TopicServiceInstance topicServiceInstance = serviceInstanceRepository.get(UUID.fromString(request.getServiceInstanceId()));
        GetServiceInstanceResponse response = GetServiceInstanceResponse.builder().parameters(Map.of("topic", topicServiceInstance.topicName)).build();
        return Mono.just(response);
    }

    @Override
    public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest deleteServiceInstanceRequest) {
        return null;
    }
}
