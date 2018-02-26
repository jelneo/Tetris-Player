import java.util.Scanner;

public class PlayerSkeleton {

	private static boolean visualMode = false;
	private static final int DATA_SIZE = 30;

	//implement this function to have a working system
	/**
	 * Picks the move with the highest value.
	 *
	 * @param s - present state
	 * @param legalMoves - List of legal moves
	 * @return the move that has the maximum value based on
	 * {@link PlayerSkeleton#simulateMove(State, int[]) simulateMove} method
	 */
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
		setVisualMode();

		int maxScore = Integer.MIN_VALUE;
		int minScore = Integer.MAX_VALUE;
		int sum = 0;
		int counter = DATA_SIZE; // set to 30 for more accurate sample size
		while(counter-- > 0) {
			State s = new State();

			if (visualMode) {
				visualize(s);
			} else {
				PlayerSkeleton p = new PlayerSkeleton();
				while (!s.hasLost()) {
					s.makeMove(p.pickMove(s, s.legalMoves()));
				}
			}

			maxScore = Math.max(maxScore, s.getRowsCleared());
			minScore = Math.min(minScore, s.getRowsCleared());
			sum += s.getRowsCleared();
			System.out.println("You have completed " + s.getRowsCleared() + " rows.");

		}
		System.out.println("Ave: " + (sum / DATA_SIZE));
		System.out.println("Min: " + minScore);
		System.out.println("Max: " + maxScore);
	}

	private static void setVisualMode() {
		Scanner sc = new Scanner(System.in);
		System.out.println("Visual Mode? 1 for yes, 0 for no");
		visualMode = sc.nextInt() == 1;
		sc.close();
	}

	private static void visualize(State s) {
		TFrame window = new TFrame(s);
		PlayerSkeleton p = new PlayerSkeleton();

		while (!s.hasLost()) {
			s.makeMove(p.pickMove(s, s.legalMoves()));

			if (visualMode) {
				s.draw();
				s.drawNext(0, 0);
			}

			// This creates a delay, making it harder to test multiple tests
			/*
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			*/
		}
		window.dispose();
	}
}

// Creates a simulated state to evaluate the value of doing a move without changing the actual game state

/**
 * {@code SimulatedState} is a simulated state. This helps to evaluate the value of performing a move without altering
 * the game state.
 */
class SimulatedState extends State {

	/********************************* Multipliers to determine value of simulated move *********************************/
	// Heavily prioritise objective of row clearing.
	private static float rowsClearedMult = 10f;

	// Multipliers used for tiebreakers.
	private static float glitchCountMult = -0.1f;
	private static float bumpinessMult = -0.1f;
	private static float totalHeightMult = -0.5f;
	private static float maxHeightMult = -0.1f;

	/********************************* End of multipliers *********************************/

	private int field[][];
	private int top[];

	public SimulatedState (State s) {
		// initialize field
		field = new int[s.getField().length][s.getField()[0].length];
		for (int i = 0; i < field.length; i++) {
			for (int j = 0; j < field[0].length; j++) {
				field[i][j] = s.getField()[i][j];
			}
		}

		// initialize top
		top = new int[s.getTop().length];
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

		// Check if game ended - penalize heavily.
		if(height+State.getpHeight()[nextPiece][orient] >= ROWS) {
			return Integer.MIN_VALUE;
		}

		/********************************* Perform simulation of adding piece *********************************/
		/********************************* Please ignore this chunk (unless necessary) *********************************/
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
		/********************************* End of simulation *********************************/

		int maxHeight = 0;

		for (int i = 0; i < top.length; i++) {
			if (top[i] > maxHeight) {
				maxHeight = top[i];
			}
		}

		return bumpinessMult * getBumpiness(top) + totalHeightMult * getTotalHeight(top) + rowsClearedMult * rowsCleared
				+ maxHeightMult * maxHeight + glitchCountMult * getGlitchCount(field);
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
