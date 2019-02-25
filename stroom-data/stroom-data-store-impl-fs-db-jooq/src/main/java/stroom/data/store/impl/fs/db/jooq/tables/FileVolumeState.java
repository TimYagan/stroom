/*
 * This file is generated by jOOQ.
 */
package stroom.data.store.impl.fs.db.jooq.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import stroom.data.store.impl.fs.db.jooq.Indexes;
import stroom.data.store.impl.fs.db.jooq.Keys;
import stroom.data.store.impl.fs.db.jooq.Stroom;
import stroom.data.store.impl.fs.db.jooq.tables.records.FileVolumeStateRecord;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.9"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class FileVolumeState extends TableImpl<FileVolumeStateRecord> {

    private static final long serialVersionUID = -735934543;

    /**
     * The reference instance of <code>stroom.file_volume_state</code>
     */
    public static final FileVolumeState FILE_VOLUME_STATE = new FileVolumeState();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<FileVolumeStateRecord> getRecordType() {
        return FileVolumeStateRecord.class;
    }

    /**
     * The column <code>stroom.file_volume_state.id</code>.
     */
    public final TableField<FileVolumeStateRecord, Integer> ID = createField("id", org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.file_volume_state.version</code>.
     */
    public final TableField<FileVolumeStateRecord, Integer> VERSION = createField("version", org.jooq.impl.SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>stroom.file_volume_state.bytes_used</code>.
     */
    public final TableField<FileVolumeStateRecord, Long> BYTES_USED = createField("bytes_used", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>stroom.file_volume_state.bytes_free</code>.
     */
    public final TableField<FileVolumeStateRecord, Long> BYTES_FREE = createField("bytes_free", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>stroom.file_volume_state.bytes_total</code>.
     */
    public final TableField<FileVolumeStateRecord, Long> BYTES_TOTAL = createField("bytes_total", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * The column <code>stroom.file_volume_state.update_time_ms</code>.
     */
    public final TableField<FileVolumeStateRecord, Long> UPDATE_TIME_MS = createField("update_time_ms", org.jooq.impl.SQLDataType.BIGINT, this, "");

    /**
     * Create a <code>stroom.file_volume_state</code> table reference
     */
    public FileVolumeState() {
        this(DSL.name("file_volume_state"), null);
    }

    /**
     * Create an aliased <code>stroom.file_volume_state</code> table reference
     */
    public FileVolumeState(String alias) {
        this(DSL.name(alias), FILE_VOLUME_STATE);
    }

    /**
     * Create an aliased <code>stroom.file_volume_state</code> table reference
     */
    public FileVolumeState(Name alias) {
        this(alias, FILE_VOLUME_STATE);
    }

    private FileVolumeState(Name alias, Table<FileVolumeStateRecord> aliased) {
        this(alias, aliased, null);
    }

    private FileVolumeState(Name alias, Table<FileVolumeStateRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> FileVolumeState(Table<O> child, ForeignKey<O, FileVolumeStateRecord> key) {
        super(child, key, FILE_VOLUME_STATE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.FILE_VOLUME_STATE_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<FileVolumeStateRecord, Integer> getIdentity() {
        return Keys.IDENTITY_FILE_VOLUME_STATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<FileVolumeStateRecord> getPrimaryKey() {
        return Keys.KEY_FILE_VOLUME_STATE_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<FileVolumeStateRecord>> getKeys() {
        return Arrays.<UniqueKey<FileVolumeStateRecord>>asList(Keys.KEY_FILE_VOLUME_STATE_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TableField<FileVolumeStateRecord, Integer> getRecordVersion() {
        return VERSION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileVolumeState as(String alias) {
        return new FileVolumeState(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileVolumeState as(Name alias) {
        return new FileVolumeState(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public FileVolumeState rename(String name) {
        return new FileVolumeState(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public FileVolumeState rename(Name name) {
        return new FileVolumeState(name, null);
    }
}
