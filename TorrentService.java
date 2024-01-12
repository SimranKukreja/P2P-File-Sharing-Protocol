import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TorrentService implements Runnable {
    PeerNode peerNode1;
    PeerNode peerNode2;

    int peerNode1Id;
    int peerNode2Id;

    Socket torrentSocket;
    ExecutorService fixedThreadPoolExecutor;

    OutputStream outputChannel;
    InputStream inputChannel;

    ScheduledExecutorService scheduledThreadPoolExecutor;

    BitValues bitfieldValue;
    FileValuePieces fileValuePieces;
    boolean startedHandshake = false;
    boolean isChoked = true;
    BitSet peerBitfield;

    Logger LOGGER = LogManager.getLogger(TorrentService.class);

    //constricts and initializes peers and schedulers
    public TorrentService(int peerNode1Id, PeerNode peerNode1, Socket torrentSocket, ExecutorService fixedThreadPoolExecutor,
                          ScheduledExecutorService scheduledThreadPoolExecutor,
                          BitValues bitfieldValue, FileValuePieces fileValuePieces) throws IOException {
        this.peerNode1Id = peerNode1Id;
        this.peerNode1 = peerNode1;
        this.torrentSocket = torrentSocket;
        this.fixedThreadPoolExecutor = fixedThreadPoolExecutor;
        this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
        this.bitfieldValue = bitfieldValue;
        this.fileValuePieces = fileValuePieces;
        this.inputChannel = this.torrentSocket.getInputStream();
        this.outputChannel = this.torrentSocket.getOutputStream();
    }

    public TorrentService(int peerNode1Id, PeerNode peerNode1, int peerNode2Id, Socket torrentSocket, ExecutorService fixedThreadPoolExecutor,
                          ScheduledExecutorService scheduledThreadPoolExecutor,
                          BitValues bitfieldValue, FileValuePieces fileValuePieces) throws IOException {
        this.peerNode1Id = peerNode1Id;
        this.peerNode1 = peerNode1;
        this.peerNode2Id = peerNode2Id;
        this.torrentSocket = torrentSocket;
        this.fixedThreadPoolExecutor = fixedThreadPoolExecutor;
        this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;
        this.bitfieldValue = bitfieldValue;
        this.fileValuePieces = fileValuePieces;
        this.inputChannel = this.torrentSocket.getInputStream();
        this.outputChannel = this.torrentSocket.getOutputStream();
    }

    //handles COMPLETE
    private void handleCompleted() throws IOException {
        this.peerNode1.addCompletedPeer(this.peerNode2Id);
        if (peerNode1.allPeersComplete()) {
            this.fileValuePieces.discardPiecesFolder();
            this.fixedThreadPoolExecutor.shutdownNow();
            this.scheduledThreadPoolExecutor.shutdown();
            peerNode1.getPeerNodeServer().getServerNodeSocket().close();
            peerNode1.closeSocketNode(this.peerNode2Id);
            LOGGER.info("{}: Peer {} has downloaded the file and terminated", fetchCurrTime(), this.peerNode1Id);
        }
    }

    // handles PIECE message
    private void handlePiece(int messageLength) throws IOException {
        int pieceIndex = byteArrToInteger(inputChannel.readNBytes(ConstantFields.PIECE_INDEX));
        byte[] pieceByteArray = inputChannel.readNBytes(messageLength - ConstantFields.PIECE_INDEX);
        LOGGER.info("{}: Peer {} has downloaded the piece {} from {}", fetchCurrTime(), this.peerNode1Id, pieceIndex, this.peerNode2Id);
        this.fileValuePieces.saveFilePiece(pieceIndex, pieceByteArray);
        bitfieldValue.settingRecdPieceIndex(pieceIndex);
        peerNode1.incrementDownloadRate(this.peerNode2Id);
        broadcastHave(pieceIndex);
        // verify if the peer node is still interested in data
        if (!bitfieldValue.hasInterest(peerBitfield)) {
            sendNotInterested();
        }
        // Check if the node has downloaded rhe complete file
        if (bitfieldValue.allPiecesReceived()) {
            this.fileValuePieces.joinPiecesintoFile();
            LOGGER.info("{}: Peer {} has downloaded the complete file", fetchCurrTime(), this.peerNode1Id);
            broadcastCompleted();
        } else {
            passRequest();
        }
    }

    // handles broadcasting on complete file receival
    // ----------------- CODE IS BREAKING HERE -------------------
    // ------------ HANDLE TERMINATION ----------------------
    private void broadcastCompleted() throws IOException {
        for (TorrentService torrentService : this.peerNode1.getPeerTorrentService().values()) {
            fixedThreadPoolExecutor.execute(new TorrentMessenger(torrentService.outputChannel, getMessageText(ConstantFields.MessageForm.COMPLETED, null)));
        }
        
        if (peerNode1.allPeersComplete()) {
            this.fileValuePieces.discardPiecesFolder();
            this.fixedThreadPoolExecutor.shutdownNow();
            this.scheduledThreadPoolExecutor.shutdown();
            peerNode1.getPeerNodeServer().getServerNodeSocket().close();
            peerNode1.closeSocketNode(this.peerNode2Id);
        }
    }

    public Socket getTorrentSocket() {
        return this.torrentSocket;
    }

    // handles HAVE message
    private void broadcastHave(int pieceIndex) throws IOException {
        for (TorrentService torrentService : this.peerNode1.getPeerTorrentService().values()) {
            this.fixedThreadPoolExecutor.execute(new TorrentMessenger(torrentService.outputChannel, getMessageText(ConstantFields.MessageForm.HAVE, integerToByteArray(pieceIndex))));
        }
    }

    //handles request for file pieces
    private void handleRequest(int messageSize) throws IOException {
        // if peer is unchocked then only accept the piece
        if (this.peerNode1.checkIfPeerIsUnchoked(this.peerNode2Id)) {
            // Construct and send piece message
            int pieceIdx = byteArrToInteger(inputChannel.readNBytes(messageSize));
            byte[] pieceArr = this.fileValuePieces.getFilePiece(pieceIdx);
            byte[] pieceMsg = retrivePieceMsg(pieceIdx, pieceArr);
            fixedThreadPoolExecutor.execute(new TorrentMessenger(this.outputChannel, pieceMsg));
        }
    }

    public static byte[] retrivePieceMsg(int pieceIndex, byte[] pieceByteDataArr) {
        byte[] pieceMsgPayload = getPieceMsgData(pieceIndex, pieceByteDataArr);
        byte[] pieceMsg = getMessageText(ConstantFields.MessageForm.PIECE, pieceMsgPayload);
        return pieceMsg;
    }

    private static byte[] getPieceMsgData(int pieceIndex, byte[] pieceByteDataArr) {
        byte[] pieceMsgData = new byte[ConstantFields.PIECE_INDEX + pieceByteDataArr.length];
        int counter = mergeSecondToFirstArr(pieceMsgData, integerToByteArray(pieceIndex), 0);
        mergeSecondToFirstArr(pieceMsgData, pieceByteDataArr, counter);
        return pieceMsgData;
    }


    // handles BITFIELD message
    private void handleBitfield(int messageSize) throws IOException {
        BitSet peerBitfield = BitSet.valueOf(inputChannel.readNBytes(messageSize));
        this.peerBitfield = peerBitfield;
        this.peerNode1.updateBitField(this.peerNode2Id, this.peerBitfield);
        // Send interested message
        if (this.bitfieldValue.hasInterest(this.peerBitfield)) {
            sendInterested();
        }
    }

    // handles INTERESTED message
    private void sendInterested() {
        this.fixedThreadPoolExecutor.execute(new TorrentMessenger(this.outputChannel, getMessageText(ConstantFields.MessageForm.INTERESTED, null)));
    }

    // handles HAVE message
    private void handleHave(int messageSize) throws IOException {
        int pieceIndex = byteArrToInteger(inputChannel.readNBytes(messageSize));
        LOGGER.info("{}: Peer {} received the 'have' message from {}", fetchCurrTime(), this.peerNode1Id, this.peerNode2Id);
        this.peerBitfield.set(pieceIndex);
        // Check if the node is interested in the peer data
        if(this.bitfieldValue.hasInterest(this.peerBitfield)) {
            this.fixedThreadPoolExecutor.execute(new TorrentMessenger(this.outputChannel, getMessageText(ConstantFields.MessageForm.INTERESTED, null)));
        }
    }

    // handles NOTINTERESTED message
    private void handleNotInterested() {
        LOGGER.info("{}: Peer {} received the 'not interested' message from {}", fetchCurrTime(), this.peerNode1Id, this.peerNode2Id);
        this.peerNode1.removeInterestedNeighboringPeers(this.peerNode2Id);
    }

    // handles INTEREDTED message
    private void handleInterested() {
        LOGGER.info("{}: Peer {} received the 'interested' message from {}", fetchCurrTime(), this.peerNode1Id, this.peerNode2Id);
        this.peerNode1.insertInterestedNeighboringPeers(this.peerNode2Id);
    }

    // handles CHOKE message
    private void handleChoke() {
        LOGGER.info("{}: Peer {} is choked by {}", fetchCurrTime(), this.peerNode1Id, this.peerNode2Id);
        this.isChoked = true;
    }

    // handles UNCHOKE message
    private void handleUnchoke() {
        LOGGER.info("{}: Peer {} is unchoked by {}", fetchCurrTime(), this.peerNode1Id, this.peerNode2Id);
        this.isChoked = false;
        passRequest();
    }

    // handles file requests
    private void passRequest() {
        // Send request only for the unchoked node
        if (!this.isChoked) {
            int nxtInterestedPieceIdx = bitfieldValue.fetchIndexOfNextInterestedPiece(peerBitfield);
            if (nxtInterestedPieceIdx != -1) {
                bitfieldValue.addingInPiecesReq(nxtInterestedPieceIdx);
                fixedThreadPoolExecutor.execute(new TorrentMessenger(this.outputChannel, getMessageText(ConstantFields.MessageForm.REQUEST, integerToByteArray(nxtInterestedPieceIdx))));
            } else {
                sendNotInterested();
            }
        }
    }

    // sends Not interested message
    private void sendNotInterested() {
        this.fixedThreadPoolExecutor.execute(new TorrentMessenger(this.outputChannel, getMessageText(ConstantFields.MessageForm.NOT_INTERESTED, null)));
    }

    // sends BitField message
    private void sendBitfield() {
        try {
            this.bitfieldValue.readLock();
            byte[] bitfield = this.bitfieldValue.getBitval().toByteArray();
            this.fixedThreadPoolExecutor.execute(new TorrentMessenger(this.outputChannel, getMessageText(ConstantFields.MessageForm.BITFIELD, bitfield)));
        } catch (Exception excep) {
            excep.printStackTrace();
        } finally {
            this.bitfieldValue.readUnlock();
        }
    }

    private void handshakeReceival() throws Exception {
        byte[] responseData = inputChannel.readNBytes(ConstantFields.HEADER_LENGTH);
        String responseHeader = new String(Arrays.copyOfRange(responseData, ConstantFields.HEADER_FRONT, ConstantFields.HEADER_FRONT + ConstantFields.HEADER_FIELD), StandardCharsets.UTF_8);
        int peerNode2IdVal = byteArrToInteger(Arrays.copyOfRange(responseData, ConstantFields.PEER_ID_FRONT, ConstantFields.PEER_ID_FRONT + ConstantFields.PEER_ID_FIELD));
        // Check if the handshake response message has correct header
        if (!responseHeader.equals(ConstantFields.HEADER)) {
            // Invalid hanshake response message header
            throw new IllegalArgumentException(String.format("Peer %d received invalid handshake message header (%s) from %d", responseHeader, peerNode2IdVal));
        }
        if (this.startedHandshake) {
            // validating if the peer id in response is correct
            if (peerNode2IdVal != this.peerNode2Id) {
                throw new IllegalArgumentException(String.format("Peer %d received invalid peer id (%d) in the handshake response", peerNode1Id, peerNode2IdVal));
            }
            LOGGER.info("{}: Peer {} makes a connection to Peer {}", fetchCurrTime(), this.peerNode1Id, this.peerNode2Id);
        } else {
            // Update peerNode2Id with received peerNode2Id
            this.peerNode2Id = peerNode2IdVal;
            startHandshake();
            LOGGER.info("{}: Peer {} is connected from Peer {}", fetchCurrTime(), this.peerNode1Id, this.peerNode2Id);
        }
    }

    private static String getResponseHeader(byte[] responseData) {
        return new String(Arrays.copyOfRange(responseData, ConstantFields.HEADER_FRONT, ConstantFields.HEADER_FRONT + ConstantFields.HEADER_FIELD), StandardCharsets.UTF_8);
    }

    public static int byteArrToInteger(byte[] byteArr) {
        return ByteBuffer.wrap(byteArr).getInt();

//        return ByteBuffer.wrap(Arrays.copyOfRange(responseData, ConstantFields.PEER_ID_FRONT, ConstantFields.PEER_ID_FRONT + ConstantFields.PEER_ID_FIELD)).getInt();
    }

    // performs handshake
    private void startHandshake() throws Exception {
        this.fixedThreadPoolExecutor.execute(new TorrentMessenger(this.outputChannel, retrieveHandshakeMessage(this.peerNode1Id)));
    }

    public static byte[] retrieveHandshakeMessage(int peerId) {
        byte[] msg = new byte[ConstantFields.HEADER_FIELD + ConstantFields.PEER_ZERO_BITS_FIELD + ConstantFields.PEER_ID_FIELD];
        int ctr = mergeSecondToFirstArr(msg, ConstantFields.HEADER.getBytes(), 0);
        for (int i=0; i<10; i++) {
            msg[ctr++] = 0;
        }
        ctr = mergeSecondToFirstArr(msg, integerToByteArray(peerId), ctr);
        return msg;
    }

    public static int mergeSecondToFirstArr(byte[] arr1, byte[] arr2, int startIndex) {
        for (byte byteData : arr2) {
            arr1[startIndex++] = byteData;
        }
        return startIndex;
    }

    public static byte[] integerToByteArray(int id) {
        return ByteBuffer.allocate(4).putInt(id).array();
    }

    // sends message to the neighbors
    public void pingNeighborWithMessage(ConstantFields.MessageForm messageForm) {
        try {
            this.fixedThreadPoolExecutor.execute(
                    createNewTorrentMessenger(messageForm)
            );
        } catch (IOException excep) {
            excep.printStackTrace();
        }
    }

    //creates new torrent messager
    private TorrentMessenger createNewTorrentMessenger(ConstantFields.MessageForm messageForm) throws IOException {
        return new TorrentMessenger(this.torrentSocket.getOutputStream(), getMessageText(messageForm, new byte[0]));
    }

    // gets the message
    public static byte[] getMessageText(ConstantFields.MessageForm messageType, byte[] messageContentPayload) {
        int messageTextLength = messageContentPayload != null ? messageContentPayload.length : 0;
        byte[] messageData = new byte[ConstantFields.MESSAGE_LENGTH_FIELD_INDEX + ConstantFields.MESSAGE_TYPE_FIELD_INDEX + messageTextLength];
        int messageCounter = mergeSecondToFirstArr(messageData, convertIntegerToByteCollection(messageTextLength), 0);
        messageData[messageCounter++] = (byte) messageType.getMessageVal();
        if (messageTextLength > 0) {
            mergeSecondToFirstArr(messageData, messageContentPayload, messageCounter);
        }
        return messageData;
    }

    //valiate the message length
    public static boolean checkIfMessageLengthIsValid(int messageTextLength){
        return messageTextLength>0;
    }

    // merge one array with another array
    public static int combineMsg2InMsg1(byte[] msgData1, byte[] msgData2, int initialIndex) {
        for (byte msgContent : msgData2) {
            msgData1[initialIndex++] = msgContent;
        }
        return initialIndex;
    }

    //converts Integer length To a Byte array
    public static byte[] convertIntegerToByteCollection(int length) {
        return ByteBuffer.allocate(4).putInt(length).array();
    }

    public static String fetchCurrTime() {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(LocalDateTime.now());
    }


    @Override
    public void run() {
        // begin the handshake if we know both of the service's peerIds.
        if (this.peerNode2Id!=0 && this.peerNode1Id != this.peerNode2Id) {
            try {
                startHandshake();
                this.startedHandshake = true;
            } catch (Exception excep) {
                excep.printStackTrace();
            }
        }
        // Receive handshake
        try {
            handshakeReceival();
        } catch (Exception excep) {
            excep.printStackTrace();
        }
        // Send bitfield message
        sendBitfield();
        peerNode1.addPeerTorrentService(this.peerNode2Id, this);
        // listen to other incoming requests or messages
        try {
            while (true) {
                byte[] messageHeaders = inputChannel.readNBytes(5);
                if (messageHeaders.length > 0) {
                    int messageSize = byteArrToInteger(Arrays.copyOfRange(messageHeaders, ConstantFields.MESSAGE_LENGTH_FRONT_INDEX, ConstantFields.MESSAGE_LENGTH_FRONT_INDEX + ConstantFields.MESSAGE_LENGTH_FIELD_INDEX));
                    ConstantFields.MessageForm messageType = ConstantFields.MessageForm.getMessageFormByValue((int) messageHeaders[ConstantFields.MESSAGE_TYPE_FRONT_INDEX]);
                    if (messageType != null) {
                        switch (messageType) {
                            case CHOKE:
                                handleChoke();
                                break;
                            case UNCHOKE:
                                handleUnchoke();
                                break;
                            case INTERESTED:
                                handleInterested();
                                break;
                            case NOT_INTERESTED:
                                handleNotInterested();
                                break;
                            case HAVE:
                                handleHave(messageSize);
                                break;
                            case BITFIELD:
                                handleBitfield(messageSize);
                                break;
                            case REQUEST:
                                handleRequest(messageSize);
                                break;
                            case PIECE:
                                handlePiece(messageSize);
                                break;
                            case COMPLETED:
                                handleCompleted();
                                break;
                        }
                    }
                }
            }
        } catch (Exception excep) {
            //  excep.printStackTrace();
        }
    }

}
