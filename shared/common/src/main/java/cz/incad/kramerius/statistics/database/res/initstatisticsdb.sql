
-- access_log tabulka --
CREATE SEQUENCE statistics_access_log_ID_SEQUENCE INCREMENT BY 1 START WITH 1 MINVALUE 0;
CREATE table statistics_access_log(
    record_ID INT NOT NULL, 
    PID VARCHAR(255), 
    DATE TIMESTAMP,
    REMOTE_IP_ADDRESS VARCHAR(255), 
    "USER" VARCHAR(255), 
    REQUESTED_URL VARCHAR(2048), 
    STAT_ACTION VARCHAR(255),
    SESSION_ID VARCHAR(255), 
    dnnt boolean,
    providedbydnnt boolean,
    evaluatemap TEXT,
    usersessionattributes TEXT,
    dbversion VARCHAR(255), 
    dnnt_labels text[],
    
        PRIMARY KEY(record_ID));

        
-- detail --
CREATE SEQUENCE statistic_access_log_detail_ID_SEQUENCE INCREMENT BY 1 START WITH 1 MINVALUE 0;
create table statistic_access_log_detail(
    detail_ID INT NOT NULL, 
    PID VARCHAR(255), 
    model VARCHAR(255), 
    ISSUED_DATE VARCHAR(255), 
    RIGHTS VARCHAR(255),
    LANG VARCHAR(255),
    TITLE VARCHAR(1024),
    solr_date text,
    issn text[],
    isbn text[],
    ccnb text[],
    
    BRANCH_ID INT NOT NULL,
    RECORD_ID INT NOT NULL REFERENCES statistics_access_log(record_ID) on delete cascade,
    PRIMARY KEY(detail_ID)
);


CREATE SEQUENCE statistic_access_log_detail_authors_ID_SEQUENCE INCREMENT BY 1 START WITH 1 MINVALUE 0;
create table statistic_access_log_detail_authors(
   author_id int not null,
   AUTHOR_NAME VARCHAR(1024), 
   DETAIL_ID INT NOT NULL REFERENCES statistic_access_log_detail(detail_id) on delete cascade,
   RECORD_ID INT NOT NULL REFERENCES statistics_access_log(record_ID) on delete cascade,
   PRIMARY KEY(author_id)
);
 
