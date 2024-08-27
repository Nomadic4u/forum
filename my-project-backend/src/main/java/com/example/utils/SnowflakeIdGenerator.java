package com.example.utils;

import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 */
@Component // 将这个类标记为Spring组件，允许Spring自动管理它的生命周期
public class SnowflakeIdGenerator {

    // 雪花算法的开始时间戳（自定义），用于计算相对时间
    private static final long START_TIMESTAMP = 1691087910202L;

    // 数据中心ID和工作节点ID占用的位数
    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L; // 序列号占用的位数

    // 数据中心ID和工作节点ID的最大值（根据位数计算得出）
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS); // 序列号的最大值

    // 各部分在最终ID中的移位数量
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS; // 工作节点ID的移位
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS; // 数据中心ID的移位
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS; // 时间戳的移位

    // 数据中心ID、工作节点ID以及序列号的变量声明
    private final long dataCenterId;
    private final long workerId;
    private long lastTimestamp = -1L; // 记录上一次生成ID时的时间戳
    private long sequence = 0L; // 序列号，用于同一毫秒内的多ID生成

    // 默认构造函数，使用默认的数据中心ID和工作节点ID（1, 1）
    public SnowflakeIdGenerator() {
        this(1, 1);
    }

    // 带参数的构造函数，允许用户指定数据中心ID和工作节点ID
    private SnowflakeIdGenerator(long dataCenterId, long workerId) {
        // 校验数据中心ID和工作节点ID是否合法
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("Data center ID can't be greater than " + MAX_DATA_CENTER_ID + " or less than 0");
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker ID can't be greater than " + MAX_WORKER_ID + " or less than 0");
        }
        this.dataCenterId = dataCenterId;
        this.workerId = workerId;
    }

    /**
     * 生成一个新的雪花算法ID（线程安全）
     *
     * @return 雪花ID
     */
    public synchronized long nextId() {
        long timestamp = getCurrentTimestamp(); // 获取当前的时间戳

        // 如果当前时间戳小于上次生成ID的时间戳，说明系统时钟出现问题
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("Clock moved backwards. Refusing to generate ID.");
        }

        // 如果当前时间戳等于上次生成ID的时间戳，说明是在同一毫秒内
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE; // 在同一毫秒内生成的ID需要增加序列号
            if (sequence == 0) { // 如果序列号超过最大值
                timestamp = getNextTimestamp(lastTimestamp); // 获取下一个有效的时间戳
            }
        } else {
            sequence = 0L; // 不同毫秒的情况，序列号重置为0
        }

        lastTimestamp = timestamp; // 更新上次生成ID的时间戳

        // 生成ID并返回：时间戳部分 | 数据中心ID部分 | 工作节点ID部分 | 序列号部分
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT) |
                (dataCenterId << DATA_CENTER_ID_SHIFT) |
                (workerId << WORKER_ID_SHIFT) |
                sequence;
    }

    // 获取当前系统时间戳
    private long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    // 获取下一毫秒的时间戳，直到大于上一次的时间戳为止
    private long getNextTimestamp(long lastTimestamp) {
        long timestamp = getCurrentTimestamp();
        while (timestamp <= lastTimestamp) {
            timestamp = getCurrentTimestamp();
        }
        return timestamp;
    }
}
