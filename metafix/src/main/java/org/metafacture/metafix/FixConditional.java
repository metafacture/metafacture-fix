/*
 * Copyright 2021 Fabian Steeg, hbz
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.metafacture.metafix;

import org.metafacture.metafix.api.FixPredicate;

import java.util.List;
import java.util.Map;

public enum FixConditional implements FixPredicate {

    all_contain {
        @Override
        public boolean test(final Metafix metafix, final Record record, final List<String> params, final Map<String, String> options) {
            return testConditional(record, params, ALL, CONTAINS);
        }
    },
    any_contain {
        @Override
        public boolean test(final Metafix metafix, final Record record, final List<String> params, final Map<String, String> options) {
            return testConditional(record, params, ANY, CONTAINS);
        }
    },
    none_contain {
        @Override
        public boolean test(final Metafix metafix, final Record record, final List<String> params, final Map<String, String> options) {
            return !any_contain.test(metafix, record, params, options);
        }
    },

    all_equal {
        @Override
        public boolean test(final Metafix metafix, final Record record, final List<String> params, final Map<String, String> options) {
            return testConditional(record, params, ALL, EQUALS);
        }
    },
    any_equal {
        @Override
        public boolean test(final Metafix metafix, final Record record, final List<String> params, final Map<String, String> options) {
            return testConditional(record, params, ANY, EQUALS);
        }
    },
    none_equal {
        @Override
        public boolean test(final Metafix metafix, final Record record, final List<String> params, final Map<String, String> options) {
            return !any_equal.test(metafix, record, params, options);
        }
    },

    exists {
        @Override
        public boolean test(final Metafix metafix, final Record record, final List<String> params, final Map<String, String> options) {
            return record.containsField(params.get(0));
        }
    },

    all_match {
        @Override
        public boolean test(final Metafix metafix, final Record record, final List<String> params, final Map<String, String> options) {
            return testConditional(record, params, ALL, MATCHES);
        }
    },
    any_match {
        @Override
        public boolean test(final Metafix metafix, final Record record, final List<String> params, final Map<String, String> options) {
            return testConditional(record, params, ANY, MATCHES);
        }
    },
    none_match {
        @Override
        public boolean test(final Metafix metafix, final Record record, final List<String> params, final Map<String, String> options) {
            return !any_match.test(metafix, record, params, options);
        }
    }

}