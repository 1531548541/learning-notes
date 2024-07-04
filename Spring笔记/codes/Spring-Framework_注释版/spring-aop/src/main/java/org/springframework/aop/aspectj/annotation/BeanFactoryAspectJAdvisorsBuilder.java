/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Advisor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Helper for retrieving @AspectJ beans from a BeanFactory and building
 * Spring Advisors based on them, for use with auto-proxying.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see AnnotationAwareAspectJAutoProxyCreator
 */
public class BeanFactoryAspectJAdvisorsBuilder {

	private final ListableBeanFactory beanFactory;

	private final AspectJAdvisorFactory advisorFactory;

	@Nullable
	private volatile List<String> aspectBeanNames;

	private final Map<String, List<Advisor>> advisorsCache = new ConcurrentHashMap<>();

	private final Map<String, MetadataAwareAspectInstanceFactory> aspectFactoryCache = new ConcurrentHashMap<>();


	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory) {
		this(beanFactory, new ReflectiveAspectJAdvisorFactory(beanFactory));
	}

	/**
	 * Create a new BeanFactoryAspectJAdvisorsBuilder for the given BeanFactory.
	 * @param beanFactory the ListableBeanFactory to scan
	 * @param advisorFactory the AspectJAdvisorFactory to build each Advisor with
	 */
	public BeanFactoryAspectJAdvisorsBuilder(ListableBeanFactory beanFactory, AspectJAdvisorFactory advisorFactory) {
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null");
		Assert.notNull(advisorFactory, "AspectJAdvisorFactory must not be null");
		this.beanFactory = beanFactory;
		this.advisorFactory = advisorFactory;
	}


	/**
	 * Look for AspectJ-annotated aspect beans in the current bean factory,
	 * and return to a list of Spring AOP Advisors representing them.
	 * <p>Creates a Spring Advisor for each AspectJ advice method.
	 * @return the list of {@link org.springframework.aop.Advisor} beans
	 * @see #isEligibleBean
	 */
	public List<Advisor> buildAspectJAdvisors() {
		List<String> aspectNames = this.aspectBeanNames;

		// 1.如果aspectNames为空，则进行解析
		if (aspectNames == null) {
			synchronized (this) {
				aspectNames = this.aspectBeanNames;
				if (aspectNames == null) {
					List<Advisor> advisors = new ArrayList<>();
					aspectNames = new ArrayList<>();
					// 1.1 获取所有的beanName
					String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
							this.beanFactory, Object.class, true, false);
					// 1.2 循环遍历所有的beanName，找出对应的增强方法
					for (String beanName : beanNames) {
						// 1.3 不合法的beanName则跳过，默认返回true，子类可以覆盖实现，AnnotationAwareAspectJAutoProxyCreator
						// 实现了自己的逻辑，支持使用includePatterns进行筛选
						if (!isEligibleBean(beanName)) {
							continue;
						}
						// We must be careful not to instantiate beans eagerly as in this case they
						// would be cached by the Spring container but would not have been weaved.
						// 获取beanName对应的bean的类型
						Class<?> beanType = this.beanFactory.getType(beanName, false);
						if (beanType == null) {
							continue;
						}
						// 1.4 如果beanType存在Aspect注解则进行处理
						if (this.advisorFactory.isAspect(beanType)) {
							// 将存在Aspect注解的beanName添加到aspectNames列表
							aspectNames.add(beanName);
							// 新建切面元数据
							AspectMetadata amd = new AspectMetadata(beanType, beanName);
							// 获取per-clause的类型是SINGLETON
							if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {
								// 使用BeanFactory和beanName创建一个BeanFactoryAspectInstanceFactory，主要用来创建切面对象实例
								MetadataAwareAspectInstanceFactory factory =
										new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);
								// 1.5 解析标记AspectJ注解中的增强方法===》》》》
								List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);
								// 1.6 放到缓存中
								if (this.beanFactory.isSingleton(beanName)) {
									// 如果beanName是单例则直接将解析的增强方法放到缓存
									this.advisorsCache.put(beanName, classAdvisors);
								}
								else {
									// 如果不是单例，则将factory放到缓存，之后可以通过factory来解析增强方法
									this.aspectFactoryCache.put(beanName, factory);
								}
								// 1.7 将解析的增强器添加到advisors
								advisors.addAll(classAdvisors);
							}
							else {
								// Per target or per this.
								// 如果per-clause的类型不是SINGLETON
								if (this.beanFactory.isSingleton(beanName)) {
									// 名称为beanName的Bean是单例，但切面实例化模型不是单例，则抛异常
									throw new IllegalArgumentException("Bean with name '" + beanName +
											"' is a singleton, but aspect instantiation model is not singleton");
								}
								MetadataAwareAspectInstanceFactory factory =
										new PrototypeAspectInstanceFactory(this.beanFactory, beanName);
								// 将factory放到缓存，之后可以通过factory来解析增强方法
								this.aspectFactoryCache.put(beanName, factory);
								// 解析标记AspectJ注解中的增强方法，并添加到advisors中
								advisors.addAll(this.advisorFactory.getAdvisors(factory));
							}
						}
					}
					// 1.9 将解析出来的切面beanName放到缓存aspectBeanNames
					this.aspectBeanNames = aspectNames;
					// 1.10 最后返回解析出来的增强器
					return advisors;
				}
			}
		}

		// 2.如果aspectNames不为null，则代表已经解析过了，则无需再次解析
		// 2.1 如果aspectNames是空列表，则返回一个空列表。空列表也是解析过的，只要不是null都是解析过的
		if (aspectNames.isEmpty()) {
			return Collections.emptyList();
		}
		// 2.2 aspectNames不是空列表，则遍历处理
		List<Advisor> advisors = new ArrayList<>();
		for (String aspectName : aspectNames) {
			// 根据aspectName从缓存中获取增强器
			List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);
			if (cachedAdvisors != null) {
				// 根据上面的解析，可以知道advisorsCache存的是已经解析好的增强器，直接添加到结果即可
				advisors.addAll(cachedAdvisors);
			}
			else {
				// 如果不存在于advisorsCache缓存，则代表存在于aspectFactoryCache中，
				// 从aspectFactoryCache中拿到缓存的factory，然后解析出增强器，添加到结果中
				MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);
				advisors.addAll(this.advisorFactory.getAdvisors(factory));
			}
		}
		// 返回增强器
		return advisors;
	}

	/**
	 * Return whether the aspect bean with the given name is eligible.
	 * @param beanName the name of the aspect bean
	 * @return whether the bean is eligible
	 */
	protected boolean isEligibleBean(String beanName) {
		return true;
	}

}
