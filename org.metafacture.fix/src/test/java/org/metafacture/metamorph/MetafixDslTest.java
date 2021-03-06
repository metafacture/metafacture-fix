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

package org.metafacture.metamorph;

import org.metafacture.framework.StreamReceiver;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import com.google.common.collect.ImmutableMap;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

/**
 * Tests the basic functionality of Metafix via DSL.
 *
 * @author Christoph Böhme (MetamorphTest)
 * @author Fabian Steeg (MetafixDslTest)
 */
@ExtendWith(MockitoExtension.class)
public class MetafixDslTest {

    private static final String LITERAL_A = "lit-A";
    private static final String LITERAL_ALOHA = "Aloha";
    private static final String LITERAL_B = "lit-B";
    private static final String LITERAL_HAWAII = "Hawaii";
    private static final String LITERAL_MOIN = "Moin";
    private static final String LITERAL_MOIN_MOIN = "Moin Moin";
    private static final String LITERAL_LANGEOOG = "Langeoog";
    private static final String LITERAL_BALTRUM = "Baltrum";

    @RegisterExtension
    private MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StreamReceiver streamReceiver;

    public MetafixDslTest() {
    }

    @Test
    public void shouldMapLiteral() {
        final Metafix metafix = fix(
                "map(a,b)"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("b", LITERAL_ALOHA);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapAndFilterNeqLiteral() {
        final Metafix metafix = fix(
                "do map(a,b)", // checkstyle-disable-line MultipleStringLiterals
                "  not_equals(string: '')",
                "end" // checkstyle-disable-line MultipleStringLiterals
        );

        metafix.startRecord("1");
        metafix.literal("a", "");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("b", LITERAL_ALOHA);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapAndFilterEqLiteral() {
        final Metafix metafix = fix(
                "do map(a,b)", // checkstyle-disable-line MultipleStringLiterals
                "  equals(string: '')",
                "end" // checkstyle-disable-line MultipleStringLiterals
        );

        metafix.startRecord("1");
        metafix.literal("a", "");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("b", "");
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapAndFilterRegexLiteral() {
        final Metafix metafix = fix(
                "do map(a,b)", // checkstyle-disable-line MultipleStringLiterals
                "  regexp(match: '.+')",
                "end" // checkstyle-disable-line MultipleStringLiterals
        );

        metafix.startRecord("1");
        metafix.literal("a", "");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("b", LITERAL_ALOHA);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapAndChangeLiteralSingle() {
        final Metafix metafix = fix(
                "do map(a,b)", // checkstyle-disable-line MultipleStringLiterals
                "  replace_all(pattern: 'a-val', with: 'b-val')",
                "end" // checkstyle-disable-line MultipleStringLiterals
        );

        metafix.startRecord("1");
        metafix.literal("a", "a-val");
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("b", "b-val");
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapAndChangeLiteralMulti() {
        final Metafix metafix = fix(
                "do map(a,b)",
                "  replace_all(pattern: 'a-val', with: 'b-val')",
                "  compose(prefix: 'pre-')",
                "  compose(postfix: '-post')",
                "end" // checkstyle-disable-line MultipleStringLiterals
        );

        metafix.startRecord("1");
        metafix.literal("a", "a-val");
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("b", "pre-b-val-post");
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapNested() {
        final Metafix metafix = fix(
                "map(a,b.c)"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("b");
        ordered.verify(streamReceiver).literal("c", LITERAL_ALOHA);
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapNestedMulti() {
        final Metafix metafix = fix(
                "map(a,b)",
                "map(a.c, b.c)",
                "map(a.d, b.d)"
        );

        metafix.startRecord("1");
        metafix.literal("a.c", LITERAL_A);
        metafix.literal("a.d", LITERAL_B);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("b");
        ordered.verify(streamReceiver).literal("c", LITERAL_A);
        ordered.verify(streamReceiver).literal("d", LITERAL_B);
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldAddLiteral() {
        final Metafix metafix = fix(
                "add_field(a,'A')"
        );

        metafix.startRecord("1");
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("a", "A");
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldAddEntity() {
        final Metafix metafix = fix(
                "add_field(a.b,'AB')"
        );

        metafix.startRecord("1");
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("a");
        ordered.verify(streamReceiver).literal("b", "AB");
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldAddEntityNested() {
        final Metafix metafix = fix(
                "add_field(a.b.c.d.e,'ABCDE')"
        );

        metafix.startRecord("1");
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("a");
        ordered.verify(streamReceiver).startEntity("b");
        ordered.verify(streamReceiver).startEntity("c");
        ordered.verify(streamReceiver).startEntity("d");
        ordered.verify(streamReceiver).literal("e", "ABCDE");
        ordered.verify(streamReceiver, Mockito.times(4)).endEntity(); // checkstyle-disable-line MagicNumber
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldReplaceInLiteral() {
        final Metafix metafix = fix(
                "do map(a)", // checkstyle-disable-line MultipleStringLiterals
                "  replace_all(pattern: 'a', with: 'b')",
                "end"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("a", LITERAL_ALOHA.replaceAll("a", "b"));
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldReplaceWithEntities() {
        final Metafix metafix = fix(
                "do map(a.b, a.b)",
                "  replace_all(pattern: 'a', with: 'b')",
                "end"
        );

        metafix.startRecord("1");
        metafix.startEntity("a");
        metafix.literal("b", LITERAL_ALOHA);
        metafix.endEntity();
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("a");
        ordered.verify(streamReceiver).literal("b", LITERAL_ALOHA.replaceAll("a", "b"));
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldTrim() {
        final Metafix metafix = fix(
                "do map(a,b)",
                "  trim()",
                "end"
        );

        metafix.startRecord("1");
        metafix.literal("a", "\t" + LITERAL_ALOHA + " ");
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("b", LITERAL_ALOHA);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void appendLiteral() {
        final Metafix metafix = fix(
                "do map(a)",
                "  compose(postfix: 'eha')",
                "end"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("a", LITERAL_ALOHA + "eha"); // checkstyle-disable-line MultipleStringLiterals
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void prependLiteral() {
        final Metafix metafix = fix(
                "do map(a)",
                "  compose(prefix: 'eha')",
                "end"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("a", "eha" + LITERAL_ALOHA);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void prependLiteralWithVarVal() {
        final Metafix metafix = fix(
                ImmutableMap.of("pre", "eha"),
                "do map(a)",
                "  compose(prefix: '$[pre]')",
                "end"
        );
        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("a", "eha" + LITERAL_ALOHA);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void prependLiteralWithVarKey() {
        final Metafix metafix = fix(
                ImmutableMap.of("composeOperation", "prefix"),
                "do map(a)",
                "  compose('$[composeOperation]': 'eha')",
                "end"
        );
        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("a", "eha" + LITERAL_ALOHA);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldHandleUnmatchedLiteralsInElseSource() {
        final Metafix metafix = fix(
                "map(Sylt,Hawaii)",
                "map(_else)"
        );

        metafix.startRecord("1");
        metafix.literal(LITERAL_LANGEOOG, LITERAL_MOIN);
        metafix.literal("Sylt", LITERAL_ALOHA);
        metafix.literal(LITERAL_BALTRUM, LITERAL_MOIN_MOIN);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal(LITERAL_LANGEOOG, LITERAL_MOIN);
        ordered.verify(streamReceiver).literal(LITERAL_HAWAII, LITERAL_ALOHA);
        ordered.verify(streamReceiver).literal(LITERAL_BALTRUM, LITERAL_MOIN_MOIN);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldAllowTreatingEntityEndEventsAsLiterals() {
        final Metafix metafix = fix(
                "map(e1)",
                "map(e1.e2)",
                "map(e1.e2.d)"
        );

        metafix.startRecord("entity end info");
        metafix.startEntity("e1");
        metafix.startEntity("e2");
        metafix.literal("d", "a");
        metafix.endEntity();
        metafix.endEntity();
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("entity end info");
        ordered.verify(streamReceiver).literal("e1.e2.d", "a");
        ordered.verify(streamReceiver).literal("e1.e2", "");
        ordered.verify(streamReceiver).literal("e1", "");
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldLookupInline() {
        verifyLookup(fix(
                "do map(a)", // checkstyle-disable-line MultipleStringLiterals
                "  lookup(Aloha: Alohaeha, 'Moin': 'Moin zäme', __default: Tach)", // checkstyle-disable-line MultipleStringLiterals
                "end"
        ));
    }

    @Test
    public void shouldLookupInTsvFile() throws URISyntaxException {
        verifyLookup(fix(
                "do map(a)",
                "  lookup(in: 'src/test/java/org/metafacture/metamorph/maps/test.tsv')",
                "end"
        ));
    }

    @Test
    public void shouldLookupInCsvFile() throws URISyntaxException {
        verifyLookup(fix(
                "do map(a)",
                "  lookup(in: 'src/test/java/org/metafacture/metamorph/maps/test.csv', separator: ',')",
                "end"
        ));
    }

    private void verifyLookup(final Metafix metafix) {
        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.literal("a", LITERAL_MOIN);
        metafix.literal("a", LITERAL_HAWAII);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("a", "Alohaeha"); // checkstyle-disable-line MultipleStringLiterals
        ordered.verify(streamReceiver).literal("a", "Moin zäme"); // checkstyle-disable-line MultipleStringLiterals
        ordered.verify(streamReceiver).literal("a", "Tach"); // checkstyle-disable-line MultipleStringLiterals
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapAndLookupInline() {
        verifyMapAndLookup(fix(
                "do map(a,b)",
                "  lookup(Aloha: Alohaeha, 'Moin': 'Moin zäme', __default: Tach)", // checkstyle-disable-line MultipleStringLiterals
                "end"
        ));
    }

    @Test
    public void shouldMapAndLookupInTsvFile() {
        verifyMapAndLookup(fix(
                "do map(a,b)",
                "  lookup(in: 'src/test/java/org/metafacture/metamorph/maps/test.tsv')", // checkstyle-disable-line MultipleStringLiterals
                "end"
        ));
    }

    @Test
    public void shouldMapAndLookupInCsvFile() {
        verifyMapAndLookup(fix(
                "do map(a,b)",
                "  lookup(in: 'src/test/java/org/metafacture/metamorph/maps/test.csv', separator: ',')", // checkstyle-disable-line MultipleStringLiterals
                "end"
        ));
    }

    private void verifyMapAndLookup(final Metafix metafix) {
        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.literal("a", LITERAL_MOIN);
        metafix.literal("a", LITERAL_HAWAII);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("b", "Alohaeha");
        ordered.verify(streamReceiver).literal("b", "Moin zäme");
        ordered.verify(streamReceiver).literal("b", "Tach");
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapToExplicitEntityAndLookupInline() {
        final Metafix metafix = fix(
                "do entity('c')",
                "  do map(a,b)",
                "    lookup(Aloha: Alohaeha, 'Moin': 'Moin zäme', __default: Tach)",
                "  end", // checkstyle-disable-line MultipleStringLiterals
                "end"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.literal("a", LITERAL_MOIN);
        metafix.literal("a", LITERAL_HAWAII);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("c");
        ordered.verify(streamReceiver).literal("b", "Alohaeha");
        ordered.verify(streamReceiver).literal("b", "Moin zäme");
        ordered.verify(streamReceiver).literal("b", "Tach");
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapToImplicitEntityAndLookupInline() {
        final Metafix metafix = fix(
                "do map(a,c.b)",
                "  lookup(Aloha: Alohaeha, 'Moin': 'Moin zäme', __default: Tach)",
                "end"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.literal("a", LITERAL_MOIN);
        metafix.literal("a", LITERAL_HAWAII);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("c");
        ordered.verify(streamReceiver).literal("b", "Alohaeha");
        ordered.verify(streamReceiver).literal("b", "Moin zäme");
        ordered.verify(streamReceiver).literal("b", "Tach");
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapAndChangeAndLookupInline() {
        final Metafix metafix = fix(
                "do map(a,b)",
                "  replace_all(pattern: 'lit-A', with: 'Aloha')",
                "  replace_all(pattern: 'lit-B', with: 'Moin')",
                "  lookup(Aloha: Alohaeha, 'Moin': 'Moin zäme', __default: Tach)",
                "end"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_A);
        metafix.literal("a", LITERAL_B);
        metafix.literal("a", LITERAL_HAWAII);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("b", "Alohaeha");
        ordered.verify(streamReceiver).literal("b", "Moin zäme");
        ordered.verify(streamReceiver).literal("b", "Tach");
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldCombineLiterals() {
        final Metafix metafix = fix(
                "do combine(name: d, value: '${place}, ${greet}')",
                "  map(a, greet)",
                "  map(b, place)",
                "end",
                "map(c, e)"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.literal("b", LITERAL_HAWAII);
        metafix.literal("c", LITERAL_MOIN);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("d", LITERAL_HAWAII + ", " + LITERAL_ALOHA);
        ordered.verify(streamReceiver).literal("e", LITERAL_MOIN);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldDoEntityAndMap() {
        final Metafix metafix = fix(
                "do entity(b)",
                " map(a, c)",
                "end"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_A);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("b");
        ordered.verify(streamReceiver).literal("c", LITERAL_A);
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapAndChangeNestedExplicit() {
        final Metafix metafix = fix(
                "do entity(b)",
                " do map(a, c)",
                "  replace_all(pattern: 'A', with: 'B')",
                " end",
                "end"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_A);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("b");
        ordered.verify(streamReceiver).literal("c", LITERAL_B);
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldMapAndChangeNestedImplicit() {
        final Metafix metafix = fix(
                "do map(a, b.c)",
                "  replace_all(pattern: 'A', with: 'B')",
                "end",
                "map(x, y)"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_A);
        metafix.literal("x", LITERAL_A);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("b");
        ordered.verify(streamReceiver).literal("c", LITERAL_B);
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).literal("y", LITERAL_A);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldCombineToEntity() {
        final Metafix metafix = fix(
                "do entity(d)",
                " do combine(name: a, value: '${place}, ${greet}')",
                "  map(a, greet)",
                "  map(b, place)",
                " end",
                "end",
                "map(c, e)"
        );

        metafix.startRecord("1");
        metafix.literal("a", LITERAL_ALOHA);
        metafix.literal("b", LITERAL_HAWAII);
        metafix.literal("c", LITERAL_MOIN);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).startEntity("d");
        ordered.verify(streamReceiver).literal("a", LITERAL_HAWAII + ", " + LITERAL_ALOHA);
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).literal("e", LITERAL_MOIN);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    public void shouldChooseFirstMapFlushWith() {
        final Metafix metafix = fix(
                "do choose(flushWith: 'd|record')",
                "  map(a, c)",
                "  map(b, c)",
                "end",
                "map(d,e)" // checkstyle-disable-line MultipleStringLiterals
        );

        metafix.startRecord("1");
        metafix.literal("b", LITERAL_B);
        metafix.literal("a", LITERAL_A);
        metafix.literal("d", LITERAL_B);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("c", LITERAL_A);
        ordered.verify(streamReceiver).literal("e", LITERAL_B);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    @Disabled("Fix choose flushing")
    public void shouldChooseFirstMap() {
        final Metafix metafix = fix(
                "do choose()",
                "  map(a, c)",
                "  map(b, c)",
                "end",
                "map(d,e)"
        );

        metafix.startRecord("1");
        metafix.literal("b", LITERAL_B);
        metafix.literal("a", LITERAL_A);
        metafix.literal("d", LITERAL_B);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("c", LITERAL_A);
        ordered.verify(streamReceiver).literal("e", LITERAL_B);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    @Disabled("Fix choose flushing")
    public void shouldChooseFirstDoMap() {
        final Metafix metafix = fix(
                "do choose()",
                "  do map(a, c)",
                "    not_equals('')",
                "  end",
                "  do map(b, c)",
                "    not_equals('')",
                "  end",
                "end",
                "map(d,e)"
        );

        metafix.startRecord("1");
        metafix.literal("b", LITERAL_B);
        metafix.literal("a", LITERAL_A);
        metafix.literal("d", LITERAL_B);
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("c", LITERAL_A);
        ordered.verify(streamReceiver).literal("e", LITERAL_B);
        ordered.verify(streamReceiver).endRecord();
        ordered.verifyNoMoreInteractions();
    }

    @Test
    @Disabled("Fix syntax")
    public void shouldUseCustomEntityMarker() {
        final Metafix metafix = fix(
                "map(entity~literal,data)"
        );

        metafix.startRecord("1");
        metafix.startEntity("entity");
        metafix.literal("literal", LITERAL_ALOHA);
        metafix.endEntity();
        metafix.endRecord();

        Mockito.verify(streamReceiver).literal("data", LITERAL_ALOHA);
    }

    @Test
    public void shouldMatchCharacterWithQuestionMarkWildcard() {
        final Metafix metafix = fix(
                "map('lit-?')"
        );

        metafix.startRecord("1");
        metafix.literal("lit", LITERAL_MOIN);
        metafix.literal(LITERAL_A, LITERAL_ALOHA);
        metafix.literal(LITERAL_B, "Aloha 'oe");
        metafix.endRecord();

        Mockito.verify(streamReceiver).literal(LITERAL_A, LITERAL_ALOHA);
        Mockito.verify(streamReceiver).literal(LITERAL_B, "Aloha 'oe");
        Mockito.verify(streamReceiver, Mockito.times(2)).literal(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void shouldMatchCharactersInCharacterClass() {
        final Metafix metafix = fix(
                "map('lit-[AB]')"
        );

        metafix.startRecord("1");
        metafix.literal(LITERAL_A, LITERAL_HAWAII);
        metafix.literal(LITERAL_B, "Oahu");
        metafix.literal("lit-C", "Fehmarn");
        metafix.endRecord();

        Mockito.verify(streamReceiver).literal(LITERAL_A, LITERAL_HAWAII);
        Mockito.verify(streamReceiver).literal(LITERAL_B, "Oahu");
        Mockito.verify(streamReceiver, Mockito.times(2)).literal(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void shouldNotFeedbackJsonLdKeywords() {
        final Metafix metafix = fix(
                "map(_else)"
        );

        metafix.startRecord("1");
        metafix.literal("@id", LITERAL_HAWAII);
        metafix.endRecord();

        Mockito.verify(streamReceiver).literal("@id", LITERAL_HAWAII);
    }

    @Test
    public void shouldNotFeedbackJsonLdKeywordsNested() {
        final Metafix metafix = fix(
                "do entity('USA')",
                "  map('USA.Sylt', 'Hawaii')",
                "end",
                "map(_elseNested)"
        );

        metafix.startRecord("1");
        metafix.literal("Shikotan", "Aekap");
        metafix.startEntity("Germany");
        metafix.literal(LITERAL_LANGEOOG, LITERAL_MOIN);
        metafix.literal(LITERAL_BALTRUM, LITERAL_MOIN_MOIN);
        metafix.endEntity();
        metafix.startEntity("USA");
        metafix.literal("Sylt", LITERAL_ALOHA);
        metafix.endEntity();
        metafix.endRecord();

        final InOrder ordered = Mockito.inOrder(streamReceiver);
        ordered.verify(streamReceiver).startRecord("1");
        ordered.verify(streamReceiver).literal("Shikotan", "Aekap");
        ordered.verify(streamReceiver).startEntity("Germany");
        ordered.verify(streamReceiver).literal(LITERAL_LANGEOOG, "Moin");
        ordered.verify(streamReceiver).literal(LITERAL_BALTRUM, LITERAL_MOIN_MOIN);
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).startEntity("USA");
        ordered.verify(streamReceiver).literal("Hawaii", "Aloha");
        ordered.verify(streamReceiver).endEntity();
        ordered.verify(streamReceiver).endRecord();
    }

    @Test
    @Disabled("Fix syntax")
    public void shouldReplaceVariables() {
        final Metafix metafix = fix(
                "vars(in: Honolulu, out: Hawaii)",
                "map($[in],$[out])"
        );

        metafix.startRecord("1");
        metafix.literal("Honolulu", LITERAL_ALOHA);
        metafix.endRecord();

        Mockito.verify(streamReceiver).literal(LITERAL_HAWAII, LITERAL_ALOHA);
    }

    private Metafix fix(final String... fix) {
        return fix(Collections.emptyMap(), fix);
    }

    private Metafix fix(final Map<String, String> vars, final String... fix) {
        final String fixString = String.join("\n", fix);
        System.out.println("\nFix string: " + fixString);

        Metafix metafix = null;
        try {
            metafix = new Metafix(fixString, vars);
            metafix.setReceiver(streamReceiver);
        }
        catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
        return metafix;
    }

}
