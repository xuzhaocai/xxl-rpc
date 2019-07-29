package com.xxl.rpc.remoting.invoker.impl;

import com.xxl.rpc.registry.ServiceRegistry;
import com.xxl.rpc.remoting.invoker.XxlRpcInvokerFactory;
import com.xxl.rpc.remoting.invoker.annotation.XxlRpcReference;
import com.xxl.rpc.remoting.invoker.reference.XxlRpcReferenceBean;
import com.xxl.rpc.remoting.provider.XxlRpcProviderFactory;
import com.xxl.rpc.util.XxlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * xxl-rpc invoker factory, init service-registry and spring-bean by annotation (for spring)
 *
 * @author xuxueli 2018-10-19
 */
public class XxlRpcSpringInvokerFactory extends InstantiationAwareBeanPostProcessorAdapter implements InitializingBean,DisposableBean, BeanFactoryAware {
    private Logger logger = LoggerFactory.getLogger(XxlRpcSpringInvokerFactory.class);

    // ---------------------- config ----------------------

    private Class<? extends ServiceRegistry> serviceRegistryClass;          // class.forname
    private Map<String, String> serviceRegistryParam;


    public void setServiceRegistryClass(Class<? extends ServiceRegistry> serviceRegistryClass) {
        this.serviceRegistryClass = serviceRegistryClass;
    }

    public void setServiceRegistryParam(Map<String, String> serviceRegistryParam) {
        this.serviceRegistryParam = serviceRegistryParam;
    }


    // ---------------------- util ----------------------

    private XxlRpcInvokerFactory xxlRpcInvokerFactory;

    @Override
    public void afterPropertiesSet() throws Exception {
        // start invoker factory  创建注册中心对象， 配置注册中心
        xxlRpcInvokerFactory = new XxlRpcInvokerFactory(serviceRegistryClass, serviceRegistryParam);
        xxlRpcInvokerFactory.start();
    }

    @Override
    public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {

        // collection   保存需要提供服务的key
        final Set<String> serviceKeyList = new HashSet<>();

        // parse XxlRpcReferenceBean
        ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
            @Override
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                if (field.isAnnotationPresent(XxlRpcReference.class)) {  // 找出带XxlRpcReference  注解的 成员
                    // valid
                    Class iface = field.getType();
                    if (!iface.isInterface()) {  // 不是接口类型就异常
                        throw new XxlRpcException("xxl-rpc, reference(XxlRpcReference) must be interface.");
                    }
                    // 获取注解信息
                    XxlRpcReference rpcReference = field.getAnnotation(XxlRpcReference.class);

                    // init reference bean   创建 bean
                    XxlRpcReferenceBean referenceBean = new XxlRpcReferenceBean(
                            rpcReference.netType(),
                            rpcReference.serializer().getSerializer(),
                            rpcReference.callType(),
                            rpcReference.loadBalance(),
                            iface,
                            rpcReference.version(),
                            rpcReference.timeout(),
                            rpcReference.address(),
                            rpcReference.accessToken(),
                            null,
                            xxlRpcInvokerFactory
                    );
                    // 获取代理
                    Object serviceProxy = referenceBean.getObject();

                    // set bean
                    field.setAccessible(true);

                    // 将代理值设置进去
                    field.set(bean, serviceProxy);

                    logger.info(">>>>>>>>>>> xxl-rpc, invoker factory init reference bean success. serviceKey = {}, bean.field = {}.{}",
                            XxlRpcProviderFactory.makeServiceKey(iface.getName(), rpcReference.version()), beanName, field.getName());

                    // collection  生成key
                    String serviceKey = XxlRpcProviderFactory.makeServiceKey(iface.getName(), rpcReference.version());

                    serviceKeyList.add(serviceKey);

                }
            }
        });

        // mult discovery 进行服务发现
        if (xxlRpcInvokerFactory.getServiceRegistry() != null) {
            try {
                // 进行服务发现
                xxlRpcInvokerFactory.getServiceRegistry().discovery(serviceKeyList);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        return super.postProcessAfterInstantiation(bean, beanName);
    }


    @Override
    public void destroy() throws Exception {

        // stop invoker factory
        xxlRpcInvokerFactory.stop();
    }

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
