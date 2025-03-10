package com.xxl.rpc.remoting.invoker.route.impl;

import com.xxl.rpc.remoting.invoker.route.XxlRpcLoadBalance;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * lru
 * 最近最少使用
 * @author xuxueli 2018-12-04
 */
public class XxlRpcLoadBalanceLRUStrategy extends XxlRpcLoadBalance {
    //  存储 serviceKey 与 address
    private ConcurrentMap<String, LinkedHashMap<String, String>> jobLRUMap = new ConcurrentHashMap<String, LinkedHashMap<String, String>>();
    private long CACHE_VALID_TIME = 0;

    public String doRoute(String serviceKey, TreeSet<String> addressSet) {

        // cache clear 过段时间清空 map
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            jobLRUMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000*60*60*24;
        }

        // init lru
        LinkedHashMap<String, String> lruItem = jobLRUMap.get(serviceKey);
        if (lruItem == null) {  /// 初始化
            /**
             * LinkedHashMap
             *      a、accessOrder：ture=访问顺序排序（get/put时排序）/ACCESS-LAST；false=插入顺序排期/FIFO；
             *      b、removeEldestEntry：新增元素时将会调用，返回true时会删除最老元素；可封装LinkedHashMap并重写该方法，比如定义最大容量，超出是返回true即可实现固定长度的LRU算法；
             */
            lruItem = new LinkedHashMap<String, String>(16, 0.75f, true){
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    if(super.size() > 1000){   // size 超过1000
                        return true;
                    }else{
                        return false;
                    }
                }
            };
            jobLRUMap.putIfAbsent(serviceKey, lruItem);
        }

        // put new
        for (String address: addressSet) {  // 添加新的
            if (!lruItem.containsKey(address)) {
                lruItem.put(address, address);
            }
        }
        // remove old 将不存在记录
        List<String> delKeys = new ArrayList<>();
        for (String existKey: lruItem.keySet()) {
            if (!addressSet.contains(existKey)) {
                delKeys.add(existKey);
            }
        }

        //  移除不存在的
        if (delKeys.size() > 0) {
            for (String delKey: delKeys) {
                lruItem.remove(delKey);
            }
        }

        // load
        String eldestKey = lruItem.entrySet().iterator().next().getKey();
        String eldestValue = lruItem.get(eldestKey);
        return eldestValue;
    }

    @Override
    public String route(String serviceKey, TreeSet<String> addressSet) {
        String finalAddress = doRoute(serviceKey, addressSet);
        return finalAddress;
    }

    public static void main(String[] args) {
        XxlRpcLoadBalanceLRUStrategy  xxlRpcLoadBalanceLRUStrategy = new XxlRpcLoadBalanceLRUStrategy();
        TreeSet<String> set= new TreeSet<>();
        set.add("192.168.7.144:8081");
        set.add("192.168.7.144:8082");
        set.add("192.168.7.144:8083");
        set.add("192.168.7.144:8084");
        set.add("192.168.7.144:8085");
        set.add("192.168.7.144:8086");

        while (true) {
            System.out.println( xxlRpcLoadBalanceLRUStrategy.route("testKey", set));
        }


    }
}
