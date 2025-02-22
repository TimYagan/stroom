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

package stroom.search.solr.search;

import stroom.query.common.v2.Payload;
import stroom.search.api.EventRefs;

public class EventRefsPayload implements Payload {
    private static final long serialVersionUID = 5271438218782010968L;

    private EventRefs eventRefs;

    public EventRefsPayload() {
    }

    EventRefsPayload(final EventRefs eventRefs) {
        this.eventRefs = eventRefs;
    }

    EventRefs getEventRefs() {
        return eventRefs;
    }
}
