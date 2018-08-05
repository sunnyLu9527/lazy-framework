package com.htt.app.cache.lock;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.TimeUnit;

/**
 *  基于InterProcessMutex的zk分布式锁实现
 *  单元测试见 ZkLockTest.java
 *  https://www.jianshu.com/p/70151fc0ef5d
 */
public class ZkCuratorLock {

    private InterProcessMutex interProcessMutex;

    public ZkCuratorLock(String path){
        //设置初始重试等待时间和重试次数 避免因为网络抖动造成临时节点错误删除
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000,3);
        CuratorFramework curator = CuratorFrameworkFactory.builder().connectString("192.168.153.130:2181")
                .sessionTimeoutMs(5000).retryPolicy(retryPolicy).build();
        curator.start();
        interProcessMutex = new InterProcessMutex(curator,path);
    }

    public boolean acquire(long time, TimeUnit unit) throws Exception {
        return interProcessMutex.acquire(time, unit);
    }

    public void release() throws Exception{
        interProcessMutex.release();
    }
}
