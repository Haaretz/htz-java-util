package com.cedarsoftware.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This Map uses very little memory (See CompactMap).  When the Map
 * has more than 'compactSize()' elements in it, the 'delegate' Map
 * is a LinkedHashMap.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class CompactLinkedMap<K, V> extends CompactMap<K, V>
{
    public CompactLinkedMap() { }
    public CompactLinkedMap(Map<K ,V> other) { super(other); }
    protected Map<K, V> getNewMap() { return new LinkedHashMap<>(compactSize() + 1); }
    protected boolean useCopyIterator() { return false; }
}
