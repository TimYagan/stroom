/*
 * Copyright 2018 Crown Copyright
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
 *
 */

package stroom.refdata.offheapstore.serdes;

import org.assertj.core.api.Assertions;
import stroom.refdata.lmdb.serde.Serde;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

class AbstractSerdeTest {
    private static final int BYTE_BUFFER_SIZE = 10_000;

    <T> void doSerialisationDeserialisationTest(T object, Supplier<Serde<T>> serdeSupplier) {

        // use two serde instances to be sure ser and de-ser are independent
        final Serde<T> serde1 = serdeSupplier.get();
        final Serde<T> serde2 = serdeSupplier.get();

        // allocate a buffer size bigger than we need
        final ByteBuffer byteBuffer = ByteBuffer.allocate(BYTE_BUFFER_SIZE);

        serde1.serialize(byteBuffer, object);

        T object2 = serde2.deserialize(byteBuffer);

        Assertions.assertThat(object).isEqualTo(object2);
    }
}
