import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

class Piece implements Delayed {
    private int currIndex;
    private LocalDateTime createTime;

    private int prevIndex;

    //constructs the data block piece
    public Piece(int val) {
        this.currIndex = val;
        this.prevIndex = -1;
        this.createTime = LocalDateTime.now().plusSeconds(30);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        LocalDateTime curr = LocalDateTime.now();
        long differe = curr.until(createTime, ChronoUnit.MILLIS);
        return unit.convert(differe, TimeUnit.MILLISECONDS);
    }

    // updates the indexes of the piece block
    public void newIndex(int val){
        this.prevIndex = this.currIndex;
        this.currIndex = val;
    }

    //fetches the currrent piece block index
    public int getCurrIndex() {
        return currIndex;
    }

    //compares the delay time
    @Override
    public int compareTo(Delayed otherIndex) {
        long res = 0;
        try{
            res = this.getDelay(TimeUnit.NANOSECONDS) - otherIndex.getDelay(TimeUnit.NANOSECONDS);
        }
        catch(Exception excep){
            excep.printStackTrace();
        }

        if (res < 0) {
            return -1;
        } else if (res > 0) {
            return 1;
        }

        return 0;
    }

    //compares the message index change
    public int compareChange(){
        int ress = 0;
        int curr = this.currIndex;
        int prev = this.prevIndex;

        try{
            if(prev==-1)
            {
                return -1;
            }
            else{
                ress = curr - prev;
            }
        }
        catch(Exception excep){
            excep.printStackTrace();
        }

        return ress;
    }
}