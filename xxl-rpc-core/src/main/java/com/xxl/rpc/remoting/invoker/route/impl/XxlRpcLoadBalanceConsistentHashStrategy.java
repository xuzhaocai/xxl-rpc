package com.xxl.rpc.remoting.invoker.route.impl;

import com.xxl.rpc.remoting.invoker.route.XxlRpcLoadBalance;
import sun.rmi.runtime.Log;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


/**
 * consustent hash
 *
 * 单个JOB对应的每个执行器，使用频率最低的优先被选举
 *      a(*)、LFU(Least Frequently Used)：最不经常使用，频率/次数
 *      b、LRU(Least Recently Used)：最近最久未使用，时间
 * 一致性hash
 * @author xuxueli 2018-12-04
 */
public class XxlRpcLoadBalanceConsistentHashStrategy extends XxlRpcLoadBalance {

    private int VIRTUAL_NODE_NUM = 5;

    /**
     * get hash code on 2^32 ring (md5散列的方式计算hash值)
     * @param key
     * @return
     */
    private long hash(String key) {

        // md5 byte
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        byte[] keyBytes = null;
        try {
            keyBytes = key.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + key, e);
        }

        md5.update(keyBytes);
        byte[] digest = md5.digest();

        // hash code, Truncate to 32-bits
        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);

        long truncateHashCode = hashCode & 0xffffffffL;
        return truncateHashCode;
    }

    public String doRoute(String serviceKey, TreeSet<String> addressSet) {

        // ------A1------A2-------A3------
        // -----------J1------------------
        TreeMap<Long, String> addressRing = new TreeMap<Long, String>();
        for (String address: addressSet) {  // 虚拟节点  ， 防止故障后带来的雪崩问题
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                long addressHash = hash("SHARD-" + address + "-NODE-" + i);
                addressRing.put(addressHash, address);
            }
        }

        long jobHash = hash(serviceKey);  // 计算key 的hash
        SortedMap<Long, String> lastRing = addressRing.tailMap(jobHash);  // 选取  当前值 后面的 所有节点
        if (!lastRing.isEmpty()) {
            // 得到最近的一个
            return lastRing.get(lastRing.firstKey());
        }

        // 后面没有了 ， 就选择
        return addressRing.firstEntry().getValue();
    }

    @Override
    public String route(String serviceKey, TreeSet<String> addressSet) {
        String finalAddress = doRoute(serviceKey, addressSet);
        return finalAddress;
    }

    public static void main(String[] args) {
        XxlRpcLoadBalanceConsistentHashStrategy  xxlRpcLoadBalanceConsistentHashStrategy = new XxlRpcLoadBalanceConsistentHashStrategy();
        TreeSet<String> set= new TreeSet<>();
        set.add("192.168.7.144:8081");
        set.add("192.168.7.144:8082");
        set.add("192.168.7.144:8083");
        set.add("192.168.7.144:8084");
        set.add("192.168.7.144:8085");
        set.add("192.168.7.144:8086");

        while (true) {
            System.out.println( xxlRpcLoadBalanceConsistentHashStrategy.route("testKey", set));
        }








    }

}
