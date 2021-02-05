package org.metafacture.fix.interpreter;

import org.metafacture.fix.fix.Do;
import org.metafacture.fix.fix.ElsIf;
import org.metafacture.fix.fix.Else;
import org.metafacture.fix.fix.Expression;
import org.metafacture.fix.fix.Fix;
import org.metafacture.fix.fix.If;
import org.metafacture.fix.fix.MethodCall;
import org.metafacture.metamorph.Metafix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.interpreter.impl.XbaseInterpreter;

public class FixInterpreter extends XbaseInterpreter {

    private static final Logger LOG = LoggerFactory.getLogger(FixInterpreter.class);

    private Metafix metafix;

    public FixInterpreter() {
    }

    public void run(final Metafix metafixParam, final EObject program) {
        if (metafixParam != null && program != null) {
            metafix = metafixParam;

            if (program instanceof Fix) {
                for (final Expression expression : ((Fix) program).getElements()) {
                    process(expression);
                }
            }
        }
    }

    private void process(final Expression expression) { // checkstyle-disable-line CyclomaticComplexity|NPathComplexity
        metafix.getExpressions().add(expression);

        boolean matched = false;

        if (expression instanceof If) {
            matched = true;

            final If ifExpression = (If) expression;
            LOG.debug("if: {}", ifExpression);

            for (final Expression element : ifExpression.getElements()) {
                process(element);
            }

            final ElsIf elseIfExpression = ifExpression.getElseIf();
            if (elseIfExpression != null) {
                LOG.debug("else if: {}", elseIfExpression);

                for (final Expression element : elseIfExpression.getElements()) {
                    process(element);
                }
            }

            final Else elseExpression = ifExpression.getElse();
            if (elseExpression != null) {
                LOG.debug("else: {}", elseExpression);

                for (final Expression element : elseExpression.getElements()) {
                    process(element);
                }
            }
        }

        if (!matched) {
            if (expression instanceof Do) {
                matched = true;

                final Do doExpression = (Do) expression;
                LOG.debug("do: {}", doExpression);

                for (final Expression element : doExpression.getElements()) {
                    process(element);
                }
            }
        }

        if (!matched) {
            if (expression instanceof MethodCall) {
                matched = true;
                LOG.debug("method call: {}", expression);
            }
        }
    }

}
