/*
 * Copyright 2022 hbz NRW
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

import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;

import java.util.concurrent.TimeUnit;

@Measurement(time = 300) // checkstyle-disable-line MagicNumber
@OutputTimeUnit(TimeUnit.MINUTES)
public class SlowMetafixBenchmark extends AbstractMetafixBenchmark {

    @Param({ // checkstyle-disable-line AnnotationUseStyle
        "alma-large"
    })
    private String input;

    public SlowMetafixBenchmark() {
    }

    @Override
    protected String getInput() {
        return input;
    }

}
