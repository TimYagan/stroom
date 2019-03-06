/*
 * This file is generated by jOOQ.
 */
package stroom.job.impl.db.jooq;


import javax.annotation.Generated;

import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.Internal;

import stroom.job.impl.db.jooq.tables.Job;
import stroom.job.impl.db.jooq.tables.JobNode;
import stroom.job.impl.db.jooq.tables.records.JobNodeRecord;
import stroom.job.impl.db.jooq.tables.records.JobRecord;


/**
 * A class modelling foreign key relationships and constraints of tables of 
 * the <code>stroom</code> schema.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.9"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

    // -------------------------------------------------------------------------
    // IDENTITY definitions
    // -------------------------------------------------------------------------

    public static final Identity<JobRecord, Integer> IDENTITY_JOB = Identities0.IDENTITY_JOB;
    public static final Identity<JobNodeRecord, Integer> IDENTITY_JOB_NODE = Identities0.IDENTITY_JOB_NODE;

    // -------------------------------------------------------------------------
    // UNIQUE and PRIMARY KEY definitions
    // -------------------------------------------------------------------------

    public static final UniqueKey<JobRecord> KEY_JOB_PRIMARY = UniqueKeys0.KEY_JOB_PRIMARY;
    public static final UniqueKey<JobRecord> KEY_JOB_NAME = UniqueKeys0.KEY_JOB_NAME;
    public static final UniqueKey<JobNodeRecord> KEY_JOB_NODE_PRIMARY = UniqueKeys0.KEY_JOB_NODE_PRIMARY;

    // -------------------------------------------------------------------------
    // FOREIGN KEY definitions
    // -------------------------------------------------------------------------

    public static final ForeignKey<JobNodeRecord, JobRecord> JOB_ID = ForeignKeys0.JOB_ID;

    // -------------------------------------------------------------------------
    // [#1459] distribute members to avoid static initialisers > 64kb
    // -------------------------------------------------------------------------

    private static class Identities0 {
        public static Identity<JobRecord, Integer> IDENTITY_JOB = Internal.createIdentity(Job.JOB, Job.JOB.ID);
        public static Identity<JobNodeRecord, Integer> IDENTITY_JOB_NODE = Internal.createIdentity(JobNode.JOB_NODE, JobNode.JOB_NODE.ID);
    }

    private static class UniqueKeys0 {
        public static final UniqueKey<JobRecord> KEY_JOB_PRIMARY = Internal.createUniqueKey(Job.JOB, "KEY_job_PRIMARY", Job.JOB.ID);
        public static final UniqueKey<JobRecord> KEY_JOB_NAME = Internal.createUniqueKey(Job.JOB, "KEY_job_name", Job.JOB.NAME);
        public static final UniqueKey<JobNodeRecord> KEY_JOB_NODE_PRIMARY = Internal.createUniqueKey(JobNode.JOB_NODE, "KEY_job_node_PRIMARY", JobNode.JOB_NODE.ID);
    }

    private static class ForeignKeys0 {
        public static final ForeignKey<JobNodeRecord, JobRecord> JOB_ID = Internal.createForeignKey(stroom.job.impl.db.jooq.Keys.KEY_JOB_PRIMARY, JobNode.JOB_NODE, "job_id", JobNode.JOB_NODE.JOB_ID);
    }
}
