/*
 * Copyright 2017 Crown Copyright
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

package stroom.search.solr.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import stroom.docstore.shared.Doc;
import stroom.query.api.v2.ExpressionOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonInclude(Include.NON_DEFAULT)
@JsonPropertyOrder({"type", "uuid", "name", "version", "createTime", "updateTime", "createUser", "updateUser", "description", "collection", "connection", "indexBatchSize", "fields", "state", "retentionExpression"})
public class SolrIndexDoc extends Doc {
    public static final String DOCUMENT_TYPE = "SolrIndex";

    private static final long serialVersionUID = 2648729644398564919L;

    private String description;
    private String collection;
    private SolrConnectionConfig solrConnectionConfig = new SolrConnectionConfig();

    private List<SolrIndexField> fields;
    private List<SolrIndexField> deletedFields;
    private SolrSynchState solrSynchState;

    private ExpressionOperator retentionExpression;

    public SolrIndexDoc() {
        fields = new ArrayList<>();
        // Always add standard id fields for now.
        fields.add(SolrIndexField.createIdField(SolrIndexConstants.STREAM_ID));
        fields.add(SolrIndexField.createIdField(SolrIndexConstants.EVENT_ID));
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getCollection() {
        if (collection == null || collection.trim().length() == 0) {
            return null;
        }
        return collection;
    }

    public void setCollection(final String collection) {
        this.collection = collection;
    }

    @JsonProperty("connection")
    public SolrConnectionConfig getSolrConnectionConfig() {
        return solrConnectionConfig;
    }

    @JsonProperty("connection")
    public void setSolrConnectionConfig(final SolrConnectionConfig solrConnectionConfig) {
        this.solrConnectionConfig = solrConnectionConfig;
    }

    @JsonProperty("fields")
    public List<SolrIndexField> getFields() {
        return fields;
    }

    @JsonProperty("fields")
    public void setFields(final List<SolrIndexField> fields) {
        this.fields = fields;
    }

    @JsonIgnore
    public List<SolrIndexField> getDeletedFields() {
        return deletedFields;
    }

    @JsonIgnore
    public void setDeletedFields(final List<SolrIndexField> deletedFields) {
        this.deletedFields = deletedFields;
    }

    @JsonProperty("state")
    public SolrSynchState getSolrSynchState() {
        return solrSynchState;
    }

    @JsonProperty("state")
    public void setSolrSynchState(final SolrSynchState solrSynchState) {
        this.solrSynchState = solrSynchState;
    }

    @JsonProperty("retentionExpression")
    public ExpressionOperator getRetentionExpression() {
        return retentionExpression;
    }

    @JsonProperty("retentionExpression")
    public void setRetentionExpression(final ExpressionOperator retentionExpression) {
        this.retentionExpression = retentionExpression;
    }

    @JsonIgnore
    @Override
    public final String getType() {
        return DOCUMENT_TYPE;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof SolrIndexDoc)) return false;
        if (!super.equals(o)) return false;
        final SolrIndexDoc solrIndexDoc = (SolrIndexDoc) o;
        return Objects.equals(description, solrIndexDoc.description) &&
                Objects.equals(collection, solrIndexDoc.collection) &&
                Objects.equals(solrConnectionConfig, solrIndexDoc.solrConnectionConfig) &&
                Objects.equals(fields, solrIndexDoc.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), description, collection, solrConnectionConfig, fields);
    }

    @Override
    public String toString() {
        return "SolrIndex{" +
                "description='" + description + '\'' +
                ", collection='" + collection + '\'' +
                ", solrConnectionConfig=" + solrConnectionConfig +
                ", fields=" + fields +
                '}';
    }
}
