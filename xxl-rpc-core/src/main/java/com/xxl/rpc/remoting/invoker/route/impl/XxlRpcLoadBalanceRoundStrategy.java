package com.xxl.rpc.remoting.invoker.route.impl;

import com.xxl.rpc.remoting.invoker.route.XxlRpcLoadBalance;

import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * round
 *
 * @author xuxueli 2018-12-04
 */
public class XxlRpcLoadBalanceRoundStrategy extends XxlRpcLoadBalance {

    private ConcurrentMap<String, Integer> routeCountEachJob = new ConcurrentHashMap<String, Integer>();
    private long CACHE_VALID_TIME = 0;
    private int count(String serviceKey) {
        // cache clear
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            routeCountEachJob.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 24*60*60*1000;
        }

        // count++
        Integer count = routeCountEachJob.get(serviceKey);
        count = (count==null || count>1000000)?(new Random().nextInt(100)):++count;  // 初始化时主动Random一次，缓解首次压力
        routeCountEachJob.put(serviceKey, count);
        return count;
    }

    @Override
    public String route(String serviceKey, TreeSet<String> addressSet) {
        // arr  转成 array
        String[] addressArr = addressSet.toArray(new String[addressSet.size()]);

        // round
        String finalAddress = addressArr[count(serviceKey)%addressArr.length];
        return finalAddress;
    }

    public static void main(String[] args) {
        XxlRpcLoadBalanceRoundStrategy   xxlRpcLoadBalanceRoundStrategy = new XxlRpcLoadBalanceRoundStrategy();
        TreeSet<String> set= new TreeSet<>();
        set.add("192.168.7.144:8081");
        set.add("192.168.7.144:8082");
        set.add("192.168.7.144:8083");
        set.add("192.168.7.144:8084");
        set.add("192.168.7.144:8085");
        set.add("192.168.7.144:8086");

        while (true) {
            System.out.println( xxlRpcLoadBalanceRoundStrategy.route("testKey", set));
        }


    }
}
