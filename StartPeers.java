//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//public class StartPeers {
//    public static final String CURR_DIRECTORY = System.getProperty("user.dir");
//    // Method to execute a command and wait for its completion
//    private static void executeCommand(String[] command) throws Exception {
//        Process process = Runtime.getRuntime().exec(command);
//        process.waitFor();
//    }
//
//    // Method to copy different types of files to the remote host
//    private static void copyFilesToRemote(String username, Peer peerInfo, String workingDir, String... filePatterns) throws Exception {
//        for (String filePattern : filePatterns) {
//            executeCommand(new String[]{"sh", "-c", String.format("scp %s %s@%s:%s", filePattern, username, peerInfo.fetchHost(), workingDir)});
//        }
//    }
//
//    // Method to copy necessary files to the remote peer's working directory
//    private static void setupRemotePeerEnvironment(String username, Peer peerInfo, String workingDir, String inputFileName) throws Exception {
//        // Create the working directory on the remote host
//        executeCommand(new String[]{"sh", "-c", String.format("ssh %s@%s mkdir -p %s", username, peerInfo.fetchHost(), workingDir)});
//
//        // Copy various file types to the remote host
//        copyFilesToRemote(username, peerInfo, workingDir, "*.java", "-r lib/", "-r *.xml", "*.cfg", inputFileName, "run.sh");
//    }
//
//    // Method to start the peer process on the remote host
//    private static void startRemotePeerProcess(String username, Peer peerInfo, String workingDir) throws Exception {
//        // Give executable permissions to the run script
//        executeCommand(new String[]{"sh", "-c", String.format("ssh %s@%s chmod 777 %s", username, peerInfo.fetchHost(), Paths.get(workingDir, "run.sh").toString())});
//
//        // Start the peer process
//        executeCommand(new String[]{"sh", "-c", String.format("ssh %s@%s %s %s %d", username, peerInfo.fetchHost(), Paths.get(workingDir, "run.sh").toString(), workingDir, peerInfo.getId())});
//    }
//
//    public static List<String> readLinesFromFile(String fullPath) throws IOException {
//        List<String> lines = new ArrayList<>();
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(fullPath))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                lines.add(line);
//            }
//        }
//        return lines;
//    }
//
//    public static boolean isValidFilePath(String path) {
//        return path != null && path.length() > 0;
//    }
//
//    //building the full file path using the current directory and path given
//    private static String buildFullFilePath(String filePath) {
//        return CURR_DIRECTORY + File.separator + filePath;
//    }
//
//    public static List<String> readFile(String path) {
//        List<String> content = new ArrayList<>();
//        if (isValidFilePath(path)) {
//            String fullPath = buildFullFilePath(path);
//
//            try {
//                content = readLinesFromFile(fullPath);
//            } catch (IOException error) {
//                error.printStackTrace();
//            }
//        }
//        return content;
//    }
//    public static void main(String[] args) {
//        // Read system properties for configuration
//        String username = System.getProperty("username");
//        String workingDir = System.getProperty("workingDir");
//        String inputFileName = System.getProperty("inputFileName");
//
//        // Read and parse the PeerInfo configuration file
//        ReadFile readFile = new ReadFile();
//        List<String> peerInfoLines = readFile.read(Constants.PEER_INFO_CONFIG_FILE_NAME);
//        PeerInfoCfg peerInfoCfg = new PeerInfoCfg();
//        peerInfoCfg.parse(peerInfoLines);
//
//        // Start remote peers
//        try {
//            for (Peer peerInfo : peerInfoCfg.getPeers().values()) {
//                setupRemotePeerEnvironment(username, peerInfo, workingDir, inputFileName);
//                startRemotePeerProcess(username, peerInfo, workingDir);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
