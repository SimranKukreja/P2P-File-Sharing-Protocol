import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ExecutorService;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PeerProcess {

    static Logger LOGGER = LogManager.getLogger(TorrentService.class);

    //declaring constants
    public static final String CURR_DIRECTORY = System.getProperty("user.dir");
    public static final String COMMON_CONFIG = "Common.cfg";
    public static final String PEER_INFO_CONFIG = "PeerInfo.cfg";

    public static Map<Integer, Peer> peerMap = new LinkedHashMap<>();

    //checking if the input path provided is valid
    public static boolean isValidFilePath(String path) {
        return path != null && path.length() > 0;
    }

    //building the full file path using the current directory and path given
    private static String buildFullFilePath(String filePath) {
        return CURR_DIRECTORY + File.separator + filePath;
    }

    //reading the passed file line by line and storing the data in a list
    public static List<String> readLinesFromFile(String fullPath) throws IOException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fullPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    //reading the file provided in the path
    public static List<String> readFile(String path) {
        List<String> content = new ArrayList<>();
        if (isValidFilePath(path)) {
            String fullPath = buildFullFilePath(path);

            try {
                content = readLinesFromFile(fullPath);
            } catch (IOException error) {
                error.printStackTrace();
            }
        }
        return content;
    }

    //adding peer details to the peer map
    public static void addPeerInfo(int portNo, boolean fileExists, int id, String host) {
        LOGGER.info("id: {}", id);
        LOGGER.info("port number: {}", portNo);
        LOGGER.info("hostname: {}", host);
        LOGGER.info("fileExists: {}", fileExists);

        Peer peerData = new Peer(portNo, fileExists, id, host);
        peerMap.put(id, peerData);
    }

    //parsing the peer config data line by line and adding it to the map
    public static void parsePeerLine(String line) {
        String[] values = line.split(" ");
        addPeerInfo(Integer.parseInt(values[2]), Integer.parseInt(values[3]) == 1, Integer.parseInt(values[0]), values[1]);
    }

    //parsing the config data from the peer config file
    public static void parsePeerData(List<String> configLines) {
        LOGGER.info("Peer Config:");
        for (String line : configLines) {
            parsePeerLine(line);
        }
    }

    //fetching specific peer data from the peer map
    public static Peer fetchPeer(int id) {
        return peerMap.get(id);
    }

    //Returning all peers as a peer map
    public static Map<Integer, Peer> getPeers() {
        return peerMap;
    }

    //creating the peer directory having peer id and current directory
    public static void createPeerDirectory(int peerId, String currDir) {
        try {
            String peerPath = Paths.get(currDir, String.format("peer_%d", peerId)).toString();
            Files.createDirectories(Paths.get(peerPath));
        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    //copy input file from source to destination
    public static void copyInputFile(int peerId, String inputFileName, Boolean fileExists) {
        if (fileExists) {
            try {
                Path src = Paths.get(CURR_DIRECTORY, inputFileName);
                Path dest = Paths.get(CURR_DIRECTORY, String.format("peer_%d", peerId), inputFileName);
                Files.copy(src, dest);
            } catch (IOException error) {
                error.printStackTrace();
            }
        }
    }

    //building the directory by creating a peer directory and copying the input file
    public static void buildDirectory(String fileName, String currDir, Peer peerData) throws Exception {
        createPeerDirectory(peerData.fetchPeerId(), currDir);
        copyInputFile(peerData.fetchPeerId(), fileName, peerData.isFileExisting());
    }

    //implementing concurrency
    private static void initializePeerAndSchedulers(int peerId, Map<Integer, Peer> dataPeerCfg,  CommonConfig dataCommonCfg) {
        ExecutorService fixedThreadPoolExecutor = createFixedThreadPool(8);
        ScheduledExecutorService scheduledThreadPoolExecutor = createScheduledThreadPool(8);
        PeerNode peerNode = new PeerNode(peerId, dataCommonCfg, dataPeerCfg, fixedThreadPoolExecutor, scheduledThreadPoolExecutor, dataCommonCfg.getPrefNeighborsCount());
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new SelectPreferredNeighborExecutor(peerId, peerNode), 0L, dataCommonCfg.getUnchokingTime(), TimeUnit.SECONDS);
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new SelectOptimisticallyUnchokedExecutor(peerId, peerNode, dataPeerCfg), 0L, dataCommonCfg.getOptUnchokingTime(), TimeUnit.SECONDS);
        scheduledThreadPoolExecutor.scheduleAtFixedRate(new HandlePiecesRequestedScheduler(peerNode), 0L, 30, TimeUnit.SECONDS);
    }

    //intializing ExecutorService
    private static ExecutorService createFixedThreadPool(int numThreads) {
        return Executors.newFixedThreadPool(numThreads);
    }

    //initializing ScheduledExecutorService
    private static ScheduledExecutorService createScheduledThreadPool(int numThreads) {
        return Executors.newScheduledThreadPool(numThreads);
    }

    public static void main(String[] peerProcessArguments) {

        //Getting the peer id from arguments
        int peerId = Integer.parseInt(peerProcessArguments[0]);

        List<String> dataPeerCfg = readFile(PEER_INFO_CONFIG);
        PeerProcess.parsePeerData(dataPeerCfg);

        List<String> dataCommonCfg = readFile(COMMON_CONFIG);
        CommonConfig commonConfig = new CommonConfig();
        commonConfig.parseCommonCfgData(dataCommonCfg);
        commonConfig.printCommonConfig();
        
        try {
            buildDirectory(commonConfig.getCommonCfgFileName(), CURR_DIRECTORY, PeerProcess.fetchPeer(peerId));
        } catch (Exception excep) {
            excep.printStackTrace();
        }

//        PeerProcess.initializePeerAndSchedulers(peerId, peerMap, commonConfig);

        ExecutorService executorService = Executors.newFixedThreadPool(8);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

        PeerNode peer = new PeerNode(peerId, commonConfig, peerMap, executorService, scheduler, commonConfig.getPrefNeighborsCount());
        scheduler.scheduleAtFixedRate(new SelectPreferredNeighborExecutor(peerId, peer), 0L, commonConfig.getUnchokingTime(), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(new SelectOptimisticallyUnchokedExecutor(peerId, peer, peerMap), 0L, commonConfig.getOptUnchokingTime(), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(new HandlePiecesRequestedScheduler(peer), 0L, 30, TimeUnit.SECONDS);
    }

}
