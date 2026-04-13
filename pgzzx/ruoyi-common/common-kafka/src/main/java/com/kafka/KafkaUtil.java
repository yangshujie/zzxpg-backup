package com.kafka;

import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Component
public class KafkaUtil {

    @Value("${datasource.kafka.address}")
    private String address;

    private  AdminClient admin;
    /**
     * 私有静态方法，创建Kafka生产者
     * @author o
     * @return KafkaProducer
     */
    private  KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        //声明kafka的地址
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,address);
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.1.17:9093,192.168.1.17:9094");
        //0、1 和 all：0表示只要把消息发送出去就返回成功；1表示只要Leader收到消息就返回成功；all表示所有副本都写入数据成功才算成功
        props.put("acks", "all");
        //重试次数
        props.put("retries", Integer.MAX_VALUE);
        //批处理的字节数
        props.put("batch.size", 30000);
        //批处理的延迟时间，当批次数据未满之时等待的时间
        props.put("linger.ms", 1);
        //用来约束KafkaProducer能够使用的内存缓冲的大小的，默认值32MB
        props.put("buffer.memory", 33554432);
        props.put("max.poll.records", "500");
        // properties.put("value.serializer",
        // "org.apache.kafka.common.serialization.ByteArraySerializer");
        // properties.put("key.serializer",
        // "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<String, String>(props);
    }

    private  KafkaProducer<String, byte[]> createProducerByte() {
        Properties props = new Properties();
        //声明kafka的地址
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,address);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.8.111:9092");
        //0、1 和 all：0表示只要把消息发送出去就返回成功；1表示只要Leader收到消息就返回成功；all表示所有副本都写入数据成功才算成功
        props.put("acks", "all");
        //重试次数
        props.put("retries", Integer.MAX_VALUE);
        //批处理的字节数
        props.put("batch.size", 16384);
        //批处理的延迟时间，当批次数据未满之时等待的时间
        props.put("linger.ms", 1);
        //用来约束KafkaProducer能够使用的内存缓冲的大小的，默认值32MB
        props.put("buffer.memory", 33554432);
        props.put("value.serializer",
         "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("key.serializer",
         "org.apache.kafka.common.serialization.ByteArraySerializer");
//        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
//        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return new KafkaProducer<String, byte[]>(props);
    }

    /**
     * 私有静态方法，创建Kafka消费者
     * @author o
     * @return KafkaConsumer
     */
    private  KafkaConsumer<String, byte[]> createConsumer(String groupId) {
        Properties props = new Properties();
        //声明kafka的地址
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,address);
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"192.168.1.17:9093,192.168.1.17:9094");
        //每个消费者分配独立的消费者组编号
        props.put("group.id", groupId);
        //如果value合法，则自动提交偏移量
        props.put("enable.auto.commit", "true");
        //设置多久一次更新被消费消息的偏移量
        props.put("auto.commit.interval.ms", "1000");
        //设置会话响应的时间，超过这个时间kafka可以选择放弃消费或者消费下一条消息
        props.put("session.timeout.ms", "30000");
        //自动重置offset
        props.put("auto.offset.reset","earliest");
        props.put("key.deserializer",
         "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.put("value.deserializer",
         "org.apache.kafka.common.serialization.ByteArrayDeserializer");
//        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
//        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        return new KafkaConsumer<String, byte[]>(props);
    }
    /**
     * 私有静态方法，创建Kafka集群管理员对象
     * @author o
     */
    public  void createAdmin(String servers){
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,servers);
        admin = AdminClient.create(props);
    }

    /**
     * 私有静态方法，创建Kafka集群管理员对象
     * @author o
     * @return AdminClient
     */
    private  void createAdmin(){
        createAdmin(address);
    }

    /**
     * 传入kafka约定的topic,json格式字符串，发送给kafka集群
     * @author o
     * @param topic
     * @param jsonMessage
     */
    public  void sendMessage(String topic, String jsonMessage) {
        KafkaProducer<String, String> producer = createProducer();
        producer.send(new ProducerRecord<String, String>(topic, jsonMessage));
        producer.close();
    }

    /**
     * 传入kafka约定的topic,json格式字符串，发送给kafka集群
     * @author o
     * @param topic
     * @param bytesMessage
     */
    public  void sendMessageByte(String topic, byte[] bytesMessage) {
        KafkaProducer<String, byte[]> producer = createProducerByte();
        producer.send(new ProducerRecord<String,  byte[]>(topic, bytesMessage));
        producer.close();
    }

    /**
     * 传入kafka约定的topic消费数据，用于测试，数据最终会输出到控制台上
     * @author o
     * @param topic
     */
    public  void consume(String groupId, String topic) {
        KafkaConsumer<String, byte[]> consumer = createConsumer(groupId);
        consumer.subscribe(Collections.singletonList(topic));
        while (true) {
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(100));
            for (ConsumerRecord<String, byte[]> record : records){
                System.out.printf(groupId + " | offset = %d, key = %s, value = %s",record.offset(), record.key(), Arrays.toString(record.value()));
                System.out.println();
            }
        }
    }
    /**
     * 传入kafka约定的topic数组，消费数据
     * @author o
     * @param topics
     */
    public void consume(String groupId, String ... topics) {
        KafkaConsumer<String, byte[]> consumer = createConsumer(groupId);
        consumer.subscribe(Arrays.asList(topics));
        while (true) {
            ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofSeconds(100));
            for (ConsumerRecord<String, byte[]> record : records){
                System.out.printf("offset = %d, key = %s, value = %s",record.offset(), record.key(), Arrays.toString(record.value()));
                System.out.println();
            }
        }
    }
    /**
     * 传入kafka约定的topic,json格式字符串数组，发送给kafka集群
     * 用于批量发送消息，性能较高。
     * @author o
     * @param topic
     * @param jsonMessages
     * @throws InterruptedException
     */
    public  void sendMessage(String topic, String... jsonMessages) throws InterruptedException {
        KafkaProducer<String, String> producer = createProducer();
        for (String jsonMessage : jsonMessages) {
            producer.send(new ProducerRecord<String, String>(topic, jsonMessage));
        }
        producer.close();
    }

    /**
     * 传入kafka约定的topic,Map集合，内部转为json发送给kafka集群 <br>
     * 用于批量发送消息，性能较高。
     * @author o
     * @param topic
     * @param mapMessageToJSONForArray
     */
    public  void sendMessage(String topic, List<Map<Object, Object>> mapMessageToJSONForArray) {
        KafkaProducer<String, String> producer = createProducer();
        for (Map<Object, Object> mapMessageToJSON : mapMessageToJSONForArray) {
            String array = JSONObject.toJSONString(mapMessageToJSON);
            producer.send(new ProducerRecord<String, String>(topic, array));
        }
        producer.close();
    }

    /**
     * 传入kafka约定的topic,Map，内部转为json发送给kafka集群
     * @author o
     * @param topic
     * @param mapMessageToJSON
     */
    public void sendMessage(String topic, Map<Object, Object> mapMessageToJSON) {
        KafkaProducer<String, String> producer = createProducer();
        String array = JSONObject.toJSONString(mapMessageToJSON);
        producer.send(new ProducerRecord<String, String>(topic, array));
        producer.close();
    }

    /**
     * 创建主题
     * @author o
     * @param name 主题的名称
     * @param numPartitions 主题的分区数
     * @param replicationFactor 主题的每个分区的副本因子
     */
    public  void createTopic(String name,int numPartitions,int replicationFactor){
        if(admin == null) {
            createAdmin();
        }
        Map<String, String> configs = new HashMap<>();
        CreateTopicsResult result = admin.createTopics(Arrays.asList(new NewTopic(name, numPartitions, (short) replicationFactor).configs(configs)));
        //以下内容用于判断创建主题的结果
        for (Map.Entry<String, KafkaFuture<Void>> entry : result.values().entrySet()) {
            try {
                entry.getValue().get();
                System.out.println("topic "+entry.getKey()+" created");
            } catch (InterruptedException | ExecutionException e) {
                if (ExceptionUtils.getRootCause(e) instanceof TopicExistsException) {
                    System.out.println("topic "+entry.getKey()+" existed");
                }
            }
        }
    }

    /**
     * 删除主题
     * @author o
     * @param names 主题的名称
     */
    public void deleteTopic(String ... names){
        if(admin == null) {
            createAdmin();
        }
        Map<String, String> configs = new HashMap<>();
        Collection<String> topics = Arrays.asList(names);
        DeleteTopicsResult result = admin.deleteTopics(topics);
        //以下内容用于判断删除主题的结果
        for (Map.Entry<String, KafkaFuture<Void>> entry : result.values().entrySet()) {
            try {
                entry.getValue().get();
                System.out.println("topic "+entry.getKey()+" deleted");
            } catch (InterruptedException | ExecutionException e) {
                if (ExceptionUtils.getRootCause(e) instanceof UnknownTopicOrPartitionException) {
                    System.out.println("topic "+entry.getKey()+" not exist");
                }
            }
        }
    }
    /**
     * 查看主题详情
     * @author o
     * @param names 主题的名称
     */
    public void describeTopic(String name,String ... names){
        if(admin == null) {
            createAdmin();
        }
        Map<String, String> configs = new HashMap<>();
        Collection<String> topics = Arrays.asList(names);
        topics.add(name);
        DescribeTopicsResult result = admin.describeTopics(topics);
        //以下内容用于显示主题详情的结果
        for (Map.Entry<String, KafkaFuture<TopicDescription>> entry : result.values().entrySet()) {
            try {
                entry.getValue().get();
                System.out.println("topic "+entry.getKey()+" describe");
                System.out.println("\t name: "+entry.getValue().get().name());
                System.out.println("\t partitions: ");
                entry.getValue().get().partitions().stream().forEach(p-> {
                    System.out.println("\t\t index: "+p.partition());
                    System.out.println("\t\t\t leader: "+p.leader());
                    System.out.println("\t\t\t replicas: "+p.replicas());
                    System.out.println("\t\t\t isr: "+p.isr());
                });
                System.out.println("\t internal: "+entry.getValue().get().isInternal());
            } catch (InterruptedException | ExecutionException e) {
                if (ExceptionUtils.getRootCause(e) instanceof UnknownTopicOrPartitionException) {
                    System.out.println("topic "+entry.getKey()+" not exist");
                }
            }
        }
    }

    /**
     * 查看主题列表
     * @author o
     * @return Set<String> TopicList
     */
    public Set<String> listTopic(){
        if(admin == null) {
            createAdmin();
        }
        ListTopicsResult result = admin.listTopics();
        try {
            result.names().get().stream().map(x->x+"\t").forEach(System.out::print);
            return result.names().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    //=========================flinkkafka======================
    public FlinkKafkaProducer<String> getKafkaProducer(String topic) {
        return new FlinkKafkaProducer<String>(address,
                topic,
                new SimpleStringSchema());
    }

    public <T> FlinkKafkaProducer<T> getKafkaProducer(KafkaSerializationSchema<T> kafkaSerializationSchema,String topic) {

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, address);

        return new FlinkKafkaProducer<T>(topic,
                kafkaSerializationSchema,
                properties,
                FlinkKafkaProducer.Semantic.EXACTLY_ONCE);
    }

    public FlinkKafkaConsumer<String> getKafkaConsumer(String topic, String groupId) {

        Properties properties = new Properties();

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, address);
//        properties.put("acks", "all");
//        //重试次数
//        properties.put("retries", Integer.MAX_VALUE);
        //批处理的字节数
        properties.put("batch.size", 60000);
        //批处理的延迟时间，当批次数据未满之时等待的时间
        properties.put("linger.ms", 1);
        //用来约束KafkaProducer能够使用的内存缓冲的大小的，默认值32MB
        properties.put("buffer.memory", 33554432);
        properties.put("max.poll.records", "1000");  // 设置的小一点，可以限制速度
        properties.setProperty("fetch.min.bytes", "100000"); // 设置 fetch.min.bytes
        properties.setProperty("fetch.max.wait.ms", "100"); // 设置 fetch.max.wait.ms
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        return new FlinkKafkaConsumer<String>(topic,
                new SimpleStringSchema(),
                properties);

    }

    //拼接Kafka相关属性到DDL
    public  String getKafkaDDL(String topic, String groupId) {
        return " 'connector' = 'kafka', " +
                " 'topic' = '" + topic + "'," +
                " 'properties.bootstrap.servers' = '" + address + "', " +
                " 'properties.group.id' = '" + groupId + "', " +
                " 'format' = 'json', " +
                " 'scan.startup.mode' = 'latest-offset'  ";
    }

    public  void main(String[] args) {
        System.out.println(listTopic());
    }
}