# P2PFileSharingBitTorrentProtocol

**Project Description:**   
The project involves implementing a peer-to-peer (P2P) file sharing system with various components such as peer server, peer client, and schedulers. The system follows a sequence of steps to establish connections, exchange information, and share pieces of a file among peers. Here's a breakdown of the project's working:

1. Configuration and Peer Initialization:

- Peer processes read configurations and initiate the peer process.
- Both server and client components are started, forming connection endpoints.
  
2. Handshake and Bitfield Exchange:

- Once the endpoints are created, a handshake message is sent to other peers via the endpoint.
- Subsequently, the bitfield, indicating available pieces, is exchanged among peers.
  
3. Interest Signaling:

- If a peer has interest in a specific piece, it sends an INTERESTED message to the relevant peers.

4. Neighbor Selection:

Schedulers are employed to choose preferred and optimistic neighbors.

5. Unchoking and Choking:

- UNCHOKE messages are sent to selected preferred and optimistic neighbors.
- CHOKE messages are sent to unselected remaining neighbors.
  
6. Piece Request and Transmission:

- Peers check if unchoked neighbors have any interested pieces.
- If so, a REQUEST message is sent for the interested piece.

7. Piece Reception and Update:

- When a peer receives a piece, it sends a PIECE message if the sender is unchoked.
- The bitfield is updated, and a HAVE message is sent to inform others about the received piece.
  
8. Interest Management:

- Upon receiving a HAVE message, a peer checks if it has any remaining interested pieces.
- If not, it sends a NOT INTERESTED message; otherwise, it requests the next piece.

9. File Completion:

- If a peer acquires all the pieces, it assembles them to reconstruct the complete file.
   
**Steps to run the file:** 
1. Unzip the folder 'BhaktiArmishKantariya.zip'
2. Login to thunder by ssh to the cise machine with your username
3. Upload the folder to the cise server
4. Navigate to the project sub-directory
5. Use the following command on the terminal to start every peer:
   run.sh ~/P2PFileSharingBitTorrentProtocol peerId

**Bit Torrent Demo:** 
https://uflorida-my.sharepoint.com/:v:/g/personal/b_kantariya_ufl_edu/EdoVN3HHCXdKj2xhIoUA-HgBiiWwKs7gCJHZVNZyh1VLZQ?nav=eyJyZWZlcnJhbEluZm8iOnsicmVmZXJyYWxBcHAiOiJPbmVEcml2ZUZvckJ1c2luZXNzIiwicmVmZXJyYWxBcHBQbGF0Zm9ybSI6IldlYiIsInJlZmVycmFsTW9kZSI6InZpZXciLCJyZWZlcnJhbFZpZXciOiJNeUZpbGVzTGlua0RpcmVjdCJ9fQ&e=HwYsNv
