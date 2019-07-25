package com.xxl.rpc.remoting.invoker.route.impl;

import com.xxl.rpc.remoting.invoker.route.XxlRpcLoadBalance;

import java.util.Random;
import java.util.TreeSet;

/**
 * random
 *
 * @author xuxueli 2018-12-04
 */
public class XxlRpcLoadBalanceRandomStrategy extends XxlRpcLoadBalance {

    private Random random = new Random();

    @Override
    public String route(String serviceKey, TreeSet<String> addressSet) {
        // 转成 array
        String[] addressArr = addressSet.toArray(new String[addressSet.size()]);

        // random  取随机位置
        String finalAddress = addressArr[random.nextInt(addressSet.size())];
        return finalAddress;
    }

}
