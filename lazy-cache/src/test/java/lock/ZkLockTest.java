package lock;

import com.htt.app.cache.lock.ZkLock;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class ZkLockTest {

    /**
     * 模拟多个线程并发访问的情况
     */
    @Test
    public void testLock(){

        CountDownLatch cd = new CountDownLatch(1);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                ZkLock zkLock = new ZkLock("/test");
                try {
                    cd.await();
                    zkLock.lock();
                    System.out.println(Thread.currentThread().getId()+"获取到锁，执行业务...");
                    Thread.sleep(500);
                    System.out.println(Thread.currentThread().getId()+"业务执行完毕，释放锁...");
                } catch (Exception e){
                    e.printStackTrace();
                } finally {
                    zkLock.releaseLock();
                }
            }).start();
        }
        cd.countDown();
        try {//这里junit须要睡一下，否则单元测试跑完资源立即释放
            System.out.println("睡一下");
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 不存在并发的情况下测试锁的功能是否正常
     */
    @Test
    public void test2(){
        try {
            ZkLock zkLock = new ZkLock("/test");
            zkLock.lock();
            System.out.println(Thread.currentThread().getId()+"获取到锁，执行业务...");
            Thread.sleep(5000);
            zkLock.releaseLock();
            System.out.println(Thread.currentThread().getId()+"业务执行完毕，释放锁...");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

}
