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

package org.metafacture.metamorph;

import org.metafacture.framework.StreamReceiver;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import com.google.common.collect.ImmutableMap;

import java.util.Arrays;

/**
 * Tests Metafix field level methods. Following the cheat sheet examples at
 * https://github.com/LibreCat/Catmandu/wiki/Fixes-Cheat-Sheet
 *
 * @author Fabian Steeg
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MultipleStringLiterals")
public class MetafixMethodTest {

    @RegisterExtension
    private MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StreamReceiver streamReceiver;

    public MetafixMethodTest() {
    }

    @Test
    public void upcase() {
        MetafixTestHelpers.assertFix(streamReceiver, Arrays.asList(//
                "upcase('title')"), //
            i -> {
                i.startRecord("1");
                i.endRecord();
                //
                i.startRecord("2");
                i.literal("title", "marc");
                i.literal("title", "json");
                i.endRecord();
                //
                i.startRecord("3");
                i.endRecord();
            }, o -> {
                o.get().startRecord("1");
                o.get().endRecord();
                //
                o.get().startRecord("2");
                o.get().literal("title", "MARC");
                o.get().literal("title", "JSON");
                o.get().endRecord();
                //
                o.get().startRecord("3");
                o.get().endRecord();
            });
    }

    @Test
    public void downcase() {
        MetafixTestHelpers.assertFix(streamReceiver, Arrays.asList(//
                "downcase('title')"), //
            i -> {
                i.startRecord("1");
                i.endRecord();
                //
                i.startRecord("2");
                i.literal("title", "MARC");
                i.literal("title", "Json");
                i.endRecord();
                //
                i.startRecord("3");
                i.endRecord();
            }, o -> {
                o.get().startRecord("1");
                o.get().endRecord();
                //
                o.get().startRecord("2");
                o.get().literal("title", "marc");
                o.get().literal("title", "json");
                o.get().endRecord();
                //
                o.get().startRecord("3");
                o.get().endRecord();
            });
    }

    @Test
    public void capitalize() {
        MetafixTestHelpers.assertFix(streamReceiver, Arrays.asList(//
                "capitalize('title')"), //
            i -> {
                i.startRecord("1");
                i.endRecord();
                //
                i.startRecord("2");
                i.literal("title", "marc");
                i.literal("title", "json");
                i.endRecord();
                //
                i.startRecord("3");
                i.endRecord();
            }, o -> {
                o.get().startRecord("1");
                o.get().endRecord();
                //
                o.get().startRecord("2");
                o.get().literal("title", "Marc");
                o.get().literal("title", "Json");
                o.get().endRecord();
                //
                o.get().startRecord("3");
                o.get().endRecord();
            });
    }

    @Test
    public void substring() {
        MetafixTestHelpers.assertFix(streamReceiver, Arrays.asList(//
                "substring('title', '0', '2')"), //
            i -> {
                i.startRecord("1");
                i.endRecord();
                //
                i.startRecord("2");
                i.literal("title", "marc");
                i.literal("title", "json");
                i.endRecord();
                //
                i.startRecord("3");
                i.endRecord();
            }, o -> {
                o.get().startRecord("1");
                o.get().endRecord();
                //
                o.get().startRecord("2");
                o.get().literal("title", "m");
                o.get().literal("title", "j");
                o.get().endRecord();
                //
                o.get().startRecord("3");
                o.get().endRecord();
            });
    }

    @Test
    public void substringWithVar() {
        MetafixTestHelpers.assertFix(streamReceiver, Arrays.asList(//
                "substring('title', '0', '$[end]')"), //
                ImmutableMap.of("end", "3"),
            i -> {
                i.startRecord("1");
                i.endRecord();
                //
                i.startRecord("2");
                i.literal("title", "marc");
                i.literal("title", "json");
                i.endRecord();
                //
                i.startRecord("3");
                i.endRecord();
            }, o -> {
                o.get().startRecord("1");
                o.get().endRecord();
                //
                o.get().startRecord("2");
                o.get().literal("title", "ma");
                o.get().literal("title", "js");
                o.get().endRecord();
                //
                o.get().startRecord("3");
                o.get().endRecord();
            });
    }

    @Test
    public void trim() {
        MetafixTestHelpers.assertFix(streamReceiver, Arrays.asList(//
                "trim('title')"), //
            i -> {
                i.startRecord("1");
                i.endRecord();
                //
                i.startRecord("2");
                i.literal("title", "  marc  ");
                i.literal("title", "  json  ");
                i.endRecord();
                //
                i.startRecord("3");
                i.endRecord();
            }, o -> {
                o.get().startRecord("1");
                o.get().endRecord();
                //
                o.get().startRecord("2");
                o.get().literal("title", "marc");
                o.get().literal("title", "json");
                o.get().endRecord();
                //
                o.get().startRecord("3");
                o.get().endRecord();
            });
    }

    @Test
    @Disabled // Use SimpleRegexTrie/WildcardTrie
    public void alternation() {
        MetafixTestHelpers.assertFix(streamReceiver, Arrays.asList(//
                "trim('title-1|title-2')"), //
            i -> {
                i.startRecord("1");
                i.endRecord();
                //
                i.startRecord("2");
                i.literal("title-1", "  marc  ");
                i.literal("title-2", "  json  ");
                i.endRecord();
                //
                i.startRecord("3");
                i.endRecord();
            }, o -> {
                o.get().startRecord("1");
                o.get().endRecord();
                //
                o.get().startRecord("2");
                o.get().literal("title-2", "marc");
                o.get().literal("title-1", "json");
                o.get().endRecord();
                //
                o.get().startRecord("3");
                o.get().endRecord();
            });
    }

    @Test
    @Disabled // Use SimpleRegexTrie/WildcardTrie
    public void wildcard() {
        MetafixTestHelpers.assertFix(streamReceiver, Arrays.asList(//
                "trim('title-?')"), //
            i -> {
                i.startRecord("1");
                i.endRecord();
                //
                i.startRecord("2");
                i.literal("title-1", "  marc  ");
                i.literal("title-2", "  json  ");
                i.endRecord();
                //
                i.startRecord("3");
                i.endRecord();
            }, o -> {
                o.get().startRecord("1");
                o.get().endRecord();
                //
                o.get().startRecord("2");
                o.get().literal("title-2", "marc");
                o.get().literal("title-1", "json");
                o.get().endRecord();
                //
                o.get().startRecord("3");
                o.get().endRecord();
            });
    }

    @Test
    @Disabled // Use SimpleRegexTrie
    public void characterClass() {
        MetafixTestHelpers.assertFix(streamReceiver, Arrays.asList(//
                "trim('title-[12]')"), //
            i -> {
                i.startRecord("1");
                i.endRecord();
                //
                i.startRecord("2");
                i.literal("title-1", "  marc  ");
                i.literal("title-2", "  json  ");
                i.endRecord();
                //
                i.startRecord("3");
                i.endRecord();
            }, o -> {
                o.get().startRecord("1");
                o.get().endRecord();
                //
                o.get().startRecord("2");
                o.get().literal("title-2", "marc");
                o.get().literal("title-1", "json");
                o.get().endRecord();
                //
                o.get().startRecord("3");
                o.get().endRecord();
            });
    }
}