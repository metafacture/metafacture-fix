/*
 * Copyright 2021 hbz NRW
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

import org.metafacture.commons.tries.SimpleRegexTrie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a record value, i.e., either an {@link Array}, a {@link Hash},
 * or a {@link java.lang.String String}.
 */
public class Value {

    /*package-private*/ enum ReservedField {
        $append, $first, $last;

        private static final Map<String, ReservedField> STRING_TO_ENUM = new HashMap<>();
        static {
            for (final ReservedField f : values()) {
                STRING_TO_ENUM.put(f.toString(), f);
            }
        }

        static ReservedField fromString(final String string) {
            return STRING_TO_ENUM.get(string);
        }
    }

    private static final String ASTERISK = "*";

    private final Array array;
    private final Hash hash;
    private final String string;

    private final Type type;

    public Value(final Array array) {
        type = array != null ? Type.Array : null;

        this.array = array;
        this.hash = null;
        this.string = null;
    }

    public Value(final List<Value> array) {
        this(array != null ? new Array() : null);

        if (array != null) {
            array.forEach(this.array::add);
        }
    }

    public Value(final Hash hash) {
        type = hash != null ? Type.Hash : null;

        this.array = null;
        this.hash = hash;
        this.string = null;
    }

    public Value(final Map<String, Value> hash) {
        this(hash != null ? new Hash() : null);

        if (hash != null) {
            hash.forEach(this.hash::put);
        }
    }

    public Value(final String string) {
        type = string != null ? Type.String : null;

        this.array = null;
        this.hash = null;
        this.string = string;
    }

    public Value(final int integer) {
        this(String.valueOf(integer));
    }

    public static Value newArray() {
        return newArray(null);
    }

    public static Value newArray(final Consumer<Array> consumer) {
        final Array array = new Array();

        if (consumer != null) {
            consumer.accept(array);
        }

        return new Value(array);
    }

    public static Value newHash() {
        return newHash(null);
    }

    public static Value newHash(final Consumer<Hash> consumer) {
        final Hash hash = new Hash();

        if (consumer != null) {
            consumer.accept(hash);
        }

        return new Value(hash);
    }

    public boolean isArray() {
        return isType(Type.Array);
    }

    public boolean isHash() {
        return isType(Type.Hash);
    }

    public boolean isString() {
        return isType(Type.String);
    }

    private boolean isType(final Type targetType) {
        return type == targetType;
    }

    public boolean isNull() {
        return extractType(Boolean.TRUE, c -> {
            if (type != null) {
                matchType()
                    .ifArray(a -> c.accept(a == null))
                    .ifHash(h -> c.accept(h == null))
                    .ifString(s -> c.accept(s == null))
                    .orElseThrow();
            }
        });
    }

    public static boolean isNull(final Value value) {
        return value == null || value.isNull();
    }

    private static boolean isNumber(final String s) {
        return s.matches("\\d+");
    }

    public Array asArray() {
        return extractType(null, c -> matchType().ifArray(c).orElseThrow());
    }

    public Hash asHash() {
        return extractType(null, c -> matchType().ifHash(c).orElseThrow());
    }

    public String asString() {
        return extractType(null, c -> matchType().ifString(c).orElseThrow());
    }

    public static Value asList(final Value value, final Consumer<Array> consumer) {
        return isNull(value) ? null : value.asList(consumer);
    }

    public Value asList(final Consumer<Array> consumer) {
        if (isArray()) {
            if (consumer != null) {
                consumer.accept(asArray());
            }

            return this;
        }
        else {
            return newArray(a -> {
                a.add(this);

                if (consumer != null) {
                    consumer.accept(a);
                }
            });
        }
    }

    public TypeMatcher matchType() {
        return new TypeMatcher(this);
    }

    private static <T> T extractType(final T defaultValue, final Consumer<Consumer<T>> consumer) {
        final AtomicReference<T> result = new AtomicReference<>(defaultValue);
        consumer.accept(result::set);
        return result.get();
    }

    @Override
    public String toString() {
        return extractType(null, c -> {
            if (!isNull()) {
                matchType()
                    .ifArray(a -> c.accept(a.toString()))
                    .ifHash(h -> c.accept(h.toString()))
                    .ifString(c)
                    .orElseThrow();
            }
        });
    }

    private static String[] tail(final String[] fields) {
        return Arrays.copyOfRange(fields, 1, fields.length);
    }

    private void transformFields(final String[] fields, final UnaryOperator<String> operator) {
        matchType()
            .ifArray(a -> a.transformFields(fields, operator))
            .ifHash(h -> h.transformFields(fields, operator))
            .orElseThrow();
    }

    enum Type {
        Array,
        Hash,
        String
    }

    /*private-private*/ static class TypeMatcher {

        private final Set<Type> expected = new HashSet<>();
        private final Value value;

        private TypeMatcher(final Value value) {
            this.value = value;
        }

        public TypeMatcher ifArray(final Consumer<Array> consumer) {
            return match(Type.Array, consumer, value.array);
        }

        public TypeMatcher ifHash(final Consumer<Hash> consumer) {
            return match(Type.Hash, consumer, value.hash);
        }

        public TypeMatcher ifString(final Consumer<String> consumer) {
            return match(Type.String, consumer, value.string);
        }

        public void orElse(final Consumer<Value> consumer) {
            if (!expected.contains(value.type)) {
                consumer.accept(value);
            }
        }

        public void orElseThrow() {
            orElse(v -> {
                final String types = expected.stream().map(Type::name).collect(Collectors.joining(" or "));
                throw new IllegalStateException("expected " + types + ", got " + value.type);
            });
        }

        private <T> TypeMatcher match(final Type type, final Consumer<T> consumer, final T rawValue) {
            if (expected.add(type)) {
                if (value.isType(type)) {
                    consumer.accept(rawValue);
                }

                return this;
            }
            else {
                throw new IllegalStateException("already expecting " + type);
            }
        }

    }

    private abstract static class AbstractValueType {

        @Override
        public abstract String toString();

        protected enum InsertMode {

            REPLACE {
                @Override
                void apply(final Hash hash, final String field, final Value value) {
                    hash.put(field, value);
                }
            },
            APPEND {
                @Override
                void apply(final Hash hash, final String field, final Value value) {
                    hash.add(field, value);
                }
            },
            /* For an indexed representation of arrays as hashes with 1, 2, 3 etc. keys.
             * i.e. ["a", "b", "c"] as { "1":"a", "2":"b", "3": "c" }
             * This is what is produced by JsonDecoder and Metafix itself for arrays.
             * TODO? maybe this would be a good general internal representation, resulting
             * in every value being either a hash or a string, no more separate array type.*/
            INDEXED {
                @Override
                void apply(final Hash hash, final String field, final Value value) {
                    hash.add(nextIndex(hash), field.equals(ReservedField.$append.name()) ? value : newHash(h -> h.put(field, value)));
                }

                private String nextIndex(final Hash hash) {
                    return "" + (hash.size() + 1) /* TODO? check if keys are actually all ints? */;
                }
            };

            abstract void apply(Hash hash, String field, Value value);

        }

    }

    /**
     * Represents an array of metadata values.
     */
    public static class Array extends AbstractValueType {

        private final List<Value> list = new ArrayList<>();

        /**
         * Creates an empty instance of {@link Array}.
         */
        private Array() {
        }

        public void add(final Value value) {
            if (!isNull(value)) {
                list.add(value);
            }
        }

        public int size() {
            return list.size();
        }

        public Value get(final int index) {
            return list.get(index);
        }

        public Stream<Value> stream() {
            return list.stream();
        }

        public void forEach(final Consumer<Value> consumer) {
            list.forEach(consumer);
        }

        @Override
        public String toString() {
            return list.toString();
        }

        public void remove(final int index) {
            list.remove(index);
        }

        private void removeNested(final String[] fields) {
            if (fields.length >= 1 && fields[0].equals(ASTERISK)) {
                list.clear();
            }
            else if (fields.length >= 1 && isNumber(fields[0])) {
                final int index = Integer.parseInt(fields[0]) - 1; // TODO: 0-based Catmandu vs. 1-based Metafacture
                if (index >= 0 && index < size()) {
                    remove(index);
                }
            }
        }

        private Value find(final String[] path) {
            final Value result;
            if (path.length > 0) {
                if (path[0].equals(ASTERISK)) {
                    result = newArray(a -> forEach(v -> a.add(findInValue(tail(path), v))));
                }
                else if (isNumber(path[0])) {
                    final int index = Integer.parseInt(path[0]) - 1; // TODO: 0-based Catmandu vs. 1-based Metafacture
                    if (index >= 0 && index < size()) {
                        result = findInValue(tail(path), get(index));
                    }
                    else {
                        result = null;
                    }
                }
                // TODO: WDCD? copy_field('your.name','author[].name'), where name is an array
                else {
                    result = newArray(a -> forEach(v -> a.add(findInValue(path, v))));
                }
            }
            else {
                result = new Value(this);
            }
            return result;
        }

        private Value findInValue(final String[] path, final Value value) {
            return extractType(null, c -> {
                // TODO: move impl into enum elements, here call only value.find
                if (value != null) {
                    value.matchType()
                        .ifArray(a -> c.accept(a.find(path)))
                        .ifHash(h -> c.accept(h.find(path)))
                        .orElse(c);
                }
            });
        }

        private void transformFields(final String[] fields, final UnaryOperator<String> operator) {
            final String field = fields[0];
            final String[] remainingFields = tail(fields);
            final int size = size();

            if (fields.length == 0 || field.equals(ASTERISK)) {
                for (int i = 0; i < size; ++i) {
                    transformFields(i, remainingFields, operator);
                }
            }
            else if (isNumber(field)) {
                final int index = Integer.parseInt(field) - 1; // TODO: 0-based Catmandu vs. 1-based Metafacture
                if (index >= 0 && index < size) {
                    transformFields(index, remainingFields, operator);
                }
            }
            // TODO: WDCD? copy_field('your.name','author[].name'), where name is an array
            else {
                for (int i = 0; i < size; ++i) {
                    transformFields(i, fields, operator);
                }
            }

            list.removeIf(v -> v == null);
        }

        private void transformFields(final int index, final String[] fields, final UnaryOperator<String> operator) {
            final Value value = get(index);

            if (value != null) {
                value.matchType()
                    .ifString(s -> set(index, operator != null ? new Value(operator.apply(s)) : null))
                    .orElse(v -> v.transformFields(fields, operator));
            }
        }

        private void insert(final InsertMode mode, final String[] fields, final Value newValue) {
            if (fields[0].equals(ASTERISK)) {
                return; // TODO: WDCD? descend into the array?
            }
            if (ReservedField.fromString(fields[0]) == null) {
                processDefault(mode, fields, newValue);
            }
            else {
                insertIntoReferencedObject(mode, fields, newValue);
            }
        }

        private void insertIntoReferencedObject(final InsertMode mode, final String[] fields, final Value newValue) {
            // TODO replace switch, extract to enum behavior like reservedField.insertIntoReferencedObject(this)?
            switch (ReservedField.fromString(fields[0])) {
                case $append:
                    if (fields.length == 1) {
                        add(newValue);
                    }
                    else {
                        add(newHash(h -> h.insert(mode, tail(fields), newValue)));
                    }
                    break;
                case $last:
                    if (size() > 0) {
                        get(size() - 1).matchType().ifHash(h -> h.insert(mode, tail(fields), newValue));
                    }
                    break;
                case $first:
                    if (size() > 0) {
                        final Value first = get(0);
                        if (first.isHash()) {
                            first.asHash().insert(mode, tail(fields), newValue);
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        private void processDefault(final InsertMode mode, final String[] fields, final Value newValue) {
            if (isNumber(fields[0])) {
                // TODO: WDCD? insert at the given index? also descend into the array?
                if (fields.length == 1) {
                    add(newValue);
                }
                else if (fields.length > 1) {
                    final Value newHash;
                    final int index = Integer.parseInt(fields[0]);
                    if (index <= size()) {
                        newHash = get(index - 1);
                    }
                    else {
                        newHash = Value.newHash();
                        add(newHash);
                    }
                    mode.apply(newHash.asHash(), fields[1], newValue);
                }
            }
            else {
                add(newHash(h -> h.insert(mode, fields, newValue)));
            }
        }

        /*package-private*/ void set(final int index, final Value value) {
            list.set(index, value);
        }

    }

    /**
     * Represents a hash of metadata fields and values.
     */
    public static class Hash extends AbstractValueType {

        private static final String FIELD_PATH_SEPARATOR = "\\.";

        private final Map<String, Value> map = new LinkedHashMap<>();
        private final SimpleRegexTrie<String> trie = new SimpleRegexTrie<>();

        /**
         * Creates an empty instance of {@link Hash}.
         */
        protected Hash() {
        }

        /**
         * Checks whether this hash contains the metadata field.
         *
         * @param field the field name
         * @return true if this hash contains the metadata field, false otherwise
         */
        public boolean containsField(final String field) {
            return matchFields(field, Stream::anyMatch);
        }

        /**
         * Checks whether this hash is empty.
         *
         * @return true if this hash is empty, false otherwise
         */
        public boolean isEmpty() {
            return map.isEmpty();
        }

        /**
         * Gets the number of field/value pairs in this hash.
         *
         * @return the number of field/value pairs in this hash
         */
        public int size() {
            return map.size();
        }

        /**
         * Adds a field/value pair to this hash, provided it's not {@link #isNull(Value) null}.
         *
         * @param field the field name
         * @param value the metadata value
         */
        public void put(final String field, final Value value) {
            if (!isNull(value)) {
                map.put(field, value);
            }
        }

        /**
         * {@link #put(String, Value) Replaces} a field/value pair in this hash,
         * provided the field name is already {@link #containsField(String) present}.
         *
         * @param field the field name
         * @param value the metadata value
         */
        public void replace(final String field, final Value value) {
            if (containsField(field)) {
                put(field, value);
            }
        }

        public Value replace(final String fieldPath, final String newValue) {
            return insert(InsertMode.REPLACE, fieldPath, newValue);
        }

        public Value append(final String fieldPath, final String newValue) {
            return insert(InsertMode.APPEND, fieldPath, newValue);
        }

        /**
         * Retrieves the field value from this hash.
         *
         * @param field the field name
         * @return the metadata value
         */
        public Value get(final String field) {
            // TODO: special treatment (only) for exact matches?
            final List<Value> list = findFields(field).map(map::get).collect(Collectors.toList());
            return list.isEmpty() ? null : list.size() == 1 ? list.get(0) : new Value(list);
        }

        public Value find(final String fieldPath) {
            return find(split(fieldPath));
        }

        private Value find(final String[] fields) {
            final String field = fields[0];
            if (field.equals(ASTERISK)) {
                // TODO: search in all elements of value.asHash()?
                return find(tail(fields));
            }
            return fields.length == 1 || !containsField(field) ? get(field) :
                findNested(field, tail(fields));
        }

        private Value findNested(final String field, final String[] remainingFields) {
            final Value value = get(field);
            return value == null ? null : extractType(null, c -> {
                value.matchType()
                    .ifArray(a -> c.accept(a.find(remainingFields)))
                    .ifHash(h -> c.accept(h.find(remainingFields)))
                    .orElseThrow();
            });
        }

        public Value findList(final String fieldPath, final Consumer<Array> consumer) {
            return asList(find(fieldPath), consumer);
        }

        public Value getList(final String field, final Consumer<Array> consumer) {
            return asList(get(field), consumer);
        }

        private String[] split(final String fieldPath) {
            return fieldPath.split(FIELD_PATH_SEPARATOR);
        }

        public void addAll(final String field, final List<String> values) {
            values.forEach(value -> add(field, new Value(value)));
        }

        public void addAll(final Hash hash) {
            hash.forEach(this::add);
        }

        /**
         * {@link #put(String, Value) Adds} a field/value pair to this hash,
         * potentially merging with an existing value.
         *
         * @param field the field name
         * @param newValue the new metadata value
         */
        public void add(final String field, final Value newValue) {
            final Value oldValue = get(field);
            put(field, oldValue == null ? newValue : oldValue.asList(a1 -> newValue.asList(a2 -> a2.forEach(a1::add))));
        }

        public Value insert(final InsertMode mode, final String fieldPath, final String newValue) {
            return insert(mode, split(fieldPath), new Value(newValue));
        }

        private Value insert(final InsertMode mode, final String[] fields, final Value newValue) {
            final String field = fields[0];
            if (fields.length == 1) {
                if (field.equals(ASTERISK)) {
                    //TODO: WDCD? insert into each element?
                }
                else {
                    mode.apply(this, field, newValue);
                }
            }
            else {
                final String[] tail = tail(fields);
                if (ReservedField.fromString(field) != null || isNumber(field)) {
                    return processRef(mode, newValue, field, tail);
                }
                if (!containsField(field)) {
                    put(field, newHash());
                }
                final Value value = get(field);
                if (value != null) {
                    // TODO: move impl into enum elements, here call only value.insert
                    value.matchType()
                        .ifArray(a -> a.insert(mode, tail, newValue))
                        .ifHash(h -> h.insert(insertMode(mode, field, tail), tail, newValue))
                        .orElseThrow();
                }
            }

            return new Value(this);
        }

        private Value processRef(final InsertMode mode, final Value newValue, final String field, final String[] tail) {
            final Value referencedValue = getReferencedValue(field);
            if (referencedValue != null) {
                return referencedValue.asHash().insert(insertMode(mode, field, tail), tail, newValue);
            }
            else {
                throw new IllegalArgumentException("Using ref, but can't find: " + field + " in: " + this);
            }
        }

        private Value getReferencedValue(final String field) {
            Value referencedValue = null;
            final ReservedField reservedField = ReservedField.fromString(field);
            if (reservedField == null) {
                return get(field);
            }
            // TODO replace switch, extract to enum behavior like reservedField.getReferencedValueInHash(this)?
            switch (reservedField) {
                case $first:
                    referencedValue = get("1");
                    break;
                case $last:
                    referencedValue = get(String.valueOf(size()));
                    break;
                case $append:
                    referencedValue = new Value(this);
                    break;
                default:
                    break;
            }
            return referencedValue;
        }

        private InsertMode insertMode(final InsertMode mode, final String field, final String[] tail) {
            // if the field is marked as array, this hash should be smth. like { 1=a, 2=b }
            final boolean isIndexedArray = field.endsWith(Metafix.ARRAY_MARKER);
            final boolean nextIsRef = tail.length > 0 && (
                    tail[0].startsWith(ReservedField.$first.name()) ||
                    tail[0].startsWith(ReservedField.$last.name()) ||
                    isNumber(tail[0]));
            return isIndexedArray && !nextIsRef ? InsertMode.INDEXED : mode;
        }

        /**
         * Removes the given field/value pair from this hash.
         *
         * @param field the field name
         */
        public void remove(final String field) {
            modifyFields(field, map::remove);
        }

        public void removeNested(final String fieldPath) {
            removeNested(split(fieldPath));
        }

        private void removeNested(final String[] fields) {
            final String field = fields[0];

            if (fields.length == 1) {
                remove(field);
            }
            else if (containsField(field)) {
                final Value value = get(field);
                // TODO: impl and call just value.remove
                if (value != null) {
                    value.matchType()
                        .ifArray(a -> a.removeNested(tail(fields)))
                        .ifHash(h -> h.removeNested(tail(fields)))
                        .orElseThrow();
                }
            }
        }

        public void copy(final List<String> params) {
            final String oldName = params.get(0);
            final String newName = params.get(1);
            findList(oldName, a -> a.forEach(v -> appendValue(split(newName), v)));
        }

        private void appendValue(final String[] newName, final Value v) {
            // TODO: impl and call just value.append
            if (v != null) {
                switch (v.type) {
                    case String:
                        append(String.join(".", newName), v.asString());
                        break;
                    case Array:
                        // TODO: do something here?
                        break;
                    case Hash:
                        if (newName.length == 1) {
                            add(newName[0], v);
                        }
                        else {
                            appendValue(newName, v.asHash().find(tail(newName)));
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        public void transformField(final String field, final UnaryOperator<Value> operator) {
            final Value oldValue = find(field);

            if (oldValue != null) {
                final Value newValue = operator.apply(oldValue);

                if (newValue != null) {
                    insert(InsertMode.REPLACE, split(field), newValue);
                }
            }
        }

        public void transformFields(final List<String> params, final UnaryOperator<String> operator) {
            transformFields(split(params.get(0)), operator);
        }

        private void transformFields(final String[] fields, final UnaryOperator<String> operator) {
            final String field = fields[0];
            final String[] remainingFields = tail(fields);

            if (field.equals(ASTERISK)) {
                // TODO: search in all elements of value.asHash()?
                transformFields(remainingFields, operator);
                return;
            }

            modifyFields(field, f -> {
                final Value value = map.get(f);

                if (value != null) {
                    if (remainingFields.length == 0) {
                        map.remove(f);

                        if (operator != null) {
                            value.asList(a -> a.forEach(v -> {
                                if (isIndexedArray(v)) {
                                    processIndexedArray(operator, f, v.asHash());
                                }
                                else {
                                    append(f, operator.apply(v.toString()));
                                }
                            }));
                        }
                    }
                    else {
                        value.transformFields(remainingFields, operator);
                    }
                }
            });
        }

        private void processIndexedArray(final UnaryOperator<String> operator, final String field, final Hash hash) {
            // TODO: we need recursion here for proper nesting, align with #102
            hash.map.values().forEach(value -> {
                if (value.isHash()) {
                    value.asHash().map.values().forEach(nested -> {
                        append(field, operator.apply(nested.toString()));
                    });
                }
                else {
                    append(field, operator.apply(value.toString()));
                }
            });
        }

        private boolean isIndexedArray(final Value v) {
            return v.isHash() && v.asHash().map.keySet().stream().allMatch(Value::isNumber);
        }

        /**
         * Retains only the given field/value pairs in this hash.
         *
         * @param fields the field names
         */
        public void retainFields(final Collection<String> fields) {
            map.keySet().retainAll(fields.stream().flatMap(this::findFields).collect(Collectors.toSet()));
        }

        /**
         * Removes all field/value pairs from this hash whose value is empty.
         */
        public void removeEmptyValues() {
            // TODO:
            //
            // - Remove empty arrays/hashes?
            // - Remove empty strings(/arrays/hashes) recursively?
            //
            // => Compare Catmandu behaviour
            map.values().removeIf(v -> v.isString() && v.asString().isEmpty());
        }

        /**
         * Iterates over all field/value pairs in this hash.
         *
         * @param consumer the action to be performed for each field/value pair
         */
        public void forEach(final BiConsumer<String, Value> consumer) {
            map.forEach(consumer);
        }

        @Override
        public String toString() {
            return map.toString();
        }

        private void modifyFields(final String pattern, final Consumer<String> consumer) {
            findFields(pattern).collect(Collectors.toSet()).forEach(consumer);
        }

        private Stream<String> findFields(final String pattern) {
            return matchFields(pattern, Stream::filter);
        }

        private <T> T matchFields(final String pattern, final BiFunction<Stream<String>, Predicate<String>, T> function) {
            trie.put(pattern, pattern);
            return function.apply(map.keySet().stream(), f -> trie.get(f).contains(pattern));
        }

    }

}
