package com.aistudio.micrhema
import org.junit.Assert.*
import org.junit.Test
class ForcedUpdatePolicyTest {
    @Test fun blocksOnlyVersionsBelowMinimum() {
        assertTrue(ForcedUpdatePolicy.required(true,"2.0.1.1243","2.0.1.99",false))
        assertFalse(ForcedUpdatePolicy.required(true,"2.0.1.1243","2.0.1.1243",false))
        assertFalse(ForcedUpdatePolicy.required(true,"2.0.1.1243","2.0.1.1244",false))
    }
    @Test fun disabledAdminAndInvalidPoliciesNeverBlock() {
        assertFalse(ForcedUpdatePolicy.required(false,"2.0.1.1243","2.0",false))
        assertFalse(ForcedUpdatePolicy.required(true,"2.0.1.1243","2.0",true))
        for(value in listOf("", "banana", "2..0", "2.0-beta", "9999999999999999999999999"))
            assertFalse(ForcedUpdatePolicy.required(true,value,"2.0",false))
    }
    @Test fun comparesNumericallyAndAcceptsReleasePrefix() {
        assertEquals(0,ForcedUpdatePolicy.compare("v2.0","2.0.0"))
        assertEquals(1,ForcedUpdatePolicy.compare("2.0.1.100","2.0.1.99"))
    }
}
