package com.idfcfirstbank.aerospike.service;

import com.aerospike.client.exp.Expression;
import com.aerospike.client.query.Filter;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AerospikeQuerySupportTest {

    private static Map<String, Object> condition(String field, String op, Object value) {
        Map<String, Object> condition = new LinkedHashMap<String, Object>();
        condition.put("field", field);
        condition.put("op", op);
        condition.put("value", value);
        return condition;
    }

    private static Map<String, Object> criteria(String match, List<Map<String, Object>> conditions) {
        Map<String, Object> criteria = new LinkedHashMap<String, Object>();
        if (match != null) {
            criteria.put("match", match);
        }
        criteria.put("conditions", conditions);
        return criteria;
    }

    @Test
    void buildsCompoundAndExpression() {
        Expression expression = AerospikeQuerySupport.toExpression(criteria("AND", Arrays.asList(
                condition("city", "EQ", "Pune"),
                condition("age", "GT", 30))));

        assertNotNull(expression);
    }

    @Test
    void buildsOrExpressionAndSingleConditionWithoutLogicalWrapper() {
        assertNotNull(AerospikeQuerySupport.toExpression(criteria("OR", Arrays.asList(
                condition("status", "EQ", "A"),
                condition("status", "EQ", "B")))));
        assertNotNull(AerospikeQuerySupport.toExpression(criteria(null, Arrays.asList(
                condition("active", "EQ", Boolean.TRUE)))));
    }

    @Test
    void supportsInOperator() {
        Map<String, Object> in = new LinkedHashMap<String, Object>();
        in.put("field", "status");
        in.put("op", "IN");
        in.put("values", Arrays.asList("A", "B", "C"));

        assertNotNull(AerospikeQuerySupport.toExpression(criteria("AND", Arrays.asList(in))));
    }

    @Test
    void supportsBetweenOperator() {
        Map<String, Object> between = new LinkedHashMap<String, Object>();
        between.put("field", "score");
        between.put("op", "BETWEEN");
        between.put("value", 10);
        between.put("value2", 20);

        assertNotNull(AerospikeQuerySupport.toExpression(criteria("AND", Arrays.asList(between))));
    }

    @Test
    void rejectsMissingConditions() {
        Map<String, Object> criteria = new LinkedHashMap<String, Object>();
        criteria.put("match", "AND");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AerospikeQuerySupport.toExpression(criteria));

        assertTrue(exception.getMessage().contains("conditions"));
    }

    @Test
    void rejectsInvalidMatch() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AerospikeQuerySupport.toExpression(criteria("XOR", Arrays.asList(
                        condition("city", "EQ", "Pune")))));

        assertTrue(exception.getMessage().contains("match"));
    }

    @Test
    void rejectsOrderingOperatorOnNonNumericValue() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AerospikeQuerySupport.toExpression(criteria("AND", Arrays.asList(
                        condition("city", "GT", "Pune")))));

        assertTrue(exception.getMessage().contains("numeric"));
    }

    @Test
    void rejectsUnknownOperator() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AerospikeQuerySupport.toExpression(criteria("AND", Arrays.asList(
                        condition("city", "LIKE", "Pun%")))));

        assertTrue(exception.getMessage().contains("unsupported operator"));
    }

    @Test
    void rejectsNullCriteria() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AerospikeQuerySupport.toExpression(null));

        assertTrue(exception.getMessage().contains("criteria must not be null"));
    }

    @Test
    void indexFilterIsNullWhenAbsent() {
        assertNull(AerospikeQuerySupport.indexFilter(criteria("AND", Arrays.asList(
                condition("city", "EQ", "Pune")))));
    }

    @Test
    void buildsEqualityIndexFilter() {
        Map<String, Object> index = new LinkedHashMap<String, Object>();
        index.put("field", "city");
        index.put("op", "EQ");
        index.put("value", "Pune");
        Map<String, Object> criteria = criteria("AND", Arrays.asList(condition("age", "GT", 18)));
        criteria.put("index", index);

        Filter filter = AerospikeQuerySupport.indexFilter(criteria);

        assertNotNull(filter);
    }

    @Test
    void buildsRangeIndexFilterAndRejectsInvertedBounds() {
        Map<String, Object> index = new LinkedHashMap<String, Object>();
        index.put("field", "age");
        index.put("op", "RANGE");
        index.put("begin", 18);
        index.put("end", 65);
        Map<String, Object> criteria = criteria("AND", Arrays.asList(condition("city", "EQ", "Pune")));
        criteria.put("index", index);

        assertNotNull(AerospikeQuerySupport.indexFilter(criteria));

        index.put("begin", 70);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AerospikeQuerySupport.indexFilter(criteria));
        assertTrue(exception.getMessage().contains("index.begin must be <= index.end"));
    }

    @Test
    void mapsIndexValueOverflowToValidationMessage() {
        Map<String, Object> index = new LinkedHashMap<String, Object>();
        index.put("field", "id");
        index.put("op", "EQ");
        index.put("value", BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE));
        Map<String, Object> criteria = criteria("AND", Arrays.asList(condition("city", "EQ", "Pune")));
        criteria.put("index", index);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AerospikeQuerySupport.indexFilter(criteria));

        assertEquals("index.value exceeds the supported 64-bit integer range", exception.getMessage());
    }
}
