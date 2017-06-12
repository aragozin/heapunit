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

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;

public interface HeapImage {

    public long instanceCount(Class<?> c);

    public long instanceCount(String selector);
    
    public HeapInstance instance(String selector);

    public Iterable<HeapInstance> instances(String selector);

    public Iterable<HeapInstance> instances(Class<?> c);
    
    public <T> T rehydrate(Instance instance);

    public <T> T rehydrateFirst(String selector);

    public <T> Iterable<T> rehydrate(String selector);

    public <T> Iterable<T> rehydrate(Class<T> c);
    
    public Heap getHeap();

}
