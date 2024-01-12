import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;


public class FileValuePieces {
    int fileValuePieceId;
    CommonConfig dataCommonCfg;
    String commonCfgFileName;
    int pieceCount;
    int sizeOfFile;
    int sizeOfPiece;
    String piecesLocationPath;
    String fileLocationPath;

    public FileValuePieces(int fileValuePieceId, CommonConfig dataCommonCfg) {
        this.fileValuePieceId = fileValuePieceId;
        this.dataCommonCfg = dataCommonCfg;
        this.commonCfgFileName = dataCommonCfg.getCommonCfgFileName();
        this.sizeOfFile = dataCommonCfg.getSizeOfFile();
        this.sizeOfPiece = dataCommonCfg.getSizeOfPiece();
        this.pieceCount = dataCommonCfg.getPieceCount();

        this.piecesLocationPath = Paths.get(ConstantFields.WORKING_LOC, String.format("peer_%d", this.fileValuePieceId), "temp").toString();
        try {
            Files.createDirectories(Paths.get(this.piecesLocationPath));
        } catch (Exception excep) {
            excep.printStackTrace();
        }
        this.fileLocationPath = Paths.get(ConstantFields.WORKING_LOC, String.format("peer_%d", this.fileValuePieceId), this.commonCfgFileName).toString();
    }

    public void divideFileIntoChunks() {
        try {
            FileInputStream inputChannel = new FileInputStream(this.fileLocationPath);
            int pieceFront = 0;
            for (int pIdx = 0; pIdx < this.pieceCount; pIdx++) {
                int newPieceFront = pieceFront + this.sizeOfPiece;
                int sizeOfPiece = this.sizeOfPiece;
                // In case of the last piece, adjust the newPieceStart and pieceLength
                if (this.sizeOfFile < newPieceFront) {
                    newPieceFront = this.sizeOfFile;
                    sizeOfPiece = this.sizeOfFile - pieceFront;
                }
                byte[] pieceByteArray = new byte[sizeOfPiece];
                inputChannel.read(pieceByteArray);
                String piecePath = Paths.get(this.piecesLocationPath, String.format("%s_%d", this.commonCfgFileName, pIdx)).toString();
                FileOutputStream outputChannel = new FileOutputStream(piecePath);
                outputChannel.write(pieceByteArray);
                outputChannel.close();
                pieceFront = newPieceFront;
            }
            inputChannel.close();
        } catch (Exception excep) {
            excep.printStackTrace();
        }
    }

    public byte[] getFilePiece(int pieceIndex) throws IOException {
        String piecePath = Paths.get(this.piecesLocationPath, String.format("%s_%d", this.commonCfgFileName, pieceIndex)).toString();
        FileInputStream inputChannel = new FileInputStream(piecePath);
        int pieceLength = (int) inputChannel.getChannel().size();
        byte[] pieceByteArray = new byte[pieceLength];
        inputChannel.read(pieceByteArray);
        inputChannel.close();
        return pieceByteArray;
    }

    public void saveFilePiece(int pieceIndex, byte[] pieceByteArray) throws IOException {
        String piecePath = Paths.get(this.piecesLocationPath, String.format("%s_%d", this.commonCfgFileName, pieceIndex)).toString();
        FileOutputStream outputChannel = new FileOutputStream(piecePath);
        outputChannel.write(pieceByteArray);
        outputChannel.close();
    }

    public void joinPiecesintoFile() throws IOException {
        FileOutputStream outputChannel = new FileOutputStream(this.fileLocationPath);
        File[] splitFiles = new File[pieceCount];
        for(int pieceIndex = 0; pieceIndex < pieceCount; pieceIndex++) {
            String piecePath = Paths.get(this.piecesLocationPath, String.format("%s_%d", this.commonCfgFileName, pieceIndex)).toString();
            splitFiles[pieceIndex] = new File(piecePath);
        }
        for(int pieceIndex = 0; pieceIndex < pieceCount; pieceIndex++) {
            FileInputStream inputChannel = new FileInputStream(splitFiles[pieceIndex]);
            int chunkFileLength = (int)splitFiles[pieceIndex].length();
            byte[] readChunkFile = new byte[chunkFileLength];
            inputChannel.read(readChunkFile);
            outputChannel.write(readChunkFile);
            inputChannel.close();
        }
        outputChannel.close();
    }

    public void discardPiecesFolder() throws IOException {
        try {
            if (Files.exists(Paths.get(piecesLocationPath))) {
                discardFolder(piecesLocationPath);
                // Files.delete(Paths.get(piecesLocationPath));
            }
        } catch (Exception excep) {
            excep.printStackTrace();
        }
    }

    public static void discardFolder(String location) throws IOException
    {
        Files
                .walk(Paths.get(location))
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
