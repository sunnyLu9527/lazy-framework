package com.htt.app.cache.lock;

import com.htt.app.cache.utils.JedisUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import redis.clients.jedis.Jedis;

import java.util.Collections;

/**
 * 分布式锁支持
 * 基于reids
 * Created by sunnyLu on 2018/6/1.
 */
public class LockSupport {

    private String SET_IF_NOT_EXIST = "NX";
    private String SET_WITH_EXPIRE_TIME = "PX";
    private String LOCK_SUCCESS = "OK";
    private Long RELEASE_SUCCESS = 1L;

    private Logger logger = LogManager.getLogger(this.getClass());

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁 key
     * @param requestId 请求编号 作为锁的 value
     * @param expireTime 过期时间 踩坑的经验，单位是 ms ！！！
     * @return
     */
    public Boolean tryGetLock(String lockKey,String requestId,int expireTime){
        //默认重试3次
        return this.lock(lockKey, requestId, expireTime,Boolean.TRUE);
    }

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁 key
     * @param requestId 请求编号 作为锁的 value
     * @param expireTime 过期时间
     * @param retry 竞争失败后是否重试
     * @return
     */
    public Boolean tryGetLock(String lockKey,String requestId,int expireTime,Boolean retry){
        return this.lock(lockKey, requestId, expireTime,retry);
    }

    private Boolean lock(String lockKey,String requestId,int expireTime,Boolean isRetry){
        Jedis jedis = null;
        int retry = isRetry ? 3 : 1;
        try {
            jedis = JedisUtils.getJedis(JedisUtils.DATA_BASE);
            String result = jedis.set(lockKey,requestId,SET_IF_NOT_EXIST,SET_WITH_EXPIRE_TIME,expireTime);
            while(retry > 0){
                if (LOCK_SUCCESS.equalsIgnoreCase(result)) {
                    logger.error("加锁成功...lockKey:"+lockKey+";requestId:"+requestId);
                    return Boolean.TRUE;
                }
                logger.error("未竞争到锁，250ms后再尝试获取...lockKey:"+lockKey+";requestId:"+requestId);
                Thread.sleep(250);//基于dubbo的调用默认超时时间为1s,所以休眠时间过长没有任何意义
                retry--;
                continue;
            }
            logger.error("竞争锁失败...lockKey:"+lockKey+";requestId:"+requestId);
            return Boolean.FALSE;
        } catch (Exception e) {
            logger.error("加锁异常！",e);
            return Boolean.FALSE;
        } finally {
            JedisUtils.returnJedis(jedis);
        }
    }

    /**
     * 释放锁
     * @param lockKey 锁 key
     * @param requestId 请求编号 对应锁的value
     * @return
     */
    public Boolean releaseLock(String lockKey,String requestId){
        Jedis jedis = null;
        try {
            jedis = JedisUtils.getJedis(JedisUtils.DATA_BASE);
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
            if (RELEASE_SUCCESS.equals(result)) {
                logger.error("锁释放成功...lockKey:"+lockKey+";requestId:"+requestId);
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        } catch (Exception e) {
            logger.error("锁释放异常！",e);
            return Boolean.FALSE;
        } finally {
            JedisUtils.returnJedis(jedis);
        }
    }
}
