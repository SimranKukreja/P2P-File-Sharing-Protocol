import java.util.LinkedHashMap;
import java.util.Map;

public class Peer {
    //props
    private int portNo;
    private boolean fileExists;
    private int id;
    private String host;

    //intitializing the props
    public Peer(int portNo, boolean fileExists, int id, String host) {
        this.portNo = portNo;
        this.fileExists = fileExists;
        this.id = id;
        this.host = host;
    }

    //getter for portNo prop
    public int fetchPortNo() {
        return portNo;
    }

    //getter for isFileExisting prop
    public boolean isFileExisting() {
        return fileExists;
    }

    //getter for fetchPeerId prop
    public int fetchPeerId() {
        return id;
    }

    //getter for fetchHost prop
    public String fetchHost() {
        return host;
    }

    private Map<Integer, Peer> completePeerMapping = new LinkedHashMap<>();

    //method to return all the peer details
    public String fetchDetails() {
        return "portNo-" + portNo + "\n" +
               "fileExists-"+ fileExists + "\n" +
               "id-" + id + "\n" +
               "host-" + host + "\n";             
    }

    //method to return the complete (peer id:peer) map
    public Map<Integer, Peer> getCompletePeerMapping() {
        return this.completePeerMapping;
    }
}
