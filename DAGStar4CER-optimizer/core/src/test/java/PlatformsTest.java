import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import core.statistics.platform.Flink.FlinkJob;
import core.statistics.platform.Flink.FlinkParser;
import core.statistics.platform.Flink.Jobmanager;
import core.statistics.util.ClusterConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class PlatformsTest {
    //Delays and periods (in ms) of timers. Set to 0 for one-shot timers.
    private static final long FlinkRefreshPeriod = 10000;
    //private static final long YarnRefreshPeriod = 10000;
    //private static final long SparkRefreshPeriod = 10000;
    private static final long FlinkDelay = 1000;
    //private static final long SparkDelay = 1000;
    // private static final long YarnDelay = 0;

    //Objects that collect and store framework metrics
    private static FlinkParser flinkParser;
    //private static SparkParser sparkParser;
    private static Jobmanager jobmanager;

    //A cache of the latest applications retrieved by the equivalent RM
    // private static List<App> yarnApps;
    private static List<FlinkJob> flinkJobs;
    // private static List<SparkJobApp> sparkJobs;

    //Used for object mapping
    private static ObjectMapper objectMapper;

    //Timers
    private Timer flink_timer, spark_timer, yarn_timer;

    //Run this test for ~30 sec
    private long TEST_DURATION = 10000;

    @BeforeEach
    void setUp() {
        //Object used to handle data from REST api calls.
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        //Used for JSON deserialization.
        flinkParser = new FlinkParser(ClusterConstants.flink_master, objectMapper);

        //Spark parser
        // sparkParser = new SparkParser(ClusterConstants.spark_master, objectMapper);

        //Init timers
        flink_timer = new Timer("FlinkMetricsTimer");
        // spark_timer = new Timer("SparkMetricsTimer");
        // yarn_timer = new Timer("YarnMetricsTimer");
    }

    @Test
    void testFL() {
        System.out.println("hmm");
        //Connect to Flink JM and retrieve stats.
        jobmanager = flinkParser.fetchFlinkJobManager();
        flinkJobs = flinkParser.fetchFlinkJobs(null, "FAILED");
        System.out.println("hmm2" + flinkJobs.size());
        File myObj = new File("StatsCustom_4.txt");

        FileWriter myWriter = null;
        try {
            myWriter = new FileWriter("StatsCustom_4.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (FlinkJob job : flinkJobs) {
            System.out.println("Monitoring flink job:" + job.jid);
            System.out.println(job.name);
            System.out.println(job.vertices.get(4).metrics.readRecords);
            System.out.println(job.vertices.get(4).duration);

            try {
                String[] jobnames = job.name.split("_");
                // myWriter.write("\n"+job.name);

                if (jobnames[2].startsWith("custom")) {
                    jobnames[2] = "CustomSource";
                } else {
                    jobnames[2] = "KafkaTopic";
                }
                if (jobnames[3].startsWith("custom")) {
                    jobnames[3] = "CustomSource";
                } else {
                    jobnames[3] = "KafkaTopic";
                }
                if (jobnames[4].startsWith("custom")) {
                    jobnames[4] = "CustomSource";
                } else {
                    jobnames[4] = "KafkaTopic";
                }


                myWriter.write("\nFlink_Join," + jobnames[2] + "," + jobnames[3] + "," + jobnames[6] + "," + jobnames[7] + "," + jobnames[4] + "," + jobnames[5]);
                myWriter.write("," + job.vertices.get(4).metrics.readRecords / (job.vertices.get(4).duration / 1000) + "," + job.vertices.get(4).metrics.readRecords + "," + job.vertices.get(4).duration);


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testPeriodFLID() {
        int z = 32;
        ArrayList<String> hs = new ArrayList<String>();


        //Connect to Flink JM and retrieve stats.
        jobmanager = flinkParser.fetchFlinkJobManager();
        flinkJobs = flinkParser.fetchFlinkJobs(null, "RUNNING");
        System.out.println("hmm2" + flinkJobs.size());

        for (FlinkJob job : flinkJobs) {

            if (!hs.contains(job.jid)) {
                try {
                    hs.add(job.jid);
                    System.out.println("Monitoring flink job:" + job.jid);
                    System.out.println(job.name);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
    }

    @Test
    void testPeriodFL() {
        int z = 32;
        ArrayList<String> hs = new ArrayList<String>();
        while (true) {

            File myObj = new File("FinalStats_1_" + z + ".txt");

            FileWriter myWriter = null;
            try {
                myWriter = new FileWriter("FinalStats_1_" + z + ".txt");
            } catch (IOException e) {
                e.printStackTrace();
            }
            z++;
            //Connect to Flink JM and retrieve stats.
            jobmanager = flinkParser.fetchFlinkJobManager();
            flinkJobs = flinkParser.fetchFlinkJobs(null, "FAILED");
            System.out.println("hmm2" + flinkJobs.size());


            for (FlinkJob job : flinkJobs) {

                if (!hs.contains(job.jid)) {
                    try {
                        hs.add(job.jid);
                        System.out.println("Monitoring flink job:" + job.jid);
                        System.out.println(job.name);
                        System.out.println(job.vertices.get(4).metrics.readRecords);
                        System.out.println(job.vertices.get(4).duration);
                        String[] jobnames = job.name.split("_");
                        // myWriter.write("\n"+job.name);

                        if (jobnames[2].startsWith("custom")) {
                            jobnames[2] = "CustomSource";
                        } else {
                            jobnames[2] = "KafkaTopic";
                        }
                        if (jobnames[3].startsWith("custom")) {
                            jobnames[3] = "CustomSource";
                        } else {
                            jobnames[3] = "KafkaTopic";
                        }
                        if (jobnames[4].startsWith("custom")) {
                            jobnames[4] = "CustomSource";
                        } else {
                            jobnames[4] = "KafkaTopic";
                        }


                        myWriter.write("\nFlink_Join," + jobnames[2] + "," + jobnames[3] + "," + jobnames[6] + "," + jobnames[7] + "," + jobnames[4] + "," + jobnames[5]);
                        myWriter.write("," + job.vertices.get(4).metrics.readRecords / (job.vertices.get(4).duration / 1000) + "," + job.vertices.get(4).metrics.readRecords + "," + job.vertices.get(4).duration);


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                try {
                    myWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Thread.sleep(20 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Test
    void testPlatformStatistics() {
        //Flink metrics collected by this periodic task
        flink_timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //Connect to Flink JM and retrieve stats.
            }
        }, FlinkDelay);

////        Parse spark jobs, requires a list of yarn apps.
//       spark_timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                sparkJobs = sparkParser.fetchSparkJobs(yarnApps);
//                for (SparkJobApp job : sparkJobs) {
//                    System.out.println("Monitoring spark job:" + job.id);
//                }
//            }
//        }, SparkDelay, SparkRefreshPeriod);
//
////        YARN job metrics (For Spark AND Apex)
//        yarn_timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                yarnApps = YARNcontroller.getYarnApps(ClusterConstants.yarn_master, objectMapper, "user1", 0, "RUNNING");
//                for (App app : yarnApps) {
//                    System.out.println("Monitoring yarn job: " + app.id);
//                }
//            }
//        }, YarnDelay, YarnRefreshPeriod);

        //Wait for the tasks to complete
        try {
            Thread.sleep(TEST_DURATION + 1000);
        } catch (InterruptedException e) {
        }
    }

    @AfterEach
    void teardown() {
        //Shutdown all timers
        flink_timer.cancel();
        flink_timer.purge();
        //spark_timer.cancel();
        //spark_timer.purge();
        //yarn_timer.cancel();
        //yarn_timer.purge();
    }
}