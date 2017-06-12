/**
 * Copyright 2016 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.gridkit.heapunit;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class HeapUnitTest {

    @Test
    public void test() throws IOException {
        
        @SuppressWarnings("unused")
        TestInstance tt = new TestInstance();
        
        HeapImage hi = HeapUnit.captureHeap();
        
        System.out.println("String count: " + hi.instanceCount(String.class));
        Assert.assertTrue(hi.instanceCount(String.class) > 0);

        System.out.println("List count: " + hi.instanceCount(List.class));
        Assert.assertTrue(hi.instanceCount(List.class) > 0);

        System.out.println("TestInstance count: " + hi.instanceCount(TestInstance.class));
        assertEquals(1, hi.instanceCount(TestInstance.class));

        System.out.println("(**$TestInstance) count: " + hi.instanceCount("(**$TestInstance)"));
        assertEquals(1, hi.instanceCount("(**$TestInstance)"));
        
        System.out.println("char[] count: " + hi.instanceCount(char[].class));
        Assert.assertTrue(hi.instanceCount(char[].class) > 0);

        System.out.println("Object[] count: " + hi.instanceCount(Object[].class));
        Assert.assertTrue(hi.instanceCount(Object[].class) > 0);
        
        TestInstance t2 = hi.rehydrateFirst("(**$TestInstance)");
        
        System.out.println(t2);
        
        HeapInstance ti = hi.instance("(**$TestInstance)");
        
        assertEquals(true, ti.value("bval"));
        assertEquals(10, ti.value("ival"));
        assertEquals(1.1d, ti.value("dval"));
    }
    
    
    static class TestInstance {
        
    	boolean bval = true;
    	int ival = 10;
    	double dval = 1.1d;
        byte[] array = new byte[0];
        String[] stringArray = new String[]{};
        List<String> list = Arrays.asList("1", "2", "3");
        
    }
}
