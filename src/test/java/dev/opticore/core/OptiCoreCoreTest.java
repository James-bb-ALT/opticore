package dev.opticore.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OptiCoreCoreTest {
    @Test
    void stringInternPoolDeduplicatesStrings() {
        String a = new String("minecraft");
        String b = new String("minecraft");
        assertNotSame(a, b);
        String internedA = StringInternPool.intern(a);
        String internedB = StringInternPool.intern(b);
        assertSame(internedA, internedB);
        assertTrue(OptiCoreMetrics.INTERN_HITS.sum() >= 1);
    }

    @Test
    void stringInternPoolSkipsNullEmptyAndLongStrings() {
        assertNull(StringInternPool.intern(null));
        assertEquals("", StringInternPool.intern(""));
        String longStr = "a".repeat(200);
        assertSame(longStr, StringInternPool.intern(longStr));
    }
}
