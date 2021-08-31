/*
 * Copyright 2013, 2019 Deutsche Nationalbibliothek and others
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

//TODO: move all classes here to fix package

package org.metafacture.metafix;

import org.metafacture.framework.StreamPipe;
import org.metafacture.framework.StreamReceiver;
import org.metafacture.framework.helpers.DefaultStreamReceiver;
import org.metafacture.mangling.StreamFlattener;
import org.metafacture.metafix.fix.Expression;
import org.metafacture.metafix.fix.Fix;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Transforms a data stream sent via the {@link StreamReceiver} interface. Use
 * {@link RecordTransformer} to transform based on a Fix DSL description.
 *
 * @author Markus Michael Geipel (Metamorph)
 * @author Christoph Böhme (Metamorph)
 * @author Fabian Steeg (Metafix)
 */
public class Metafix implements StreamPipe<StreamReceiver> {

    public static final String VAR_START = "$[";
    public static final String VAR_END = "]";
    public static final Map<String, String> NO_VARS = Collections.emptyMap();
    private static final String ENTITIES_NOT_BALANCED = "Entity starts and ends are not balanced";

    // TODO: Use SimpleRegexTrie / WildcardTrie for wildcard, alternation and character class support
    private Map<String, Object> currentRecord = new LinkedHashMap<>();
    private Fix fix;
    private final List<Expression> expressions = new ArrayList<>();
    private Map<String, String> vars = NO_VARS;
    private final StreamFlattener flattener = new StreamFlattener();
    private final Deque<Integer> entityCountStack = new LinkedList<>();
    private int entityCount;
    private StreamReceiver outputStreamReceiver;
    private String recordIdentifier;

    public Metafix() {
        init();
    }

    public Metafix(final String fixDef) throws FileNotFoundException {
        this(fixDef, NO_VARS);
    }

    public Metafix(final String fixDef, final Map<String, String> vars) throws FileNotFoundException {
        this(fixDef.endsWith(".fix") ? new FileReader(fixDef) : new StringReader(fixDef), vars);
    }

    public Metafix(final Reader morphDef) {
        this(morphDef, NO_VARS);
    }

    public Metafix(final Reader fixDef, final Map<String, String> vars) {
        buildPipeline(fixDef, vars);
        init();
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    private void init() {
        flattener.setReceiver(new DefaultStreamReceiver() {
            @Override
            public void literal(final String name, final String value) {
                // TODO: set up logging
                System.out.printf("Putting '%s':'%s'\n", name, value);
                add(currentRecord, name, value);
            }
        });
    }

    private void buildPipeline(final Reader fixDef, final Map<String, String> theVars) {
        final Fix f = FixStandaloneSetup.parseFix(fixDef);
        this.fix = f;
        this.vars = theVars;
    }

    @Override
    public void startRecord(final String identifier) {
        currentRecord = new LinkedHashMap<>();
        System.out.printf("Start record: %s\n", currentRecord);
        flattener.startRecord(identifier);
        entityCountStack.clear();
        entityCount = 0;
        entityCountStack.add(Integer.valueOf(entityCount));
        recordIdentifier = identifier;
    }

    @Override
    public void endRecord() {
        entityCountStack.removeLast();
        if (!entityCountStack.isEmpty()) {
            throw new IllegalStateException(ENTITIES_NOT_BALANCED);
        }
        flattener.endRecord();
        System.out.printf("End record, walking fix: %s\n", currentRecord);
        final RecordTransformer transformer = new RecordTransformer(currentRecord, vars, fix);
        currentRecord = transformer.transform();
        if (!currentRecord.containsKey("__reject")) {
            outputStreamReceiver.startRecord(recordIdentifier);
            System.out.println("Sending results to " + outputStreamReceiver);
            currentRecord.keySet().forEach(k -> {
                emit(k, currentRecord.get(k));
            });
            outputStreamReceiver.endRecord();
        }
    }

    private void emit(final Object key, final Object val) {
        if (val == null) {
            return;
        }
        final List<?> vals = val instanceof List ? (List<?>) val : Arrays.asList(val);
        final boolean isMulti = vals.size() > 1;
        if (isMulti) {
            outputStreamReceiver.startEntity(key.toString());
        }
        vals.forEach(value -> {
            if (value instanceof Map) {
                final Map<?, ?> nested = (Map<?, ?>) value;
                outputStreamReceiver.startEntity(isMulti ? "" : key.toString());
                nested.entrySet().forEach(nestedEntry -> {
                    emit(nestedEntry.getKey(), Arrays.asList(nestedEntry.getValue()));
                });
                outputStreamReceiver.endEntity();
            }
            else {
                outputStreamReceiver.literal(isMulti ? "" : key.toString(), value.toString());
            }
        });
        if (isMulti) {
            outputStreamReceiver.endEntity();
        }
    }

    @Override
    public void startEntity(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("Entity name must not be null.");
        }
        ++entityCount;
        entityCountStack.push(Integer.valueOf(entityCount));
        flattener.startEntity(name);
    }

    @Override
    public void endEntity() {
        entityCountStack.pop().intValue();
        flattener.endEntity();
    }

    @Override
    public void literal(final String name, final String value) {
        flattener.literal(name, value);
    }

    @Override
    public void resetStream() {
        outputStreamReceiver.resetStream();
    }

    @Override
    public void closeStream() {
        outputStreamReceiver.closeStream();
    }

    /**
     * @param streamReceiver the outputHandler to set
     */
    @Override
    public <R extends StreamReceiver> R setReceiver(final R streamReceiver) {
        if (streamReceiver == null) {
            throw new IllegalArgumentException("'streamReceiver' must not be null");
        }
        outputStreamReceiver = streamReceiver;
        return streamReceiver;
    }

    public StreamReceiver getStreamReceiver() {
        return outputStreamReceiver;
    }

    public Map<String, String> getVars() {
        return vars;
    }

    public Map<String, Object> getCurrentRecord() {
        return currentRecord;
    }

    static void addAll(final Map<String, Object> record, final String fieldName, final List<String> values) {
        values.forEach(value -> {
            add(record, fieldName, value);
        });
    }

    static void addAll(final Map<String, Object> record, final Map<String, Object> values) {
        values.entrySet().forEach(value -> {
            add(record, value.getKey(), value.getValue());
        });
    }

    static void add(final Map<String, Object> record, final String name, final Object val) {
        final Object object = record.get(name);
        record.put(name, object == null ? val : asListWith(object, val));
    }

    static List<Object> asListWith(final Object object, final Object value) {
        final List<Object> list = asList(object);
        list.add(value);
        return list;
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(final Object object) {
        return new ArrayList<>(
                object instanceof List ? (List<Object>) object : Arrays.asList(object));
    }

}