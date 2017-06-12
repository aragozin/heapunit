package org.gridkit.heapunit;

import org.gridkit.jvmtool.heapdump.HeapWalker;
import org.netbeans.lib.profiler.heap.Instance;

/**
 * This is reference to Java object in heap.
 * <p>
 * {@link HeapImage} wraps {@link Instance} and add some extra utility.
 * 
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
public class HeapInstance {

	private final SimpleHeapImage heap;
	private final Instance instance;

	public HeapInstance(SimpleHeapImage heap, Instance instance) {
		this.heap = heap;
		this.instance = instance;
	}

	/**
	 * Retries first element matched by path expression.
	 * <p>
	 * This method do not do rehydration automatically {@link String}
	 * <li>{@link String}</li>
	 * <li>primitive types</li>
	 * <li>box types</li>
	 * <li>primitive arrays</li>
	 * would be converted to Java objects, other values would be returned as {@link HeapInstance}.
	 * <p>
	 * This method is convenient to access field values of instance.
	 */
	@SuppressWarnings("unchecked")
	public <T> T value(String path) {
		Object v = HeapWalker.valueOf(instance, path);
		if (v instanceof Instance) {
			return (T) new HeapInstance(heap, (Instance) v);
		}
		else {
			return (T) v;
		}
	}
	
	public <T> T rehydrate() {
		return heap.rehydrate(instance);
	}
	
	@Override
	public String toString() {
		return instance.getJavaClass().getName() + "@" + instance.getInstanceId();
	}
}
