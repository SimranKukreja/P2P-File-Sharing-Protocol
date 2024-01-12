import java.io.OutputStream;
import java.util.ArrayList;

public class TorrentMessenger implements Runnable {

    private final OutputStream outputChannel;
    private final byte[] messageContent;

    // constructs output stream and intializes message
    public TorrentMessenger(OutputStream outputChannel, byte[] messageContent) {
        this.outputChannel = outputChannel;
        this.messageContent = messageContent;
    }

    //processes the messaging
    @Override
    public void run() {
        try {
            if (Thread.currentThread().isInterrupted())
                return;
            synchronized(outputChannel) {
                outputChannel.write(this.messageContent);
            }
        } catch (Exception excep){
            // excep.printStackTrace();
        }
    }
}
