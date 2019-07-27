package com.xxl.rpc.test;

import com.xxl.rpc.serialize.impl.KryoSerializer;

public class KryoSerializerTest {

    public static void main(String[] args) {


        KryoSerializer serializer = new KryoSerializer();

        User user  =new User();
        user.setName("name");

        byte[] bytes = serializer.serialize(user);

        Object deserialize = serializer.deserialize(bytes, User.class);
        System.out.println(deserialize);
    }

    public static  class User{

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
