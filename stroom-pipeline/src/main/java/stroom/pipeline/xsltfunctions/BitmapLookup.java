/*
 * Copyright 2016 Crown Copyright
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

package stroom.pipeline.xsltfunctions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.EmptyAtomicSequence;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import stroom.pipeline.state.StreamHolder;
import stroom.refdata.LookupIdentifier;
import stroom.refdata.ReferenceData;
import stroom.refdata.ReferenceDataResult;
import stroom.refdata.offheapstore.RefDataStoreProvider;
import stroom.refdata.offheapstore.RefDataValueProxy;
import stroom.refdata.offheapstore.RefDataValueProxyConsumer;
import stroom.util.shared.Severity;

import javax.inject.Inject;

class BitmapLookup extends AbstractLookup {
    @Inject
    BitmapLookup(final ReferenceData referenceData,
                 final RefDataStoreProvider refDataStoreProvider,
                 final StreamHolder streamHolder,
                 final RefDataValueProxyConsumer.Factory consumerFactory) {
        super(referenceData, refDataStoreProvider.get(), streamHolder, consumerFactory);
    }

    @Override
    protected Sequence doLookup(final XPathContext context,
//                                final String map,
//                                final String key,
//                                final long eventTime,
                                final boolean ignoreWarnings,
                                final boolean trace,
                                final LookupIdentifier lookupIdentifier) throws XPathException {
        SequenceMaker sequenceMaker = null;

        String key = lookupIdentifier.getKey();
        int val;
        try {
            if (key.startsWith("0x")) {
                val = Integer.valueOf(key.substring(2), 16);
            } else {
                val = Integer.valueOf(key);
            }
        } catch (final NumberFormatException e) {
            throw new NumberFormatException("unable to parse number '" + key + "'");
        }

        final int[] bits = Bitmap.getBits(val);
        StringBuilder failedBits = null;

        if (bits.length > 0) {
            for (final int bit : bits) {
                final String k = String.valueOf(bit);
                final LookupIdentifier bitIdentifier = lookupIdentifier.cloneWithNewKey(k);
                final ReferenceDataResult result = getReferenceData(bitIdentifier);
                final RefDataValueProxy refDataValueProxy = result.getRefDataValueProxy();

//                final EventListProxyConsumer eventListConsumer = EventListProxyConsumerFactory.getConsumer(
//                        eventListProxy,
//                        context);

//                final Sequence sequence = eventListConsumer.map(eventListProxy);

                if (refDataValueProxy != null) {
                    if (sequenceMaker == null) {
                        sequenceMaker = new SequenceMaker(context, getRefDataStore(), getConsumerFactory());
                        sequenceMaker.open();
                    }
                    sequenceMaker.consume(refDataValueProxy);

                    if (trace) {
                        outputInfo(Severity.INFO, "Lookup success ", lookupIdentifier, trace, result, context);
                    }
                } else if (!ignoreWarnings) {
                    if (trace) {
                        outputInfo(Severity.WARNING, "Lookup failed ", lookupIdentifier, trace, result, context);
                    }

                    if (failedBits == null) {
                        failedBits = new StringBuilder();
                    }
                    failedBits.append(k);
                    failedBits.append(",");
                }
            }

            if (failedBits != null) {
                failedBits.setLength(failedBits.length() - 1);
                failedBits.insert(0, "{");
                failedBits.append("}");

                // Create the message.
                final StringBuilder sb = new StringBuilder();
                sb.append("Lookup failed ");
                lookupIdentifier.append(sb);
                outputWarning(context, sb, null);
            }

            if (sequenceMaker != null) {
                sequenceMaker.close();
                return sequenceMaker.toSequence();
            }
        }

        return EmptyAtomicSequence.getInstance();
    }
}
