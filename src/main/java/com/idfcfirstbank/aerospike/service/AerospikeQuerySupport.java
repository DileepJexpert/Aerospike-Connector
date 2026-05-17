package com.idfcfirstbank.aerospike.service;

import com.aerospike.client.exp.Exp;
import com.aerospike.client.exp.Expression;
import com.aerospike.client.query.Filter;
import com.idfcfirstbank.aerospike.util.AerospikeValidation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translates a Mule-friendly criteria map into an Aerospike server-side
 * {@link Expression} and an optional secondary-index {@link Filter}.
 *
 * <p>Criteria shape:
 * <pre>
 * {
 *   match: "AND" | "OR",            // optional, default AND
 *   conditions: [
 *     { field: "city",   op: "EQ",      value: "Pune" },
 *     { field: "age",    op: "GT",      value: 30 },
 *     { field: "score",  op: "BETWEEN", value: 10, value2: 20 },
 *     { field: "status", op: "IN",      values: ["A", "B"] }
 *   ],
 *   index: { field: "city", op: "EQ", value: "Pune" }      // optional
 *   // or: index: { field: "age", op: "RANGE", begin: 18, end: 65 }
 * }
 * </pre>
 *
 * <p>Supported operators: {@code EQ, NE, GT, GE, LT, LE, BETWEEN, IN}.
 * Ordering operators ({@code GT, GE, LT, LE, BETWEEN}) require numeric values.
 * The optional {@code index} block, when present, is applied as a secondary
 * index filter so the server narrows the candidate set before evaluating the
 * remaining conditions; without it the query is a full primary-index scan with
 * the expression applied server-side.
 */
final class AerospikeQuerySupport {

    private AerospikeQuerySupport() {
    }

    static Expression toExpression(Map<String, Object> criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("criteria must not be null");
        }

        Object rawConditions = criteria.get("conditions");
        if (!(rawConditions instanceof List<?>) || ((List<?>) rawConditions).isEmpty()) {
            throw new IllegalArgumentException("criteria.conditions must be a non-empty list");
        }

        String match = stringOrDefault(criteria.get("match"), "AND").toUpperCase();
        if (!"AND".equals(match) && !"OR".equals(match)) {
            throw new IllegalArgumentException("criteria.match must be 'AND' or 'OR'");
        }

        List<Exp> predicates = new ArrayList<Exp>();
        for (Object rawCondition : (List<?>) rawConditions) {
            if (!(rawCondition instanceof Map<?, ?>)) {
                throw new IllegalArgumentException("each condition must be a map");
            }
            predicates.add(condition(asStringKeyedMap(rawCondition)));
        }

        Exp combined = predicates.size() == 1
                ? predicates.get(0)
                : ("OR".equals(match)
                        ? Exp.or(predicates.toArray(new Exp[0]))
                        : Exp.and(predicates.toArray(new Exp[0])));
        return Exp.build(combined);
    }

    static Filter indexFilter(Map<String, Object> criteria) {
        if (criteria == null) {
            return null;
        }
        Object rawIndex = criteria.get("index");
        if (rawIndex == null) {
            return null;
        }
        if (!(rawIndex instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("criteria.index must be a map");
        }

        Map<String, Object> index = asStringKeyedMap(rawIndex);
        String field = field(index.get("field"));
        String op = stringOrDefault(index.get("op"), "EQ").toUpperCase();

        if ("EQ".equals(op)) {
            Object value = index.get("value");
            if (value instanceof Number) {
                return Filter.equal(field, integralValue((Number) value, "index.value"));
            }
            if (value instanceof String && !((String) value).trim().isEmpty()) {
                return Filter.equal(field, ((String) value).trim());
            }
            throw new IllegalArgumentException("index.value must be a non-blank String or integral Number");
        }
        if ("RANGE".equals(op)) {
            long begin = integralValue(asNumber(index.get("begin"), "index.begin"), "index.begin");
            long end = integralValue(asNumber(index.get("end"), "index.end"), "index.end");
            if (begin > end) {
                throw new IllegalArgumentException("index.begin must be <= index.end");
            }
            return Filter.range(field, begin, end);
        }
        throw new IllegalArgumentException("criteria.index.op must be 'EQ' or 'RANGE'");
    }

    private static Exp condition(Map<String, Object> condition) {
        String field = field(condition.get("field"));
        Object rawOp = condition.get("op");
        if (rawOp == null) {
            throw new IllegalArgumentException("condition.op is required for field '" + field + "'");
        }
        String op = String.valueOf(rawOp).trim().toUpperCase();

        if ("IN".equals(op)) {
            Object rawValues = condition.get("values");
            if (!(rawValues instanceof List<?>) || ((List<?>) rawValues).isEmpty()) {
                throw new IllegalArgumentException("condition.values must be a non-empty list for IN on '" + field + "'");
            }
            List<Exp> matches = new ArrayList<Exp>();
            for (Object value : (List<?>) rawValues) {
                matches.add(comparison("EQ", field, value));
            }
            return matches.size() == 1 ? matches.get(0) : Exp.or(matches.toArray(new Exp[0]));
        }

        if ("BETWEEN".equals(op)) {
            Number low = asNumber(condition.get("value"), "condition.value");
            Number high = asNumber(condition.get("value2"), "condition.value2");
            return Exp.and(
                    Exp.ge(numericBin(field, low), numericLiteral(low)),
                    Exp.le(numericBin(field, high), numericLiteral(high)));
        }

        return comparison(op, field, condition.get("value"));
    }

    private static Exp comparison(String op, String field, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("condition.value must not be null for field '" + field + "'");
        }

        boolean ordering = "GT".equals(op) || "GE".equals(op) || "LT".equals(op) || "LE".equals(op);
        if (ordering && !(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "operator " + op + " on '" + field + "' requires a numeric value");
        }

        Exp bin = bin(field, value);
        Exp literal = literal(value);
        switch (op) {
            case "EQ":
                return Exp.eq(bin, literal);
            case "NE":
                return Exp.ne(bin, literal);
            case "GT":
                return Exp.gt(bin, literal);
            case "GE":
                return Exp.ge(bin, literal);
            case "LT":
                return Exp.lt(bin, literal);
            case "LE":
                return Exp.le(bin, literal);
            default:
                throw new IllegalArgumentException(
                        "unsupported operator '" + op + "'; expected one of EQ, NE, GT, GE, LT, LE, BETWEEN, IN");
        }
    }

    private static Exp bin(String field, Object value) {
        if (value instanceof Boolean) {
            return Exp.boolBin(field);
        }
        if (value instanceof Number) {
            return numericBin(field, (Number) value);
        }
        if (value instanceof String) {
            return Exp.stringBin(field);
        }
        throw new IllegalArgumentException(
                "unsupported value type for field '" + field + "': " + value.getClass().getName());
    }

    private static Exp literal(Object value) {
        if (value instanceof Boolean) {
            return Exp.val((Boolean) value);
        }
        if (value instanceof Number) {
            return numericLiteral((Number) value);
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) {
                throw new IllegalArgumentException("string condition value must not be blank");
            }
            return Exp.val(text);
        }
        throw new IllegalArgumentException("unsupported value type: " + value.getClass().getName());
    }

    private static Exp numericBin(String field, Number value) {
        return isIntegral(value) ? Exp.intBin(field) : Exp.floatBin(field);
    }

    private static Exp numericLiteral(Number value) {
        return isIntegral(value)
                ? Exp.val(integralValue(value, "value"))
                : Exp.val(value.doubleValue());
    }

    private static boolean isIntegral(Number value) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().scale() <= 0;
        }
        if (value instanceof BigInteger || value instanceof Long
                || value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return true;
        }
        double doubleValue = value.doubleValue();
        return doubleValue == Math.rint(doubleValue) && !Double.isInfinite(doubleValue);
    }

    private static long integralValue(Number value, String name) {
        if (value instanceof BigDecimal) {
            try {
                return ((BigDecimal) value).stripTrailingZeros().longValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(name + " exceeds the supported 64-bit integer range", exception);
            }
        }
        if (value instanceof BigInteger) {
            try {
                return ((BigInteger) value).longValueExact();
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException(name + " exceeds the supported 64-bit integer range", exception);
            }
        }
        return value.longValue();
    }

    private static Number asNumber(Object value, String name) {
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(name + " must be a numeric value");
        }
        return (Number) value;
    }

    private static String field(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("condition.field is required");
        }
        String field = String.valueOf(value).trim();
        AerospikeValidation.requireNotBlank(field, "condition.field");
        return field;
    }

    private static String stringOrDefault(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asStringKeyedMap(Object value) {
        return (Map<String, Object>) value;
    }
}
