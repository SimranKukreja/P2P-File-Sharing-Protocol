import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class SelectPreferredNeighborExecutor implements Runnable {
    public int peerId;

    int CHOKEDPEER = 0;

    int UNCHOKEDPEER = 1;

    private PeerNode peerNode;

    Logger LOGGER = LogManager.getLogger(TorrentService.class);

    public SelectPreferredNeighborExecutor(int peerId, PeerNode peerNode) {
        this.peerId = peerId;
        this.peerNode = peerNode;
    }

//    public boolean checkIfThreadIsNotInterrupted() {
//        return !Thread.currentThread().isInterrupted();
//    }

    @Override
    public void run() {
        if (checkIfThreadIsInterrupted())
            return;
        this.peerNode.selectNeighborAgain();
        //get the preferred neighboring peers
        Set<Integer> preferredNeighboringPeers = this.peerNode.getPreferredNeighboringPeers();
        LOGGER.info("{}: Peer {} has the preferred neighbors {}", fetchCurrTime(), this.peerId, preferredNeighboringPeers);

        //validate neighbor peer type
        validatePeerNeighbors();
    }

    public static String fetchCurrTime() {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now());
    }

    //check each neighbor type
    private void validatePeerNeighbors() {
        try {
            for (Map.Entry<Integer, TorrentService> peerEntry : getPeerEntries()) {
                int peerId = peerEntry.getKey();
                TorrentService torrentService = peerEntry.getValue();
                validateExtractedPeers(peerId, torrentService);
            }
        } catch (Exception excep) {
            excep.printStackTrace();
        }

    }

    //send unchoke message is peer is neighbor else choke the peer
    private void validateExtractedPeers(int peerId, TorrentService torrentService) {
        try {
            if (checkIfPeerIsNeighbor(peerId)) {
                torrentService.pingNeighborWithMessage(ConstantFields.MessageForm.UNCHOKE);
            } else if (checkIfPeerIsOptimisticNeighbor(peerId)) {
                return;
            } else {
                torrentService.pingNeighborWithMessage(ConstantFields.MessageForm.CHOKE);
            }
        } catch (Exception excep) {
            excep.printStackTrace();
        }

    }

    //returns the peers entries from the torrent service
    private Set<Map.Entry<Integer, TorrentService>> getPeerEntries() {
        return peerNode.getPeerTorrentService().entrySet();
    }

    //checks if the thread is interrupted
    public boolean checkIfThreadIsInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    //checks if the given peer Id is a neighbor
    public boolean checkIfPeerIsNeighbor(int peerId) {
        return peerNode.getPreferredNeighboringPeers().contains(peerId);
    }

    //checks if the given peer id is a optimistic neighbor
    public boolean checkIfPeerIsOptimisticNeighbor(int peerId) {
        return peerNode.getOptimisticNeighboringPeer().get() == peerId;
    }

    // checks if the given neighbor exists in the torrent
    public boolean checkExistanceOfNeighbors(int peerId) {
        boolean answer = false;
        try {
            for (Map.Entry<Integer, TorrentService> peerEntry : getPeerEntries()) {
               if(peerEntry.getKey() == peerId){
                   answer = true;
               }
            }
        } catch (Exception excep) {
            excep.printStackTrace();
        }
        return answer;
    }
}
