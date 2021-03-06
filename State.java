import java.awt.Color;

/**
 * State contains Tetris simulation. It keeps track of the state and allows you to make moves.
 * Moves are defined by two numbers: the SLOT (leftmost column of the piece) and the ORIENT (the orientation of the piece).
 */
public class State {
	public static final int COLS = 10;
	public static final int ROWS = 21;
	public static final int N_PIECES = 7;

	public boolean lost = false;

	public TLabel label;

	// Present turn
	private int turn = 0;
	private int cleared = 0;

  // 0 means that the square is empty. Non-zero values denote the turn that the square was filled by a piece.
	private int[][] field = new int[ROWS][COLS];

	// (top row + 1) of each column. 0 indicates empty top row.
	private int[] top = new int[COLS];

	// Id number of next piece (Technically the piece you are making the move with)
	protected int nextPiece;

	/********************************* List of Possible Moves *********************************/
	// All legal moves - first index is piece id - then a list of 2-length arrays
	protected static int[][][] legalMoves = new int[N_PIECES][][];
	
	// Indices for legalMoves
	public static final int ORIENT = 0;
	public static final int SLOT = 1;
	
	// Possible orientations for a given piece type
	protected static int[] pOrients = {1,2,4,4,4,2,2};

	// Width of the pieces [piece ID][orientation]
	protected static int[][] pWidth = {
			{2},
			{1,4},
			{2,3,2,3},
			{2,3,2,3},
			{2,3,2,3},
			{3,2},
			{3,2}
	};

	//height of the pieces [piece ID][orientation]
	private static int[][] pHeight = {
			{2},
			{4,1},
			{3,2,3,2},
			{3,2,3,2},
			{3,2,3,2},
			{2,3},
			{2,3}
	};

	private static int[][][] pBottom = {
		{{0,0}},
		{{0},{0,0,0,0}},
		{{0,0},{0,1,1},{2,0},{0,0,0}},
		{{0,0},{0,0,0},{0,2},{1,1,0}},
		{{0,1},{1,0,1},{1,0},{0,0,0}},
		{{0,0,1},{1,0}},
		{{1,0,0},{0,1}}
	};

	private static int[][][] pTop = {
		{{2,2}},
		{{4},{1,1,1,1}},
		{{3,1},{2,2,2},{3,3},{1,1,2}},
		{{1,3},{2,1,1},{3,3},{2,2,2}},
		{{3,2},{2,2,2},{2,3},{1,2,1}},
		{{1,2,2},{3,2}},
		{{2,2,1},{2,3}}
	};

	/********************************* List of all Legal moves *********************************/
	{
		// For each piece type
		for(int i = 0; i < N_PIECES; i++) {
			// Figure out number of legal moves
			int n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				// Number of locations in this orientation
				n += COLS + 1 - pWidth[i][j];
			}

			// Allocate space
			legalMoves[i] = new int[n][2];

			// For each orientation
			n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//for each slot
				for(int k = 0; k < COLS+1-pWidth[i][j];k++) {
					legalMoves[i][n][ORIENT] = j;
					legalMoves[i][n][SLOT] = k;
					n++;
				}
			}
		}
	
	}
	
	public int[][] getField() {
		return field;
	}

	public int[] getTop() {
		return top;
	}

    public static int[] getpOrients() {
        return pOrients;
    }
    
    public static int[][] getpWidth() {
        return pWidth;
    }

    public static int[][] getpHeight() {
        return pHeight;
    }

    public static int[][][] getpBottom() {
        return pBottom;
    }

    public static int[][][] getpTop() {
        return pTop;
    }


	public int getNextPiece() {
		return nextPiece;
	}
	
	public boolean hasLost() {
		return lost;
	}
	
	public int getRowsCleared() {
		return cleared;
	}
	
	public int getTurnNumber() {
		return turn;
	}

	// Constructor
	public State() {
		nextPiece = randomPiece();
	}

	/********************************* Simulation methods *********************************/

	// Random integer, returns 0-6
	private int randomPiece() {
		return (int)(Math.random() * N_PIECES);
	}
	
	// Gives legal moves for
	public int[][] legalMoves() {
		return legalMoves[nextPiece];
	}
	
	// Make a move based on the move index - its order in the legalMoves list
	public void makeMove(int move) {
		makeMove(legalMoves[nextPiece][move]);
	}
	
	// Make a move based on an array of orient and slot
	public void makeMove(int[] move) {
		makeMove(move[ORIENT], move[SLOT]);
	}

	/**
	 * Makes a move using the next piece.
	 * First, checks if game has ended when piece is added.
	 * Then, fills board with the piece.
	 * Finally, removes the filled rows with the new piece and select a new piece random piece as the next piece.
	 *
	 * {@param orient} Orientation of the next piece.
	 * {@param slot} leftmost column of the piece.
	 * @return false if you lose. True otherwise.
	 */
	public boolean makeMove(int orient, int slot) {
		turn++;
		// Height of the first column that the piece makes contact with
		int height = top[slot] - pBottom[nextPiece][orient][0];

		// For each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient]; c++) {
			height = Math.max(height, top[slot + c] - pBottom[nextPiece][orient][c]);
		}

		// Check if game ended (i.e. sum of piece + prev height reaches top of board)
		if(height + pHeight[nextPiece][orient] >= ROWS) {
			lost = true;
			return false;
		}

		// For each column in the piece - fill in the appropriate blocks (blocks filled by piece)
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			//from bottom to top of brick
			for(int h = height + pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i + slot] = turn;
			}
		}
		
		// Adjust top (Update after the peice is added)
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c] = height + pTop[nextPiece][orient][c];
		}

		// initial number of rows cleared - incremented as clearing happens
		int rowsCleared = 0;
		
		// Check for full rows - starting at the top
		for(int r = height + pHeight[nextPiece][orient] - 1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < COLS; c++) {
				if (field[r][c] == 0) {
					full = false;
					break;
				}
			}

			//if the row was full - remove it and slide above stuff down
			if (full) {
				rowsCleared++;
				cleared++;
				//for each column
				for(int c = 0; c < COLS; c++) {

					//slide down all bricks
					for(int i = r; i < top[c]; i++) {
						field[i][c] = field[i+1][c];
					}
					//lower the top
					top[c]--;
					while(top[c]>=1 && field[top[c]-1][c]==0)	top[c]--;
				}
			}
		}

		//pick a new piece
		nextPiece = randomPiece();

		return true;
	}

	/********************************* GUI for simulation *********************************/
	/**
	 * Draws the board
	 */
	public void draw() {
		label.clear();
		label.setPenRadius();
		//outline board
		label.line(0, 0, 0, ROWS+5);
		label.line(COLS, 0, COLS, ROWS+5);
		label.line(0, 0, COLS, 0);
		label.line(0, ROWS-1, COLS, ROWS-1);
		
		//show bricks
				
		for(int c = 0; c < COLS; c++) {
			for(int r = 0; r < top[c]; r++) {
				if(field[r][c] != 0) {
					drawBrick(c,r);
				}
			}
		}
		
		for(int i = 0; i < COLS; i++) {
			label.setPenColor(Color.red);
			label.line(i, top[i], i+1, top[i]);
			label.setPenColor();
		}
		
		label.show();
	}
	
	public static final Color brickCol = Color.gray;

	/**
	 * Draws a unit block of the box.
	 */
	private void drawBrick(int c, int r) {
		label.filledRectangleLL(c, r, 1, 1, brickCol);
		label.rectangleLL(c, r, 1, 1);
	}

	/**
	 * Draws the next piece above the board.
	 */
	public void drawNext(int slot, int orient) {
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			for(int j = pBottom[nextPiece][orient][i]; j <pTop[nextPiece][orient][i]; j++) {
				drawBrick(i+slot, j+ROWS+1);
			}
		}

		label.show();
	}

	/**
	 * Visualization - Clears the drawing of the next piece so it can be drawn in a different slot/orientation.
	 */
	public void clearNext() {
		label.filledRectangleLL(0, ROWS+.9, COLS, 4.2, TLabel.DEFAULT_CLEAR_COLOR);
		label.line(0, 0, 0, ROWS+5);
		label.line(COLS, 0, COLS, ROWS+5);
	}

}
