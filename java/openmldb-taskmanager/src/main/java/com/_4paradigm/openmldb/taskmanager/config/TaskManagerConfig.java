/*
 * Copyright 2021 4Paradigm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com._4paradigm.openmldb.taskmanager.config;

import com._4paradigm.openmldb.taskmanager.util.BatchJobUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

/**
 * The global configuration of TaskManager.
 *
 * Need to call TaskManagerConfig.parser() before accessing configuration.
 */
public class TaskManagerConfig {
    private static Logger logger = LoggerFactory.getLogger(TaskManagerConfig.class);

    public static String HOST;
    public static int PORT;
    public static int WORKER_THREAD;
    public static int IO_THREAD;
    public static String ZK_CLUSTER;
    public static String ZK_ROOT_PATH;
    public static String ZK_TASKMANAGER_PATH;
    public static String ZK_MAX_JOB_ID_PATH;
    public static int ZK_SESSION_TIMEOUT;
    public static int ZK_CONNECTION_TIMEOUT;
    public static int ZK_BASE_SLEEP_TIME;
    public static int ZK_MAX_CONNECT_WAIT_TIME;
    public static int ZK_MAX_RETRIES;
    public static String SPARK_MASTER;
    public static String SPARK_YARN_JARS;
    public static String SPARK_HOME;
    public static int PREFETCH_JOBID_NUM;
    public static String JOB_LOG_PATH;
    public static String SPARK_DEFAULT_CONF;
    public static String SPARK_EVENTLOG_DIR;
    public static int SPARK_YARN_MAXAPPATTEMPTS;
    public static String OFFLINE_DATA_PREFIX;
    public static String NAMENODE_URI;
    public static String BATCHJOB_JAR_PATH;
    public static String HADOOP_CONF_DIR;

    public static void parse() throws IOException, NumberFormatException, ConfigException {
        Properties prop = new Properties();
        prop.load(TaskManagerConfig.class.getClassLoader().getResourceAsStream("taskmanager.properties"));

        HOST = prop.getProperty("server.host", "0.0.0.0");
        PORT = Integer.parseInt(prop.getProperty("server.port", "9902"));
        if (PORT < 1 || PORT > 65535) {
            throw new ConfigException("server.port", "invalid port, should be in range of 1 through 65535");
        }
        WORKER_THREAD = Integer.parseInt(prop.getProperty("server.worker_threads", "4"));
        IO_THREAD = Integer.parseInt(prop.getProperty("server.io_threads", "4"));
        ZK_SESSION_TIMEOUT = Integer.parseInt(prop.getProperty("zookeeper.session_timeout", "5000"));

        ZK_CLUSTER = prop.getProperty("zookeeper.cluster", "");
        if (ZK_CLUSTER.isEmpty()) {
            throw new ConfigException("zookeeper.cluster", "should not be empty");
        }

        ZK_ROOT_PATH = prop.getProperty("zookeeper.root_path", "");
        if (ZK_ROOT_PATH.isEmpty()) {
            throw new ConfigException("zookeeper.root_path", "should not be empty");
        }

        ZK_TASKMANAGER_PATH = ZK_ROOT_PATH + "/taskmanager";
        ZK_MAX_JOB_ID_PATH = ZK_TASKMANAGER_PATH + "/max_job_id";
        ZK_CONNECTION_TIMEOUT = Integer.parseInt(prop.getProperty("zookeeper.connection_timeout", "5000"));
        ZK_BASE_SLEEP_TIME = Integer.parseInt(prop.getProperty("zookeeper.base_sleep_time", "1000"));
        ZK_MAX_RETRIES = Integer.parseInt(prop.getProperty("zookeeper.max_retries", "10"));
        ZK_MAX_CONNECT_WAIT_TIME = Integer.parseInt(prop.getProperty("zookeeper.max_connect_waitTime", "30000"));

        SPARK_MASTER = prop.getProperty("spark.master", "local").toLowerCase();
        if (!Arrays.asList("local", "yarn", "yarn-cluster", "yarn-client").contains(SPARK_MASTER) ) {
            throw new ConfigException("spark.master", "should be one of local, yarn, yarn-cluster or yarn-client");
        }
        boolean isLocal = SPARK_MASTER.equals("local");
        boolean isYarn = SPARK_MASTER.startsWith("yarn");
        boolean isYarnCluster = SPARK_MASTER.equals("yarn") || SPARK_MASTER.equals("yarn-cluster");

        SPARK_YARN_JARS = prop.getProperty("spark.yarn.jars", "");
        if (isLocal && !SPARK_YARN_JARS.isEmpty()) {
            logger.warn("Ignore the config of spark.yarn.jars which is invalid for local mode");
        }
        if (isYarn) {
            if (!SPARK_YARN_JARS.isEmpty() && SPARK_YARN_JARS.startsWith("file://")) {
                throw new ConfigException("spark.yarn.jars", "should not use local filesystem for yarn mode");
            }
        }

        SPARK_HOME = prop.getProperty("spark.home", "");
        if (SPARK_HOME.isEmpty()) {
            try {
                if(System.getenv("SPARK_HOME") == null) {
                    throw new ConfigException("spark.home", "should set config 'spark.home' or environment variable 'SPARK_HOME'");
                } else {
                    SPARK_HOME = System.getenv("SPARK_HOME");
                }
            } catch (Exception e) {
                throw new ConfigException("spark.home", "should set environment variable 'SPARK_HOME' if 'spark.home' is null");
            }
        }
        // TODO: Check if we can get spark-submit

        PREFETCH_JOBID_NUM = Integer.parseInt(prop.getProperty("prefetch.jobid.num", "1"));
        if (PREFETCH_JOBID_NUM < 1) {
            throw new ConfigException("prefetch.jobid.num", "should be larger or equal to 1");
        }

        NAMENODE_URI = prop.getProperty("namenode.uri", "");
        if (!NAMENODE_URI.isEmpty()) {
            logger.warn("Config of 'namenode.uri' will be deprecated later");
        }

        JOB_LOG_PATH = prop.getProperty("job.log.path", "../log/");
        if (JOB_LOG_PATH.isEmpty()) {
            throw new ConfigException("job.log.path", "should not be null");
        } else {
            if (JOB_LOG_PATH.startsWith("hdfs") || JOB_LOG_PATH.startsWith("s3")) {
                throw new ConfigException("job.log.path", "only support local filesystem");
            }

            File directory = new File(JOB_LOG_PATH);
            if (!directory.exists()) {
                logger.info("The log path does not exist, try to create directory: " + JOB_LOG_PATH);
                boolean created = directory.mkdirs();
                if (created) {
                    throw new ConfigException("job.log.path", "fail to create log path");
                }
            }
        }

        SPARK_DEFAULT_CONF = prop.getProperty("spark.default.conf", "");
        if (!SPARK_DEFAULT_CONF.isEmpty()) {
            String[] defaultSparkConfs = TaskManagerConfig.SPARK_DEFAULT_CONF.split(",");
            for (String sparkConfMap: defaultSparkConfs) {
                if (!sparkConfMap.isEmpty()) {
                    String[] kv = sparkConfMap.split("=");
                    if (kv.length != 2) {
                        throw new ConfigException("spark.default.conf", String.format("error format of %s", sparkConfMap));
                    } else if (!kv[0].startsWith("spark")) {
                        throw new ConfigException("spark.default.conf", String.format("config key should start with 'spark' but get %s", kv[0]));
                    }
                }
            }
        }

        SPARK_EVENTLOG_DIR = prop.getProperty("spark.eventLog.dir", "");
        if (!SPARK_EVENTLOG_DIR.isEmpty() && isYarn) {
            // TODO: Check if we can use local filesystem with yarn-client mode
            if (SPARK_EVENTLOG_DIR.startsWith("file://")) {
                throw new ConfigException("spark.eventLog.dir", "should not use local filesystem for yarn mode");
            }
        }

        SPARK_YARN_MAXAPPATTEMPTS = Integer.parseInt(prop.getProperty("spark.yarn.maxAppAttempts", "1"));
        if (SPARK_YARN_MAXAPPATTEMPTS < 1) {
            throw new ConfigException("spark.yarn.maxAppAttempts", "should be larger or equal to 1");
        }

        OFFLINE_DATA_PREFIX = prop.getProperty("offline.data.prefix", "file:///tmp/openmldb_offline_storage/");
        if (OFFLINE_DATA_PREFIX.isEmpty()) {
            throw new  ConfigException("offline.data.prefix", "should not be null");
        } else {
            if (isYarnCluster && OFFLINE_DATA_PREFIX.startsWith("file://") ) {
                throw new ConfigException("offline.data.prefix", "should not use local filesystem for yarn mode");
            }
        }

        BATCHJOB_JAR_PATH = prop.getProperty("batchjob.jar.path", "");
        if (BATCHJOB_JAR_PATH.isEmpty()) {
            try {
                BATCHJOB_JAR_PATH = BatchJobUtil.findLocalBatchJobJar();
            } catch (Exception e) {
                throw new ConfigException("batchjob.jar.path", "config is null and fail to load default openmldb-batchjob jar");
            }
        }

        HADOOP_CONF_DIR = prop.getProperty("hadoop.conf.dir", "");
        if (isYarn && HADOOP_CONF_DIR.isEmpty()) {
            try {
                if(System.getenv("HADOOP_CONF_DIR") == null) {
                    throw new ConfigException("hadoop.conf.dir", "should set config 'hadoop.conf.dir' or environment variable 'HADOOP_CONF_DIR'");
                } else {
                    HADOOP_CONF_DIR = System.getenv("HADOOP_CONF_DIR");
                }
            } catch (Exception e) {
                throw new ConfigException("hadoop.conf.dir", "should set environment variable 'HADOOP_CONF_DIR' if 'hadoop.conf.dir' is null");
            }
        }
        // TODO: Check if we can get core-site.xml
    }

}
