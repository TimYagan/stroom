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

package stroom.dashboard.client.main;

import stroom.dashboard.client.flexlayout.TabLayout;
import stroom.dashboard.client.main.ComponentRegistry.ComponentType;
import stroom.dashboard.shared.ComponentConfig;
import stroom.dashboard.shared.TabConfig;
import stroom.util.shared.HasDisplayValue;
import stroom.widget.tab.client.presenter.Layer;
import stroom.widget.tab.client.presenter.TabData;

public interface Component extends TabData, Layer, HasDisplayValue {
    Components getComponents();

    void setComponents(Components components);

    ComponentType getType();

    ComponentConfig getComponentData();

    /**
     * Link components together.
     */
    void link();

    /**
     * Called when a component is requested that it show it's settings.
     */
    void showSettings();

    /**
     * Set the associated tab layout for this component.
     *
     * @param tabLayout The tab layout to associate with this component.
     */
    void setTabLayout(TabLayout tabLayout);

    TabConfig getTabConfig();

    void setTabConfig(TabConfig tabConfig);

    void onRemove();

    String getId();

    void read(ComponentConfig componentConfig);

    void write(ComponentConfig componentConfig);
}
