
public class PlayerSkeleton {

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

	// Simulates a move and returns a float that allows for evaluation. The higher the better.
	public float simulateMove(State s, int[] move) {
		SimulatedState ss = new SimulatedState(s);
		return ss.getMoveValue(move);
    }

	public static void main(String[] args) {
		int maxScore = Integer.MIN_VALUE;
		int minScore = Integer.MAX_VALUE;
		int sum = 0;
		int counter = 10;
		while(counter-- > 0) {
			State s = new State();
			new TFrame(s);
			PlayerSkeleton p = new PlayerSkeleton();
			while (!s.hasLost()) {
				s.makeMove(p.pickMove(s, s.legalMoves()));
				s.draw();
				s.drawNext(0, 0);
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			maxScore = Math.max(maxScore, s.getRowsCleared());
			minScore = Math.min(minScore, s.getRowsCleared());
			sum += s.getRowsCleared();
			System.out.println("You have completed " + s.getRowsCleared() + " rows.");
		}
		System.out.println("Ave: " + (sum/10));
		System.out.println("Min: " + minScore);
		System.out.println("Max: " + maxScore);
	}

}

// Creates a simulated state to evaluate the value of doing a move without changing the actual game state
class SimulatedState extends State {

	// Multipliers. Heavily prioritise row clearing, the rest are tie-breakers
	private static float rowsClearedMult = 10f;
	private static float glitchCoundMult = -0.1f;
	private static float bumpinessMult = -0.1f;
	private static float totalHeightMult = -0.5f;
	private static float maxHeightMult = -0.1f;

	private int field[][];
	private int top[];

	public SimulatedState (State s) {
		field = new int[s.getField().length][s.getField()[0].length];
		top = new int[s.getTop().length];
		for (int i = 0; i < field.length; i++) {
			for (int j = 0; j < field[0].length; j++) {
				field[i][j] = s.getField()[i][j];
			}
		}
		for (int i = 0; i < top.length; i++) {
			top[i] = s.getTop()[i];
		}
		nextPiece = s.getNextPiece();
	}

	// Returns the value of making a move
	public float getMoveValue(int move[]) {
		return getMoveValue(move[ORIENT], move[SLOT]);
	}

	public float getMoveValue(int orient, int slot) {
		//height if the first column makes contact
		int height = top[slot]-getpBottom()[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-State.getpBottom()[nextPiece][orient][c]);
		}

		//check if game ended
		if(height+State.getpHeight()[nextPiece][orient] >= ROWS) {
			return Integer.MIN_VALUE;
		}


		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {

			//from bottom to top of brick
			for(int h = height+State.getpBottom()[nextPiece][orient][i]; h < height+State.getpTop()[nextPiece][orient][i]; h++) {
				field[h][i+slot] = 1;
			}
		}

		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+State.getpTop()[nextPiece][orient][c];
		}

		int rowsCleared = 0;

		//check for full rows - starting at the top
		for(int r = height+State.getpHeight()[nextPiece][orient]-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < COLS; c++) {
				if(field[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				rowsCleared++;
				//for each column
				for(int c = 0; c < COLS; c++) {

					//slide down all bricks
					for(int i = r; i < top[c]; i++) {
						field[i][c] = field[i+1][c];
					}
					//lower the top
					top[c]--;
					while(top[c]>=1 && field[top[c]-1][c]==0)
						top[c]--;
				}
			}
		}

		int maxHeight = 0;

		for (int i = 0; i < top.length; i++) {
			if (top[i] > maxHeight) {
				maxHeight = top[i];
			}
		}

		return bumpinessMult * getBumpiness(top) + totalHeightMult * getTotalHeight(top) + rowsClearedMult * rowsCleared
				+ maxHeightMult * maxHeight + glitchCoundMult * getGlitchCount(field);
	}

	// Checks for how bumpy the top is
	public float getBumpiness(int[] top) {
		float bumpiness = 0;
		for (int i = 0; i < top.length - 1; i++) {
			bumpiness += Math.abs(top[i] - top[i + 1]);
		}
		return bumpiness;
	}

	// Returns the sum of heights
	public float getTotalHeight(int[] top) {
		float totalHeight = 0;
		for (int i = 0; i < top.length; i++) {
			totalHeight += top[i];
		}
		return totalHeight;
	}

	public float getGlitchCount(int[][] field) {
		float glitchCount = 0;
		for (int r = 1; r < field.length; r++) {
			for(int c = 0; c < field[r].length; c++) {
				if ((field[r][c] == 0) && (field[r - 1][c] != 0)) {
					glitchCount++;
				}
			}
		}
		return glitchCount;
	}

}
