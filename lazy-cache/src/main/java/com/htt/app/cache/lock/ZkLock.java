package com.htt.app.cache.lock;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 基于zookeeper的分布式锁
 * 单元测试见 ZkLockTest.java
 *  @see ZkCuratorLock   本类仅为了了解zk锁实现机制，如须要企业使用请使用ZkCuratorLock
 */
public class ZkLock {

    //zk锁的根节点路径
    private String rootPath = "/lock";
    //zk锁所在的节点路径 前缀必须带'/'
    private String lockPath;
    //当前设置的锁节点路径
    private String currentPath;
    //须要监控的前一个锁节点路径
    private String beforePath;

    private CuratorFramework curator;

    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * 初始化zkClient
     * @return
     */
    private CuratorFramework getZkClient(){
        if (curator != null)
            return curator;
        //设置初始重试等待时间和重试次数 避免因为网络抖动造成临时节点错误删除
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000,3);
        curator = CuratorFrameworkFactory.builder().connectString("192.168.153.130:2181")
                .sessionTimeoutMs(5000).retryPolicy(retryPolicy).build();
        curator.start();
        return curator;
    }

    /**
     * 第一步 首先须要确保锁的根节点是创建好的
     * @param lockPath
     */
    public ZkLock(String lockPath) {
        this.lockPath = lockPath;
        //创建分布式锁功能的根节点
        try {
            Stat stat = this.getZkClient().checkExists().forPath(rootPath);
            if (stat != null)
                return;
            this.getZkClient().create().creatingParentContainersIfNeeded().forPath(rootPath);
        } catch (Exception e) {
            logger.error("ZKLockException:",e);
            //zk连接过程中出现了不可预知的问题，此处须要抛业务异常，中断业务
            this.getZkClient().close();
            throw new RuntimeException(e);
        }
    }

    private boolean tryLock(){
        try {
            if (currentPath == null) {//仅当首次尝试创建节点时须要创建，后面的重试不须要进行此步骤
                //创建临时顺序子节点
                currentPath = this.getZkClient().create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(rootPath + lockPath);
                System.out.println(Thread.currentThread().getId()+"创建了临时顺序节点"+currentPath);
            }
            //获取所有子节点
            List<String> childNodeList = this.getZkClient().getChildren().forPath(rootPath);
            //排序
            Collections.sort(childNodeList);
            if (currentPath.equalsIgnoreCase(rootPath+"/"+childNodeList.get(0))){
                return true;//当前节点是最小的 无须监听 加锁成功
            } else {//当前节点不是最小的节点 须要监听前一个节点
                int currentIndex = childNodeList.indexOf(currentPath.substring(rootPath.length()+1));
                beforePath = rootPath+"/"+childNodeList.get(currentIndex-1);
                return false;
            }
        } catch (Exception e) {
            logger.error("ZKLockException:",e);
            //zk连接过程中出现了不可预知的问题，此处须要抛业务异常，中断业务
            this.getZkClient().close();
            throw new RuntimeException(e);
        }
    }

    /**
     * 加锁
     */
    public void lock(){
        if (!tryLock()){
            //阻塞等待
            waitForLock();
            //再次尝试获取锁
            lock();
        }
    }

    /**
     * 阻塞等待前面一个临时节点移除
     */
    private void waitForLock(){
        System.out.println(Thread.currentThread().getId()+"阻塞等待:"+beforePath);
        try {
            CountDownLatch cd = new CountDownLatch(1);
            //创建节点监听
            NodeCache nodeCache = new NodeCache(this.getZkClient(),beforePath);
            NodeCacheListener listener = () -> {
                ChildData data = nodeCache.getCurrentData();
                if (null == data){
                    cd.countDown();
                }
            };
            nodeCache.getListenable().addListener(listener);
            nodeCache.start();
            //阻塞等待
            if (this.getZkClient().checkExists().forPath(beforePath) != null){
                cd.await();
            }
            System.out.println(Thread.currentThread().getId()+"轮到我了");
            nodeCache.close();//取消监听
        } catch (Exception e){
            logger.error("ZKLockException:",e);
            //zk连接过程中出现了不可预知的问题，此处须要抛业务异常，中断业务
            this.getZkClient().close();
            throw new RuntimeException(e);
        }
    }

    /**
     * 释放锁
     */
    public void releaseLock(){
        try {
            this.getZkClient().delete().deletingChildrenIfNeeded().forPath(currentPath);
            this.getZkClient().close();
        } catch (Exception e) {
            //这一步释放锁 业务已经处理完毕，即使释放锁失败也不用再抛异常，因为zk断开连接后会清除临时节点
            logger.error("ZKLockException:",e);
        }
    }
}
