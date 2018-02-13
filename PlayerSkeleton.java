
public class PlayerSkeleton {

    // Multipliers
    private static float bumpinessMult = (float)-0.3;
    private static float totalHeightMult = (float)-0.2;
    private static float holeFilledMult = (float)0.5;

	//implement this function to have a working system
    // Returns the move that has the maximum value based on simulateMove()
	public int pickMove(State s, int[][] legalMoves) {

		int maxIdx = 0;
		float max = simulateMove(s, legalMoves[0]);
		for (int i = 1; i < legalMoves.length; i++) {
			if (simulateMove(s, legalMoves[i]) > max) {
                maxIdx = i;
                max = simulateMove(s, legalMoves[i]);
            }
		}
		return maxIdx;
	}

	// Simulates a move and returns a float that allows for evaluation. The higher the better
    // Currently does not check whether the move will clear a row
	public float simulateMove(State s, int[] move) {
	    float value;
	    int[] top = s.getTop().clone();
	    int maxHeight = 0;
	    int fillsHole = fillsHole(s, move, top);

	    for (int i = 0; i < s.getpWidth()[s.getNextPiece()][move[0]]; i++) {
	        if (top[i + move[1]] + s.getpHeight()[s.getNextPiece()][move[0]] > maxHeight) {
	            maxHeight = top[i + move[1]] + s.getpHeight()[s.getNextPiece()][move[0]];
            }
        }
        for (int i = 0; i < s.getpWidth()[s.getNextPiece()][move[0]]; i++) {
	        top[i + move[1]] += maxHeight + s.getpHeight()[s.getNextPiece()][move[0]];
        }

	    value = bumpinessMult * getBumpiness(top) + totalHeightMult * getTotalHeight(top) + holeFilledMult * fillsHole;
	    return value;
    }

    public float getBumpiness(int[] top) {
	    float bumpiness = 0;
	    for (int i = 0; i < top.length - 1; i++) {
	        bumpiness += Math.abs(top[i] - top[i + 1]);
        }
	    return bumpiness;
    }

    //returns the sum of heights
    public float getTotalHeight(int[] top) {
        float totalHeight = 0;
        for (int i = 0; i < top.length; i++) {
            totalHeight += top[i];
        }
        return totalHeight;
    }

    //checks whether the move will fill a hole
    //still needs a lot of work
    public int fillsHole (State s, int[] move, int[] top) {
	    try {
            if (top[move[1] - 1] > top[move[1]]) {
                return 1;
            }
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            try {
                if ((top[move[1] + s.getpWidth()[s.getNextPiece()][move[0]]]) > (top[move[1] +
                        s.getpWidth()[s.getNextPiece()][move[0]] - 1])) {
                    return 1;
                }
            } catch (ArrayIndexOutOfBoundsException aioobe2) {
                return 0;
            }
        }
        return 0;
    }

	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			s.draw();
			s.drawNext(0,0);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}
