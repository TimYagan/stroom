/*
 * Copyright 2017 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stroom.pipeline.legacy;

import stroom.entity.shared.DocumentEntity;
import stroom.importexport.shared.ExternalFile;
import stroom.entity.shared.SQLNameConstants;
import stroom.pipeline.shared.data.PipelineData;
import stroom.docref.DocRef;

import javax.xml.bind.annotation.XmlTransient;

/**
 * This entity is used to persist pipeline configuration.
 */
public class OldPipelineEntity extends DocumentEntity {
    public static final String TABLE_NAME = SQLNameConstants.PIPELINE;
    public static final String FOREIGN_KEY = FK_PREFIX + TABLE_NAME + ID_SUFFIX;
    public static final String PARENT_PIPELINE = SQLNameConstants.PARENT + SEP + SQLNameConstants.PIPELINE;
    public static final String DATA = SQLNameConstants.DATA;

    public static final String ENTITY_TYPE = "Pipeline";

    private static final long serialVersionUID = 4519634323788508083L;

    private String description;

    private String parentPipelineXML;
    private DocRef parentPipeline;

    private String data;
    private PipelineData pipelineData;

    public OldPipelineEntity() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getParentPipelineXML() {
        return parentPipelineXML;
    }

    public void setParentPipelineXML(final String parentPipelineXML) {
        this.parentPipelineXML = parentPipelineXML;
    }

    @XmlTransient
    public DocRef getParentPipeline() {
        return parentPipeline;
    }

    public void setParentPipeline(final DocRef parentPipeline) {
        this.parentPipeline = parentPipeline;
    }

    @ExternalFile
    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    @XmlTransient
    public PipelineData getPipelineData() {
        return pipelineData;
    }

    public void setPipelineData(final PipelineData pipelineData) {
        this.pipelineData = pipelineData;
    }

    /**
     * @return generic UI drop down value
     */
    @Override
    public String getDisplayValue() {
        return String.valueOf(getName());
    }

    @Override
    public final String getType() {
        return ENTITY_TYPE;
    }
}
