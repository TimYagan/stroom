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

package stroom.widget.valuespinner.client;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HasHandlers;

public class SpinnerEvent extends GwtEvent<SpinnerEvent.Handler> {
    private static Type<Handler> TYPE;
    private final long value;

    private SpinnerEvent(final long value) {
        this.value = value;
    }

    public static Type<Handler> getType() {
        if (TYPE == null) {
            TYPE = new GwtEvent.Type<>();
        }
        return TYPE;
    }

    public static void fire(final HasHandlers source, final long value) {
        source.fireEvent(new SpinnerEvent(value));
    }

    @Override
    public Type<Handler> getAssociatedType() {
        return getType();
    }

    @Override
    protected void dispatch(final Handler handler) {
        handler.onChange(this);
    }

    public long getValue() {
        return value;
    }

    public interface Handler extends EventHandler {
        void onChange(SpinnerEvent event);
    }
}
