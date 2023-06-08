// Copyright 2023 Minsoo Cheong
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package io.github.mscheong01.interfaice.openai

import io.github.mscheong01.interfaice.EnableInterfaiceProxies
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter

class OpenAiBeanFactoryPostProcessor(
    openAiApiAdapter: OpenAiApiAdapter
) : BeanFactoryPostProcessor {

    private val proxyFactory = OpenAiProxyFactory(openAiApiAdapter)

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        if (beanFactory !is BeanDefinitionRegistry) {
            return
        }

        val interfaceProvider = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
                return super.isCandidateComponent(beanDefinition) || beanDefinition.metadata.isAbstract
            }
        }.apply {
            addIncludeFilter(AnnotationTypeFilter(OpenAiInterface::class.java, true, true))
        }

        val candidates = getBasePackages(beanFactory).map { basePackage ->
            interfaceProvider.findCandidateComponents(basePackage)
        }.distinct().flatten()

        for (candidate in candidates) {
            try {
                val beanClass = Class.forName(candidate.beanClassName)
                beanFactory.registerBeanDefinition(
                    beanClass.simpleName,
                    candidate.apply {
                        (this as AbstractBeanDefinition).setInstanceSupplier {
                            proxyFactory.create(beanClass)
                        }
                    }
                )
            } catch (e: ClassNotFoundException) {
                throw IllegalStateException("Unable to find class for bean: " + candidate.getBeanClassName())
            }
        }
    }

    fun getBasePackages(beanFactory: ConfigurableListableBeanFactory): Set<String> {
        val basePackages = mutableSetOf<String>()
        beanFactory.beanDefinitionNames.forEach {
            val definition = beanFactory.getBeanDefinition(it)
            if (definition is AnnotatedBeanDefinition) {
                val metadata = definition.metadata
                if (metadata.hasAnnotation(EnableInterfaiceProxies::class.java.name)) {
                    val annotationAttributes = metadata.getAnnotationAttributes(EnableInterfaiceProxies::class.java.name)
                        ?: return@forEach
                    val annotatedBasePackages = annotationAttributes.get("basePackages") as Array<String>
                    basePackages.addAll(annotatedBasePackages)
                }
            }
        }
        return basePackages
    }
}
