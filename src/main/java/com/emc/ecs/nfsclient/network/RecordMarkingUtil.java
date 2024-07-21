/**
 * Copyright 2016-2018 Dell Inc. or its subsidiaries. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 * <p>
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.emc.ecs.nfsclient.network;

import com.emc.ecs.nfsclient.rpc.Xdr;

import org.apache.commons.lang3.NotImplementedException;

/**
 * RFC1831: RECORD MARKING STANDARD When RPC messages are passed on top of a
 * byte stream transport protocol (like TCP), it is necessary to delimit one
 * message from another in order to detect and possibly recover from protocol
 * errors. This is called record marking (RM).
 *
 * @author seibed
 */
public class RecordMarkingUtil {

    /**
     * Special constant used for the last fragment size. This is a number bigger
     * than the largest unsigned int, so it cannot be a real fragment size..
     */
    private static final int LAST_FRAG = 0x80000000;

    /**
     * The size mask is applied to the long fragment size to get the real
     * unsigned int size. When this mask is applied to <code>LAST_FRAG</code>,
     * the resulting real fragment size is 0. For all real fragment sizes, this
     * mask has no effect.
     */
    private static final int SIZE_MASK = 0x7fffffff;


    /**
     * Remove record marking from the byte array and convert to an Xdr.
     *
     * @param bytes The byte array.
     * @return The Xdr.
     */
    static Xdr removeRecordMarking(byte[] bytes) {
        Xdr toReturn = new Xdr(bytes.length);
        Xdr input = new Xdr(bytes);

        long fragSize;
        boolean lastFragment = false;

        input.setOffset(0);
        int inputOff = input.getOffset();

        while (!lastFragment) {

            fragSize = input.getUnsignedInt();
            lastFragment = isLastFragment(fragSize);
            fragSize = maskFragmentSize(fragSize);

            toReturn.putBytes(input.getBuffer(), input.getOffset(), (int) fragSize);
            inputOff += fragSize;
            input.setOffset(inputOff);
        }

        // get xid
        int off = toReturn.getOffset();
        toReturn.setOffset(0);
        int xid = toReturn.getInt();
        toReturn.setXid(xid);
        toReturn.setOffset(off);

        return toReturn;
    }

    /**
     * @param fragmentSize
     *            A long that contains either the int number of bytes in the
     *            fragment, or a special value if this is the last fragment.
     * @return <code>true</code> if it is, <code>false</code> if it is not.
     */
    static boolean isLastFragment(long fragmentSize) {
        return ((fragmentSize & LAST_FRAG) != 0);
    }

    /**
     * @param fragmentSize
     *            A long that contains either the int number of bytes in the
     *            fragment, or a special value if this is the last fragment.
     * @return The masked fragment size, which is the real size of the fragment.
     */
    static long maskFragmentSize(long fragmentSize) {
        return fragmentSize & SIZE_MASK;
    }

    /**
     * Should never be used.
     */
    private RecordMarkingUtil() {
        throw new NotImplementedException("No class instances should be needed.");
    }

}
