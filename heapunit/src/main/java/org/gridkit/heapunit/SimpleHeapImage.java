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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.gridkit.jvmtool.heapdump.HeapWalker;
import org.gridkit.jvmtool.heapdump.RefSet;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

public class SimpleHeapImage implements HeapImage {

    private static final Map<String, Class<?>> PTYPES = new HashMap<String, Class<?>>();
    static {
        PTYPES.put("void", void.class);
        PTYPES.put("byte", byte.class);
        PTYPES.put("short", short.class);
        PTYPES.put("char", char.class);
        PTYPES.put("int", int.class);
        PTYPES.put("long", long.class);
        PTYPES.put("float", float.class);
        PTYPES.put("double", double.class);
    }
    
    private Objenesis objeneis = new ObjenesisStd();
    private final Map<Long, Object> rehydrationCache = new HashMap<Long, Object>();
    private final Map<String, Set<JavaClass>> inheritanceCache = new HashMap<String, Set<JavaClass>>();
    private final Map<String, Rehydrator> instantiators = new HashMap<String, Rehydrator>();
    
    private final Heap heap;

    public SimpleHeapImage(Heap heap) {
        this.heap = heap;
    }

    @Override
    public long instanceCount(Class<?> c) {
        long cc = 0;
        Set<JavaClass> types = getSubclasses(c);
        for(Instance i: heap.getAllInstances()) {
            if (types.contains(i.getJavaClass())) {
                cc++;
            }
        }
        return cc;
    }

    @Override
    public long instanceCount(String selector) {
        long cc = 0;
        for(@SuppressWarnings("unused") HeapInstance i: iterate(selector)) {
            cc++;
        }
        return cc;
    }
    
    @Override
    public HeapInstance instance(String selector) {
        Iterator<HeapInstance> it = iterate(selector).iterator();
        return it.hasNext() ? it.next() : null;
    }

    @Override
    public Iterable<HeapInstance> instances(String selector) {
        return iterate(selector);
    }

    @Override
    public Iterable<HeapInstance> instances(Class<?> c) {
        final Set<JavaClass> types = getSubclasses(c);
        return new Iterable<HeapInstance>() {
            
            @Override
            public Iterator<HeapInstance> iterator() {
                final Iterator<Instance> it = heap.getAllInstances().iterator();
                return new AbsIt<HeapInstance>() {

                    @Override
                    protected HeapInstance seek() {
                        while(it.hasNext()) {
                            Instance i = it.next();
                            if (types == null || types.contains(i.getJavaClass())) {
                                return new HeapInstance(SimpleHeapImage.this, i);
                            }
                        }
                        return null;
                    }
                };
            }
        };
    }

    private Iterable<HeapInstance> iterate(final String selector) {
        final Set<JavaClass> types = filterTypes(selector);       
        
        if (types != null && types.isEmpty()) {
            return Collections.emptyList();
        }
        
        return new Iterable<HeapInstance>() {
            
            @Override
            public Iterator<HeapInstance> iterator() {
                
                final RefSet visited = new RefSet();
                final Iterator<Instance> instances = heap.getAllInstances().iterator();
                
                return new AbsIt<HeapInstance>() {

                    Iterator<Instance> walker = null;
                    
                    @Override
                    protected HeapInstance seek() {
                        while(true) {
                            if (walker == null) {
                                if (instances.hasNext()) {
                                    Instance i = instances.next();
                                    if (types == null || types.contains(i.getJavaClass())) {
                                        walker = HeapWalker.walk(i, selector).iterator(); 
                                    }
                                }
                                else {
                                    return null;
                                }
                            }
                            else if (walker.hasNext()) {
                                Instance i = walker.next();
                                long a = i.getInstanceId();
                                if (!visited.get(a)) {
                                    visited.set(a, true);
                                    return new HeapInstance(SimpleHeapImage.this, i);
                                }
                            }
                            else {
                                walker = null;
                            }                            
                        }
                    }
                };
            }
        };
    }

    protected Set<JavaClass> filterTypes(String selector) {
        if (selector.startsWith("(")) {
            int n = selector.indexOf(")");
            String tf = selector.substring(1, n);
            return HeapWalker.filterTypes(tf, heap.getAllClasses());
        }
        else {
            return null;
        }
    }

    private Set<JavaClass> getSubclasses(Class<?> c) {
        String name = c.getName();
        if (inheritanceCache.containsKey(name)) {
            return inheritanceCache.get(name);
        }
        else {
            Set<JavaClass> result = new HashSet<JavaClass>();
            
            for(JavaClass jc: heap.getAllClasses()) {
                if (isInherited(jc, c)) {
                    result.add(jc);
                }
            }
            
            inheritanceCache.put(name, result);
            return result;
        }
    }

    private boolean isInherited(JavaClass jc, Class<?> c) {
        String dcname = jc.getName();
        String rcname = className(c);
        if (dcname.equals(rcname)) {
            return true;
        }
        else {
            Class<?> x = classForName(dcname);
            if (x != null && c.isAssignableFrom(x)) {
                return true;
            }
        }
        return false;
    }

    private Class<?> classForName(String dcname) {
        if (dcname.endsWith("[]")) {
            Class<?> x = classForName(dcname.substring(0, dcname.length() - 2));
            if (x != null) {
                x = Array.newInstance(x, 0).getClass();
            }
            return x;
        }
        else {
            if (PTYPES.containsKey(dcname)) {
                return PTYPES.get(dcname);
            }
            try {
                return Class.forName(dcname);
            }
            catch(Exception e) {
                // ignore
            }            
            catch(NoClassDefFoundError e) {
                // ignore
            }
            return null;
        }
    }

    private String className(Class<?> c) {
        if (c.isArray()) {
            return className(c.getComponentType()) + "[]";
        }
        else {
            return c.getName();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T rehydrate(Instance instance) {
        long id = instance.getInstanceId();
        if (rehydrationCache.containsKey(id)) {
            return (T) rehydrationCache.get(id);
        }
        else {
            JavaClass jc = instance.getJavaClass();
            Rehydrator rh = rehydrator(jc);
            if (rh != null) {
                try {
                    return (T) rh.rehydrateInstance(instance);
                } catch (Exception e) {
                } catch (InstantiationError e) {
                }
                return (T) rehydrationCache.get(id);
            }
            else {
                rehydrationCache.put(id, null);
                return null;
            }
        }
    }

    private Rehydrator rehydrator(JavaClass jc) {
        if (instantiators.containsKey(jc.getName())) {
            return instantiators.get(jc.getName());
        }
        else {
            Rehydrator rh = null;
            Class<?> c = classForName(jc.getName());
            if (c == String.class || (c.isArray() && c.getComponentType().isPrimitive())) {
                rh = new SimpleRehydrator();
            }
            else if (c.isArray()) {
                rh = new ArrayRehydrator(c);
            }
            else {
                try {
                    rh = new ObjenesisRehydrator(c, jc);
                }
                catch(Exception e) {
                    // ignore
                }
            }
            instantiators.put(jc.getName(), rh);
            return rh;
        }
    }

    @Override
    public <T> T rehydrateFirst(String selector) {
    	HeapInstance hp = instance(selector);
        return hp == null ? null : hp.<T>rehydrate();
    }

    @Override
    public <T> Iterable<T> rehydrate(final String selector) {        
        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                final Iterator<HeapInstance> it = instances(selector).iterator();
                return new AbsIt<T>() {

                    @Override
                    @SuppressWarnings("unchecked")
                    protected T seek() {
                        return it.hasNext() ? (T)it.next().rehydrate() : null;
                    }
                };
            }
        };
    }

    @Override
    public <T> Iterable<T> rehydrate(final Class<T> c) {
        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                final Iterator<HeapInstance> it = instances(c).iterator();
                return new AbsIt<T>() {

                    @Override
                    protected T seek() {
                        return it.hasNext() ? it.next().<T>rehydrate() : null;
                    }
                };
            }
        };
    }

    @Override
    public Heap getHeap() {
        return heap;
    }
    
    private static abstract class AbsIt<T> implements Iterator<T> {

        T next;
        
        protected abstract T seek();
        
        @Override
        public boolean hasNext() {
            if (next == null) {
                next = seek();
            }
            return next != null;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T r = next;
            next = null;
            return r;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();            
        }
    }
    
    private abstract class Rehydrator {
        
        public abstract Object rehydrateInstance(Instance i) throws Exception;
        
    }
    
    private class SimpleRehydrator extends Rehydrator {

        @Override
        public Object rehydrateInstance(Instance i) {
            Object v = HeapWalker.valueOf(i);
            rehydrationCache.put(i.getInstanceId(), v);
            return v;
        }
    }

    private class ArrayRehydrator extends Rehydrator {
        
        Class<?> ctype;
        
        public ArrayRehydrator(Class<?> type) {
            this.ctype = type.getComponentType();
        }

        @Override
        public Object rehydrateInstance(Instance ii) {
            ObjectArrayInstance oai = (ObjectArrayInstance) ii;
            int len = oai.getLength();
            Object a = Array.newInstance(ctype, len);
            rehydrationCache.put(ii.getInstanceId(), a);
            List<Instance> values = oai.getValues();
            for(int i = 0; i != len; ++i) {
                Array.set(a, i, rehydrate(values.get(i)));
            }
            return a;
        }
    }
    
    private class ObjenesisRehydrator extends Rehydrator {

        private ObjectInstantiator<?> instantiator;
        private Map<String, Field> fields = new LinkedHashMap<String, Field>();
        
        public ObjenesisRehydrator(Class<?> type, JavaClass jc) {
            instantiator = objeneis.getInstantiatorOf(type);
            initFields(type, jc);
        }
        
        private void initFields(Class<?> c, JavaClass jc) {

            if (jc.getSuperClass() != null) {
                initFields(c.getSuperclass(), jc.getSuperClass());
            }
            
            for(org.netbeans.lib.profiler.heap.Field jf: jc.getFields()) {
                if (!jf.isStatic()) {
                    try {
                        Field rf = c.getDeclaredField(jf.getName());
                        rf.setAccessible(true);
                        String name = jf.getDeclaringClass().getName() + "#" + jf.getName();
                        fields.put(name, rf);
                    }
                    catch(Exception e) {
                        // ignore
                    }
                }
            }
        }

        @Override
        public Object rehydrateInstance(Instance i) throws Exception {
                        
            Object v = instantiator.newInstance();
            
            rehydrationCache.put(i.getInstanceId(), v);
            
            for(FieldValue fv: i.getFieldValues()) {
                try {
                    String name = fv.getField().getDeclaringClass().getName() + "#" + fv.getField().getName();
                    Field f = fields.get(name);
                    if (f != null) {
                        if (f.getType().isPrimitive()) {
                            setPrimitive(v, f, fv.getValue());
                        }
                        else {
                            f.set(v, rehydrate(((ObjectFieldValue)fv).getInstance()));
                        }
                    }
                }
                catch(Exception e) {
                    // ignore
                }
            }
            
            return v;
        }

        private void setPrimitive(Object o, Field f, String value) throws IllegalArgumentException, IllegalAccessException {
            if (f.getType() == boolean.class) {
                f.set(o, Boolean.valueOf(value));
            }
            else if (f.getType() == byte.class) {
                f.set(o, Byte.valueOf(value));
            }
            else if (f.getType() == char.class) {
                f.set(o, value.charAt(0));
            }
            else if (f.getType() == short.class) {
                f.set(o, Short.valueOf(value));
            }
            else if (f.getType() == int.class) {
                f.set(o, Integer.valueOf(value));
            }
            else if (f.getType() == long.class) {
                f.set(o, Long.valueOf(value));
            }
            else if (f.getType() == float.class) {
                f.set(o, Float.valueOf(value));
            }
            else if (f.getType() == double.class) {
                f.set(o, Double.valueOf(value));
            }
        }
    }       
}
