package com.kongbig.sparkproject.test;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import java.util.*;

/**
 * Describe: 模拟实时数据
 * Author:   kongbig
 * Data:     2018/4/21 19:06.
 */
public class MockRealTimeData extends Thread {

    private static final Random random = new Random();
    private static final String[] provinces = new String[]{"江苏", "湖北", "湖南", "河南", "河北"};
    private static final Map<String, String[]> provinceCityMap = new HashMap<String, String[]>();

    private Producer<Integer, String> producer;

    public MockRealTimeData() {
        provinceCityMap.put("江苏", new String[]{"南京", "苏州"});
        provinceCityMap.put("湖北", new String[]{"武汉", "荆州"});
        provinceCityMap.put("湖南", new String[]{"长沙", "湘潭"});
        provinceCityMap.put("河南", new String[]{"郑州", "洛阳"});
        provinceCityMap.put("河北", new String[]{"石家庄", "唐山"});

        producer = new Producer<Integer, String>(createProducerConfig());
    }

    private ProducerConfig createProducerConfig() {
        Properties props = new Properties();
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("metadata.broker.list", "192.168.33.71:9092,192.168.33.72:9092,192.168.33.73:9092");
        return new ProducerConfig(props);
    }

    public void run() {
        while (true) {
            String province = provinces[random.nextInt(5)];
            String city = provinceCityMap.get(province)[random.nextInt(2)];

            String log = new Date().getTime() + " " + province + " " + city + " "
                    + random.nextInt(1000) + " " + random.nextInt(10);
            producer.send(new KeyedMessage<Integer, String>("AdRealTimeLog", log));

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 启动Kafka Producer（相当于kafka的客户端）
     *
     * @param args
     */
    public static void main(String[] args) {
        MockRealTimeData producer = new MockRealTimeData();
        producer.start();
    }

}
