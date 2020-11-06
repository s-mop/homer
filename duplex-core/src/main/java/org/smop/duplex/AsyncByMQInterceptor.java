package org.smop.duplex;

import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.annotation.RabbitListenerAnnotationBeanPostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Method;

/**
 * 提供一种通过MQ[异步]调用方法的方式
 * <p>
 * 要求：
 * <p>
 * 1 被调用方法有@RabbitListener注解，注解中有queuesToDeclare参数，参数中有具体value值（key）
 * 例：@RabbitListener(queuesToDeclare = @Queue("XXX"))
 * <p>
 * 2 参数为至多一个对象（Object）,有需要可考虑参数装箱
 * <p>
 * 3 调用者线程名称不包含'amqp'字符串（区分MQ Container线程调用目标方法）
 * <p>
 * 4 目前无法获取非void类型方法返回值(targetMethod.getReturnType().equals(Void.TYPE))
 * 如有需要可考虑AsyncRabbitTemplate配合callBack
 * 参考https://stackoverflow.com/questions/49567994/rabbitmq-asynchronous-call-have-to-wait-until-subscriber-return-a
 * -response
 */
@Aspect
@Log4j2
@Configuration
public class AsyncByMQInterceptor implements ApplicationContextAware {

	@Resource
	private RabbitTemplate rabbitTemplate;
	private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();
	private BeanExpressionContext evalContext;
	private ConfigurableListableBeanFactory beanFactory;

	@Around("@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
	public Object cut(ProceedingJoinPoint pjp) throws Throwable {
		Object[] args = pjp.getArgs();
		Queue[] queues = getQueues(pjp);
		if (isNotMQAsyncCall(args, queues)) {
			return pjp.proceed();
		}
		rabbitTemplate.convertAndSend(resolveKey(queues), args[0]);
		return null;
	}

	/**
	 * 处理带有占位符的key
	 * @see RabbitListenerAnnotationBeanPostProcessor
	 */
	private String resolveKey(Queue[] queues) {
		String s = this.beanFactory.resolveEmbeddedValue(queues[0].value());
		return (String) resolver.evaluate(s, evalContext);
	}

	private Queue[] getQueues(ProceedingJoinPoint pjp) {
		Method targetMethod = ((MethodSignature) (pjp.getSignature())).getMethod();
		RabbitListener annotation = targetMethod.getAnnotation(RabbitListener.class);
		return annotation.queuesToDeclare();
	}

	private boolean isNotMQAsyncCall(Object[] args, Queue[] queues) {
		return Thread.currentThread().getName().contains("amqp") || args.length != 1 || !isValidQueueInfo(queues);
	}

	private boolean isValidQueueInfo(Queue[] queues) {
		return queues.length > 0 && !StringUtils.isEmpty(queues[0].value());
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		ConfigurableApplicationContext configurableApplicationContext =
				(ConfigurableApplicationContext) applicationContext;
		beanFactory = configurableApplicationContext.getBeanFactory();
		this.evalContext = new BeanExpressionContext(beanFactory, null);
	}
}
