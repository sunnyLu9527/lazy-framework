package com.htt.app.cache.lock;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;

import java.util.concurrent.TimeUnit;

/**
 *  基于InterProcessMutex的zk分布式锁实现
 *  单元测试见 ZkLockTest.java
 *  https://www.jianshu.com/p/70151fc0ef5d
 */
public class ZkCuratorLock {

    private InterProcessMutex interProcessMutex;

    private CuratorFramework curator;

    public ZkCuratorLock(String path){
        //设置初始重试等待时间和重试次数 避免因为网络抖动造成临时节点错误删除
        //必须指定 因为一旦客户端断开连接临时节点就删除了，以下是几种重试策略
        //ExponentialBackoffRetry:重试指定的次数, 且每一次重试之间停顿的时间逐渐增加.
        //RetryNTimes:指定最大重试次数的重试策略
        //RetryOneTime:仅重试一次
        //RetryUntilElapsed:一直重试直到达到规定的时间
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000,3);
        curator = CuratorFrameworkFactory.builder().connectString("192.168.50.102:2181")
                .sessionTimeoutMs(5000).retryPolicy(retryPolicy).build();
        curator.start();
        interProcessMutex = new InterProcessMutex(curator,path);
    }

    private ZkCuratorLock(){}

    /**
     * 获取锁 超时版
     * @param time 获取锁等待时间
     * @param unit 时间单位
     * @return
     * @throws Exception
     */
    public boolean acquire(long time, TimeUnit unit) throws Exception {
        return interProcessMutex.acquire(time, unit);
    }

    /**
     * 获取锁
     * 该锁会始终阻塞，直到可用
     * @throws Exception
     */
    public void acquire() throws Exception {
        interProcessMutex.acquire();
    }

    /**
     * 释放锁
     * @throws Exception
     */
    public void release() throws Exception{
        interProcessMutex.release();
    }

    /**
     * 关闭zookeeper连接
     * @throws Exception
     */
    public void close(){
        CloseableUtils.closeQuietly(curator);
    }
}
