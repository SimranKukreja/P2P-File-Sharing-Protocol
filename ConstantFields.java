import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

public class ConstantFields {

    //defines the constants for the peer block
    public static final Integer HEADER_LENGTH = 32;
    public static final String HEADER = "P2PBITTORRENTFILES";
    public static final Integer HEADER_FRONT = 0;
    public static final Integer HEADER_FIELD = 18;
    public static final Integer PEER_ZERO_BITS_FIELD = 10;
    public static final Integer PEER_ID_FIELD = 4;
    public static final Integer PEER_ZERO_BITS_FRONT = 10;
    public static final Integer PEER_ID_FRONT = 28;

    public static final Integer MESSAGE_LENGTH_FRONT_INDEX = 0;
    public static final Integer MESSAGE_LENGTH_FIELD_INDEX = 4;
    public static final Integer MESSAGE_TYPE_FRONT_INDEX = 4;
    public static final Integer MESSAGE_TYPE_FIELD_INDEX = 1;

    public static final Integer PIECE_FRONT = 0;
    public static final Integer PIECE_INDEX = 4;

    public static final String WORKING_LOC = System.getProperty("user.dir");


    public static enum MessageForm {
        CHOKE(0), UNCHOKE(1), INTERESTED(2), NOT_INTERESTED(3),
        HAVE(4), BITFIELD(5), REQUEST(6), PIECE(7), COMPLETED(8);
        private final int messageValue;

        //initializes the message type
        private MessageForm(int messageValue) {
            this.messageValue = messageValue;
        }

        public int getMessageVal() {
            return this.messageValue;
        }

        //returns the type of message
        public static MessageForm getMessageFormByValue(int messageValue) {
            MessageForm defaultMessageForm = null;
            try{
                for (MessageForm messageForm: MessageForm.values()) {
                    if (checkIfMessageIsSame(messageValue, messageForm)) {
                        return messageForm;
                    }
                }
            }
            catch(Exception excep){
                excep.printStackTrace();
            }
            return defaultMessageForm;
        }

        ///validates if messageValue and MessageForm is same
        private static boolean checkIfMessageIsSame(int messageValue, MessageForm messageForm) {
            return messageForm.getMessageVal() == messageValue;
        }
    }
    
}
