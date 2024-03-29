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
package io.github.mscheong01.interfaice

import io.github.mscheong01.interfaice.openai.OpenAiApiAdapter
import io.github.mscheong01.interfaice.openai.OpenAiApiClient
import io.github.mscheong01.interfaice.openai.OpenAiBeanFactoryPostProcessor
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@EnableConfigurationProperties(InterfaiceConfigurationProperties::class)
@AutoConfiguration
open class InterfaiceAutoConfiguration {

    @Bean
    open fun openAiBeanFactoryPostProcessor(
        openAiApiAdapter: OpenAiApiAdapter,
        customTranscodingRules: List<TranscodingRules.CustomRule<*>>
    ): OpenAiBeanFactoryPostProcessor {
        return OpenAiBeanFactoryPostProcessor(openAiApiAdapter, customTranscodingRules)
    }

    @ConditionalOnMissingBean(OpenAiApiAdapter::class)
    @Bean
    open fun openAiApiClient(
        properties: InterfaiceConfigurationProperties
    ): OpenAiApiAdapter {
        return OpenAiApiClient().apply {
            setProperties(properties.openai)
        }
    }
}
