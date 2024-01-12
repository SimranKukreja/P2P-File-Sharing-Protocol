import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class PeerNode {
    private final int peerId;
    private final CommonConfig dataCommonCfg;
    private final FileValuePieces fileValuePieces;

    Map<Integer, Peer> dataPeerCfg;

    private final ExecutorService fixedThreadPoolExecutor;

    private final ScheduledExecutorService scheduledThreadPoolExecutor;

    private final AtomicInteger optimisticNeighboringPeer = new AtomicInteger(-1);

    private final int prefNeighborsCount;

    private final Map<Integer, Integer> downloadingSpeedMap = new ConcurrentHashMap<>();


    private final Set<Integer> preferredNeighboringPeers = ConcurrentHashMap.newKeySet();

    private final Set<Integer> interestedNeighboringPeers = ConcurrentHashMap.newKeySet();

    private final Map<Integer, TorrentService> peerTorrentServices = new ConcurrentHashMap<>();
    private final Map<Integer, BitSet> peerBitfields = new ConcurrentHashMap<>();
    private final PeerNodeServer peerNodeServer;
    private final PeerNodeClient peerNodeClient;
    private final Set<Integer> completedPeerNodes = new HashSet<>();
    private final BitValues bitValues;

    //constructs a peer node
    public PeerNode(int peerId, CommonConfig dataCommonCfg, Map<Integer, Peer> dataPeerCfg, ExecutorService fixedThreadPoolExecutor, ScheduledExecutorService scheduledThreadPoolExecutor, int prefNeighborsCount) {
        this.peerId = peerId;
        this.dataCommonCfg = dataCommonCfg;
        this.dataPeerCfg = dataPeerCfg;
        this.fixedThreadPoolExecutor = fixedThreadPoolExecutor;
        this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
        this.prefNeighborsCount = prefNeighborsCount;
        this.fileValuePieces = new FileValuePieces(this.peerId, this.dataCommonCfg);
//        Set all of the values in the bitfield (whose size is equal to the number of pieces) to True if the peer has a file and Cut the file into fragments.
        BitSet bitfield = new BitSet(this.dataCommonCfg.getPieceCount());
        if (this.dataPeerCfg.get(this.peerId).isFileExisting()) {
            bitfield.set(0, dataCommonCfg.getPieceCount());
            this.fileValuePieces.divideFileIntoChunks();
        }
        this.bitValues = new BitValues(dataCommonCfg, bitfield);
        // Start peer node server and client
        this.peerNodeServer = new PeerNodeServer();
        this.fixedThreadPoolExecutor.execute(this.peerNodeServer);
        this.peerNodeClient = new PeerNodeClient();
        this.fixedThreadPoolExecutor.execute(this.peerNodeClient);

        for (Peer peer : this.dataPeerCfg.values()) {
            if(peer.isFileExisting()) {
                this.completedPeerNodes.add(peer.fetchPeerId());
            }
        }
    }

    //re-selecting preferred neighbors every p seconds
    public void selectNeighborAgain() {
        // Resetting the un-choked peer
        resetUnchockedPeer();

        //iterate over the download rates and initialize downloadingSpeedMap
        for (int pId : downloadingSpeedMap.keySet()) {
            this.downloadingSpeedMap.put(pId, 0);
        }

        selectprefNeighborsCount();
    }

    // Select prefNeighborsCount with highest download speed and are interested in peer data
    public void selectprefNeighborsCount() {
        // get the download rates of each peer and sort the peer in descending download rate order
        List<Integer> sortedPeersBasedOnDownloadRate = sortPeerBasedOnDownloadRate();

        int pnCount = 0;
        int i = 0;
        while (pnCount < prefNeighborsCount && i < this.interestedNeighboringPeers.size()) {
            int currentPeerNode = sortedPeersBasedOnDownloadRate.get(i);
            try{
                if (checkIfInterestedNeighborHasCurrentPeer(currentPeerNode)) {
                    this.preferredNeighboringPeers.add(currentPeerNode);
                    pnCount++;
                }
            }
            catch(Exception excep){
                excep.printStackTrace();
            }
            i++;
        }
    }

    //checks if the interested neighbors have current peer
    public boolean checkIfInterestedNeighborHasCurrentPeer(int currentPeerNode) {
        return this.interestedNeighboringPeers.contains(currentPeerNode);
    }

    //resets the unchoked peer object
    private void resetUnchockedPeer() {
        //clearing the downloading rates and list of un-choked peers
        this.preferredNeighboringPeers.clear();
    }

    //sorting peers based on their download rates
    public List<Integer> sortPeerBasedOnDownloadRate() {
        List<Map.Entry<Integer, Integer>> sortedDownloadingSpeedMap = getSortedDownloadingSpeedMap();
        List<Integer> orderedPeersBasedOnSpeed = new ArrayList<>();
        try {
            populatePeersOnDownloadRate(sortedDownloadingSpeedMap, orderedPeersBasedOnSpeed);
        } catch (Exception excep) {
            excep.printStackTrace();
        }
        return orderedPeersBasedOnSpeed;

    }

    //populates the peers based on their download rates
    private static void populatePeersOnDownloadRate(List<Map.Entry<Integer, Integer>> sortedDownloadingSpeedMap, List<Integer> orderedPeersBasedOnSpeed) {
        for (Map.Entry<Integer, Integer> entry : sortedDownloadingSpeedMap) {
            orderedPeersBasedOnSpeed.add(entry.getKey());
        }
    }

    // sorts the neighbors according to download rate
    public List<Map.Entry<Integer, Integer>> getSortedDownloadingSpeedMap() {
        List<Map.Entry<Integer, Integer>> sortedDownloadingSpeedMap = new ArrayList<>(downloadingSpeedMap.entrySet());
        try {
            sortedDownloadingSpeedMap.sort(Map.Entry.comparingByValue());
        } catch (Exception excep) {
            excep.printStackTrace();
        }
        return sortedDownloadingSpeedMap;
    }

    //fetches preferred neighbors
    public Set<Integer> getPreferredNeighboringPeers() {
        return this.preferredNeighboringPeers;
    }

    //increment the interested neighbor
//    public int checkInterestedNeighborCount(int currentPeerNode) {
//        return this.prefNeighborsCount + 1;
//    }

    // reset the peer count
//    private void resetNeighboringPeerCount() {
        //clearing the downloading rates and list of un-choked peers
//        this.preferredNeighboringPeers.clear();
//    }

    //sets a neighbor as optimistic neighbor
    public void setOptimisticNeighboringPeer(int neighboringPeer) {
        this.optimisticNeighboringPeer.set(neighboringPeer);
    }

    // fetches the optimistic neighbor and returns the same
    public AtomicInteger getOptimisticNeighboringPeer() {
        return this.optimisticNeighboringPeer;
    }

    //fetches the current service
    public Map<Integer, TorrentService> getPeerTorrentService() {
        return this.peerTorrentServices;
    }

    //fetches the service for given peer id
    public TorrentService getPeerTorrentService(int pId) {
        return this.peerTorrentServices.get(pId);
    }

    //adding new peer service
    public void addPeerTorrentService(int peerId, TorrentService torrentService) {
        try {
            this.peerTorrentServices.put(peerId, torrentService);
            this.downloadingSpeedMap.put(peerId, 0);
        } catch (Exception excep) {
            excep.printStackTrace();
        }
    }

    public Set<Integer> retrieveInterestedNeighboringPeers() {
        return this.interestedNeighboringPeers;
    }

    public void insertInterestedNeighboringPeers(int peerId) {
        this.interestedNeighboringPeers.add(peerId);
    }

    public BitValues getBitValues() {
        return this.bitValues;
    }

    public Map<Integer, BitSet> getPeerBitfields() {
        return this.peerBitfields;
    }
    public void removeInterestedNeighboringPeers(int peerId) {
        this.interestedNeighboringPeers.remove(peerId);
    }

    public boolean checkIfPeerIsUnchoked(int peerId) {
        return preferredNeighboringPeers.contains(peerId) || optimisticNeighboringPeer.get() == peerId;
    }

    public void updateBitField(int peerId, BitSet bitfield) {
        this.peerBitfields.put(peerId, bitfield);
    }

    public void incrementDownloadRate(int peerId) {
        this.downloadingSpeedMap.put(peerId, this.downloadingSpeedMap.get(peerId) + 1);
    }

    public boolean allPeersComplete() {
        Set<Integer> peerIds = this.dataPeerCfg.keySet();
        // Check if the current peer has received all the pieces
        if (bitValues.allPiecesReceived()) {
            peerIds.remove(peerId);
        }
        // Check if all the remaining peers have received the file
        peerIds.removeAll(completedPeerNodes);
        return peerIds.size() == 0;
    }

    public PeerNode.PeerNodeServer getPeerNodeServer() {
        return this.peerNodeServer;
    }

    public void closeSocketNode(int pId) throws IOException {
        peerTorrentServices.get(pId).getTorrentSocket().close();
    }

    public void addCompletedPeer(int peerId) {
        completedPeerNodes.add(peerId);
    }

    // Peer node server class
    public class PeerNodeServer implements Runnable {
        ServerSocket serverNodeSocket;

        public PeerNodeServer() {
            try {
                // peer node server socket that listens to requests for connecting
                Peer currentPeer = PeerNode.this.dataPeerCfg.get(peerId);
                String host = currentPeer.fetchHost();
                int portNo = currentPeer.fetchPortNo();
                this.serverNodeSocket = new ServerSocket(portNo, 50, InetAddress.getByName(host));
            } catch (Exception excep) {
                excep.printStackTrace();
            }
        }

        public ServerSocket getServerNodeSocket() {
            return this.serverNodeSocket;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    Socket socket = serverNodeSocket.accept();
                    PeerNode.this.fixedThreadPoolExecutor.execute(new TorrentService(peerId, PeerNode.this, socket, PeerNode.this.fixedThreadPoolExecutor, PeerNode.this.scheduledThreadPoolExecutor, PeerNode.this.bitValues, PeerNode.this.fileValuePieces));
                }
            } catch (Exception excep) {
                //  excep.printStackTrace();
            }

        }
    }

    // Peer node client class
    public class PeerNodeClient implements Runnable {
        @Override
        public void run() {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            for (Peer peer : PeerNode.this.dataPeerCfg.values()) {
                if (peer.fetchPeerId() == peerId) {
                    break;
                }
                try {
                    Socket socket = null;
                    while (socket == null) {
                        socket = new Socket(peer.fetchHost(), peer.fetchPortNo());
                    }
                    PeerNode.this.fixedThreadPoolExecutor.execute(new TorrentService(peerId, PeerNode.this, peer.fetchPeerId(), socket, PeerNode.this.fixedThreadPoolExecutor, PeerNode.this.scheduledThreadPoolExecutor, PeerNode.this.bitValues, PeerNode.this.fileValuePieces));
                } catch (Exception excep) {
                    excep.printStackTrace();
                }
            }
        }
    }

}
