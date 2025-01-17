/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal

import spock.lang.IgnoreIf
import spock.lang.Specification
import spock.lang.Subject

class FeaturePreviewsTest extends Specification {

    @Subject
    def previews = new FeaturePreviews()

    def 'has no features enabled by default'() {
        expect:
        !previews.isFeatureEnabled(feature)
        where:
        feature << FeaturePreviews.Feature.values()
    }

    def "can enable #feature feature"() {
        when:
        previews.enableFeature(feature)
        then:
        previews.isFeatureEnabled(feature)
        where:
        feature << FeaturePreviewsActivationFixture.activeFeatures()
    }

    @IgnoreIf({ FeaturePreviewsActivationFixture.inactiveFeatures().isEmpty() })
    def "ignores activation of inactive #feature feature"() {
        when:
        previews.enableFeature(feature)
        then:
        !previews.isFeatureEnabled(feature)
        where:
        feature << FeaturePreviewsActivationFixture.inactiveFeatures()
    }

    def 'lists active features'() {
        expect:
        previews.activeFeatures == FeaturePreviewsActivationFixture.activeFeatures()
    }
}
