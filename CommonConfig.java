import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class CommonConfig {

    public int getPrefNeighborsCount() {
        return prefNeighborsCount;
    }

    public int getPieceCount() {
        return pieceCount;
    }

    public int getUnchokingTime() {
        return unchokingTime;
    }

    public int getOptUnchokingTime() {
        return optUnchokingTime;
    }

    public String getCommonCfgFileName() {
        return commonCfgFileName;
    }

    public int getSizeOfFile() {
        return sizeOfFile;
    }

    public int getSizeOfPiece() {
        return sizeOfPiece;
    }

    int prefNeighborsCount;
    int pieceCount;
    int unchokingTime;
    int optUnchokingTime;
    String commonCfgFileName;
    int sizeOfFile;
    int sizeOfPiece;

    Logger LOGGER = LogManager.getLogger(TorrentService.class);

    //setting the prefNeighborsCount
    public void setPrefNeighborsCount(String line) {
        this.prefNeighborsCount = extractValue(line);
    }

    //setting the unchoking time interval
    public void setUnchokingTime(String line) {
        this.unchokingTime = extractValue(line);
    }

    //setting the optimistic unhocking time interval
    public void setOptUnchokingTime(String line) {
        this.optUnchokingTime = extractValue(line);
    }

    //setting the file name for the common config file
    public void setcommonCfgFileName(String line) {
        this.commonCfgFileName = line.split(" ")[1];
    }

    //setting the file size
    public void setFileSize(String line) {
        this.sizeOfFile = extractValue(line);
    }

    //setting the piece size
    public void setPieceSize(String line) {
        this.sizeOfPiece = extractValue(line);
    }

    //computing the piece count using file and piece size
    public void calculatePieceCount() {
        double sizeOfFileInDouble = (double) this.sizeOfFile;
        double sizeOfPieceInDouble = (double) this.sizeOfPiece;
        pieceCount = (int) Math.ceil(sizeOfFileInDouble / sizeOfPieceInDouble);
    }

    //checking if the common config file has any lines
    public boolean isValidConfig(List<String> configLines) {
        return configLines != null && configLines.size() == 6;
    }

    //extracting the value from the config line by splitting it by " "
    public static int extractValue(String line) {
        return Integer.parseInt(line.split(" ")[1]);
    }


    public void parseCommonCfgData(List<String> configLines) {
        if (isValidConfig(configLines)) {
            setPrefNeighborsCount(configLines.get(0));
            setUnchokingTime(configLines.get(1));
            setOptUnchokingTime(configLines.get(2));
            setcommonCfgFileName(configLines.get(3));
            setFileSize(configLines.get(4));
            setPieceSize(configLines.get(5));
            calculatePieceCount();
        }
    }

    public void printCommonConfig(){
        LOGGER.info("\nCommon Config:");

        LOGGER.info("Number of Preferred Neighbors: {}", getPrefNeighborsCount());
        LOGGER.info("Unchoking Interval: {}", getUnchokingTime());
        LOGGER.info("Optimistic Unchoking Interval: {}", getOptUnchokingTime());
        LOGGER.info("File Name: {}", getCommonCfgFileName());
        LOGGER.info("File Size: {}", getSizeOfFile());
        LOGGER.info("Piece Size: {}\n", getSizeOfPiece());
    }
}
