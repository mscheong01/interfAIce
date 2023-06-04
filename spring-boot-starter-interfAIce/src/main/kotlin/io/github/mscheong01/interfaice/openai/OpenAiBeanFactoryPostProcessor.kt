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

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter

class OpenAiBeanFactoryPostProcessor(
    openAiApiAdapter: OpenAiApiAdapter,
) : BeanFactoryPostProcessor {

    private val proxyFactory = OpenAiProxyFactory(openAiApiAdapter)

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        if (beanFactory !is BeanDefinitionRegistry) {
            return
        }
        val provider = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
                return super.isCandidateComponent(beanDefinition) || beanDefinition.metadata.isAbstract
            }
        }.apply {
            addIncludeFilter(AnnotationTypeFilter(OpenAiInterface::class.java, true, true));
        }

        val candidates = provider.findCandidateComponents("io.github.mscheong01.interfaice") // TODO: Make this configurable
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
}
