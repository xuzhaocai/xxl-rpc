package com.xxl.rpc.remoting.invoker.call;

/**
 * rpc call type
 *
 * @author xuxueli 2018-10-19
 */
public enum CallType {


    SYNC,// 同步

    FUTURE,  // 异步

    CALLBACK, // 回调

    ONEWAY;// 单向调用


    public static CallType match(String name, CallType defaultCallType){
        for (CallType item : CallType.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultCallType;
    }

}
