import java.util.*;

public class TileDistribution {
    static final int NUMBER_OF_TILES = 7;

    private int totalCount = 0;
    private Map<Integer, MutableInt> tileDistribution = new HashMap<>();
    private Random rnd = new Random();

    TileDistribution() {
        for (int i = 0; i < NUMBER_OF_TILES; i++) {
            tileDistribution.put(i, new MutableInt(10));
            totalCount += 10;
        }
    }

    TileDistribution(int[] tiles) {
        totalCount = 0;
        for (int i = 0; i < NUMBER_OF_TILES; i++) {
            tileDistribution.put(i, new MutableInt(tiles[i]));
            totalCount += tiles[i];
        }
    }


    public void increment(Integer tile) {
        totalCount++;
        MutableInt count = tileDistribution.get(tile);
        count.increment();
    }

    public int get(Integer tile) {
        return tileDistribution.get(tile).get();
    }

    public double getFreq(Integer tile) {
        return ((double) get(tile)) / ((double) totalCount);
    }

    public int selectRandomTile() {
        double number = rnd.nextDouble();
        List<Double> freqs = getCumulFreq();

        if (number < freqs.get(0)) {
            return 0;
        } else if (number < freqs.get(1)) {
            return 1;
        } else if (number < freqs.get(2)) {
            return 2;
        } else if (number < freqs.get(3)) {
            return 3;
        } else if (number < freqs.get(4)) {
            return 4;
        } else if (number < freqs.get(5)) {
            return 5;
        } else {
            return 6;
        }
    }

    private List<Double> getCumulFreq() {
        double cumul = 0;
        List<Double> cumulFreq = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_TILES; i++) {
            cumul += getFreq(i);
            cumulFreq.add(i, cumul);
        }

        return cumulFreq;
    }

    class MutableInt {
        private int value = 0; // note that we start at 1 since we're counting

        MutableInt(int input) {
            value = input;
        }

        public void increment() {
            value++;
        }

        public int get() {
            return value;
        }
    }
}

