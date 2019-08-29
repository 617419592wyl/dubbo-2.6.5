/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.utils;

import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//用ConcurrentHashMap包装的线程安全的set
public class ConcurrentHashSet<E> extends AbstractSet<E> implements Set<E>, java.io.Serializable {

    private static final long serialVersionUID = -8672117787651310382L;

    private static final Object PRESENT = new Object();

    private final ConcurrentMap<E, Object> map;

    public ConcurrentHashSet() {
        map = new ConcurrentHashMap<E, Object>();
    }

    public ConcurrentHashSet(int initialCapacity) {
        map = new ConcurrentHashMap<E, Object>(initialCapacity);
    }

    /**
     * Returns an iterator over the elements in this set. The elements are
     * returned in no particular order.在这个集合的元素上返回一个迭代器。元素没有特定的顺序返回。
     *
     * @return an Iterator over the elements in this set
     * @see ConcurrentModificationException
     */
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    /**
     * Returns the number of elements in this set (its cardinality).返回该集合中的元素数量(其基数)。
     *
     * @return the number of elements in this set (its cardinality)
     */
    @Override
    public int size() {
        return map.size();
    }

    /**
     * Returns <tt>true</tt> if this set contains no elements.如果该集合不包含元素，则返回true。
     *
     * @return <tt>true</tt> if this set contains no elements
     */
    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified element. More
     * formally, returns <tt>true</tt> if and only if this set contains an
     * element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this set is to be tested
     * @return <tt>true</tt> if this set contains the specified element
     * 如果该集合包含指定元素，则返回true。更正式地说，当且仅当这个集合包含一个元素e (o==null ?e = = null: o.equals (e))。
     */
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * Adds the specified element to this set if it is not already present. More
     * formally, adds the specified element <tt>e</tt> to this set if this set
     * contains no element <tt>e2</tt> such that
     * <tt>(e==null&nbsp;?&nbsp;e2==null&nbsp;:&nbsp;e.equals(e2))</tt>. If this
     * set already contains the element, the call leaves the set unchanged and
     * returns <tt>false</tt>.
     *
     * @param e element to be added to this set
     * @return <tt>true</tt> if this set did not already contain the specified
     * element
     * 如果指定元素还未出现，则将其添加到此集合。更正式地说，如果这个集合不包含e2元素，则将指定的元素e添加到这个集合中(e==null ?e2 = = null: e.equals (e2))。如果该集合已经包含元素，调用将保持该集合不变并返回false。
     */
    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    /**
     * Removes the specified element from this set if it is present. More
     * formally, removes an element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>, if this
     * set contains such an element. Returns <tt>true</tt> if this set contained
     * the element (or equivalently, if this set changed as a result of the
     * call). (This set will not contain the element once the call returns.)
     *
     * @param o object to be removed from this set, if present
     * @return <tt>true</tt> if the set contained the specified element
     * 如果指定元素存在，则从该集合中移除该元素。更正式地说，删除一个元素e，使得(o==null ?)==null: 0 .equals(e))，如果这个集合包含这样一个元素。如果该集合包含元素，则返回true(或者，如果该集合因调用而更改，则返回true)。(一旦调用返回，该集合将不包含元素。)
     */
    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    /**
     * Removes all of the elements from this set. The set will be empty after
     * this call returns.从该集合中移除所有元素。该集合在此调用返回后为空。
     */
    @Override
    public void clear() {
        map.clear();
    }

}
