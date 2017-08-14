/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.phoenix.hbase.index;

import org.apache.hadoop.hbase.regionserver.IncreasingToUpperBoundRegionSplitPolicy;
import org.apache.hadoop.hbase.regionserver.Store;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.phoenix.query.QueryConstants;

import java.util.Map;

/**
 * Split policy for local indexed tables to select split key from non local index column families
 * always.
 */
public class IndexRegionSplitPolicy extends IncreasingToUpperBoundRegionSplitPolicy {

//    @Override
    protected boolean skipStoreFileRangeCheck(String familyName) {
        if (familyName.startsWith(QueryConstants.LOCAL_INDEX_COLUMN_FAMILY_PREFIX)) {
            return true;
        }
        return false;
    }

    @Override
    protected byte[] getSplitPoint() {
        byte[] oldSplitPoint = super.getSplitPoint();
        if (oldSplitPoint == null) return null;
        Map<byte[], Store> stores = region.getStores();
        byte[] splitPointFromLargestStore = null;
        long largestStoreSize = 0;
        boolean isLocalIndexKey = false;
        for (Store s : stores.values()) {
            if (s.getFamily().getNameAsString()
                    .startsWith(QueryConstants.LOCAL_INDEX_COLUMN_FAMILY_PREFIX)) {
                byte[] splitPoint = s.getSplitPoint();
                if (oldSplitPoint != null && splitPoint != null
                        && Bytes.compareTo(oldSplitPoint, splitPoint) == 0) {
                    isLocalIndexKey = true;
                }
            }
        }
        if (!isLocalIndexKey) return oldSplitPoint;

        for (Store s : stores.values()) {
            if (!s.getFamily().getNameAsString()
                    .startsWith(QueryConstants.LOCAL_INDEX_COLUMN_FAMILY_PREFIX)) {
                byte[] splitPoint = s.getSplitPoint();
                long storeSize = s.getSize();
                if (splitPoint != null && largestStoreSize < storeSize) {
                    splitPointFromLargestStore = splitPoint;
                    largestStoreSize = storeSize;
                }
            }
        }
        return splitPointFromLargestStore;
    }
}
