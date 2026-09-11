package dev.hexin.dataagent.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlSafetyValidatorTest {

    private final SqlSafetyValidator validator = new SqlSafetyValidator();

    @Test
    void allowsSimpleSelect() {
        assertTrue(validator.validate("SELECT region, COUNT(*) FROM customers GROUP BY region").allowed());
    }

    @Test
    void allowsCteSelect() {
        assertTrue(validator.validate("WITH totals AS (SELECT customer_id, SUM(amount) total FROM orders GROUP BY customer_id) SELECT * FROM totals").allowed());
    }

    @Test
    void rejectsDelete() {
        var result = validator.validate("DELETE FROM orders");
        assertFalse(result.allowed());
    }

    @Test
    void rejectsStackedStatements() {
        var result = validator.validate("SELECT * FROM customers; DROP TABLE customers");
        assertFalse(result.allowed());
    }

    @Test
    void rejectsWriteInsideCte() {
        var result = validator.validate("WITH removed AS (DELETE FROM orders RETURNING *) SELECT * FROM removed");
        assertFalse(result.allowed());
    }
}
