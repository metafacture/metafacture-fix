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

import org.metafacture.commons.StringUtil;
import org.metafacture.framework.MetafactureException;
import org.metafacture.metafix.FixPredicate.Quantifier;
import org.metafacture.metafix.fix.Do;
import org.metafacture.metafix.fix.ElsIf;
import org.metafacture.metafix.fix.Else;
import org.metafacture.metafix.fix.Expression;
import org.metafacture.metafix.fix.Fix;
import org.metafacture.metafix.fix.If;
import org.metafacture.metafix.fix.MethodCall;
import org.metafacture.metafix.fix.Options;
import org.metafacture.metafix.fix.Unless;

import com.google.common.collect.ImmutableMap;
import org.eclipse.emf.common.util.EList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transform a record using a Fix
 *
 * @author Fabian Steeg
 *
 */
class RecordTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(RecordTransformer.class);

    private Fix fix;
    private Map<String, Object> record;
    private Map<String, String> vars;

    RecordTransformer(final Map<String, Object> record, final Map<String, String> vars, final Fix fix) {
        this.record = new LinkedHashMap<>(record);
        this.vars = vars;
        this.fix = fix;
    }

    Map<String, Object> transform() {
        processSubexpressions(fix.getElements());
        return record;
    }

    Map<String, Object> getRecord() {
        return record;
    }

    private String resolveVars(final String string) {
        return string == null ? null : StringUtil.format(string, Metafix.VAR_START, Metafix.VAR_END, false, vars);
    }

    private void processSubexpressions(final List<Expression> expressions) {
        for (final Expression sub : expressions) {
            final EList<String> params = sub.getParams();
            if (sub instanceof Do) {
                processBind((Do) sub, params);
            }
            else if (sub instanceof If) {
                processIf((If) sub, params);
            }
            else if (sub instanceof Unless) {
                processUnless((Unless) sub, params);
            }
            else {
                processFunction(sub, params);
            }
        }
    }

    private void processBind(final Do theDo, final EList<String> params) {
        if (theDo.getName().equals("list")) { // TODO impl multiple binds via FixBind enum
            final Map<String, String> options = options(theDo.getOptions());
            final Map<String, Object> fullRecord = new LinkedHashMap<>(record);
            final Object values = FixMethod.find(record, FixMethod.split(options.get("path")));
            Metafix.asList(values).stream().filter(val -> val != null).forEach(val -> {
                // for each val, bind the current record/scope/context to the given var name:
                record = new LinkedHashMap<>(ImmutableMap.of(options.get("var"), val));
                processSubexpressions(theDo.getElements());
                record.remove(options.get("var"));
                // and remember the things we added while bound (this probably needs some tweaking):
                Metafix.addAll(fullRecord, record);
            });
            record = fullRecord;
        }
        else {
            LOG.warn("Unprocessed bind: {}", theDo);
            // TODO, possibly: use morph collectors here
            // final CollectFactory collectFactory = new CollectFactory();
            // final Map<String, String> attributes = resolvedAttributeMap(params, theDo.getOptions());
            // final Collect collect = collectFactory.newInstance(expression.getName(), attributes);
        }
    }

    private void processIf(final If ifExp, final EList<String> parameters) {
        final ElsIf elsIfExp = ifExp.getElseIf();
        final Else elseExp = ifExp.getElse();
        if (testConditional(ifExp.getName(), parameters)) {
            processSubexpressions(ifExp.getElements());
        }
        else if (elsIfExp != null && testConditional(elsIfExp.getName(), elsIfExp.getParams())) {
            processSubexpressions(elsIfExp.getElements());
        }
        else if (elseExp != null) {
            processSubexpressions(elseExp.getElements());
        }
    }

    private void processUnless(final Unless unless, final EList<String> parameters) {
        if (!testConditional(unless.getName(), parameters)) {
            processSubexpressions(unless.getElements());
        }
    }

    private boolean testConditional(final String conditional, final EList<String> params) {
        LOG.debug("<IF>: {} parameters: {}", conditional, params);
        boolean result = false;
        if ("exists".equals(conditional)) {
            return record.containsKey(params.get(0));
        }
        if (!conditional.contains("_")) {
            throw new IllegalArgumentException("Missing quantifier prefix (all_, any_, none_) for " + conditional);
        }
        final String[] quantifierAndPredicate = conditional.split("_");
        try {
            final Quantifier quantifier = FixPredicate.Quantifier.valueOf(quantifierAndPredicate[0]);
            final FixPredicate predicate = FixPredicate.valueOf(quantifierAndPredicate[1]);
            result = quantifier.test(record, predicate, params);
        }
        catch (final IllegalArgumentException e) {
            throw new MetafactureException(e);
        }
        // TODO, possibly: use morph functions here (& in processFunction):
        // final FunctionFactory functionFactory = new FunctionFactory();
        // functionFactory.registerClass("not_equals", NotEquals.class);
        // functionFactory.registerClass("replace_all", Replace.class);
        // final Function function = functionFactory.newInstance(conditional,
        // resolvedAttributeMap(parameters, theIf.getOptions()));
        return result;
    }

    private void processFunction(final Expression expression, final List<String> params) {
        try {
            final FixMethod method = FixMethod.valueOf(expression.getName());
            final List<String> resolvedParams = params.stream().map(this::resolveVars).collect(Collectors.toList());
            final Map<String, String> options = options(((MethodCall) expression).getOptions());
            method.apply(record, resolvedParams, options);
        }
        catch (final IllegalArgumentException e) {
            throw new MetafactureException(e);
        }
    }

    private Map<String, String> options(final Options options) {
        final Map<String, String> map = new HashMap<>();
        if (options != null) {
            for (int i = 0; i < options.getKeys().size(); i += 1) {
                map.put(options.getKeys().get(i), options.getValues().get(i));
            }
        }
        return map;
    }

}