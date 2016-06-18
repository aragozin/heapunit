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

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.gridkit.lab.jvm.attach.HeapDumper;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;

class HeapDumpProcuder {

    private static int PID;
    static {
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        PID = Integer.valueOf(pid.substring(0, pid.indexOf('@')));
    }


    public static Heap makeHeapDump(String path, long timeoutMs) throws IOException {
        File file = new File(path);
        if (file.getParentFile() != null && file.getParentFile().mkdirs());
        if (file.exists() && file.delete());
        System.out.println("Generating heap dump: " + path);
        System.out.println(HeapDumper.dumpLive(PID, path, timeoutMs));
        
        Heap heap = HeapFactory.createFastHeap(file); 
        
        return heap;
    }    
}

