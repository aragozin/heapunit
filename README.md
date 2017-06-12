HeapUnit
=========

Test library to introspecting your Java heap via heap dumps.

HeapUnit can

 - capture heap dump of own JVM
 - scan content of dump
 - reconstruct Java objects from heap dump

Example
---------
Code snippet below dumps TCP Socket instances found in heap

	HeapImage hi = HeapUnit.captureHeap();
		
	for(HeapInstance i: hi.instances(SocketImpl.class)) {
		// fd field in SocketImpl class is nullified when socket gets closed
		boolean open = i.value("fd") != null;
		System.out.println(i.rehydrate() + (open ? " - open" : " - closed"));
	}

Full source is [here](heapunit/src/test/java/org/gridkit/heapunit/DumpSocketsExample.java)
                      
Maven artifact
---------

HeapUnit and dependencies is available in Maven Central Repo

	<dependency>
	    <groupId>org.gridkit.heapunit</groupId>
	    <artifactId>heapunit</artifactId>
	    <version>0.1</version>
	</dependency>
