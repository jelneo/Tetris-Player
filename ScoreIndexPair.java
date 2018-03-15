/**
 * A class to create an object that stores a score and an index
 */
public class ScoreIndexPair implements Comparable<ScoreIndexPair> {
    private float score = 0.0f;
    private int index = 0;

    public ScoreIndexPair(float score, int index){
        this.score = score;
        this.index = index;
    }

    public float getScore() {
        return score;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public int compareTo(ScoreIndexPair other) {
        return (Float.compare(score, other.score));
    }
}
