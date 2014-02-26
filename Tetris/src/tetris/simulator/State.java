package tetris.simulator;

import java.util.Arrays;

public class State 
{
	public static final int COLS = 10;
	public static final int ROWS = 21;
	public static final int N_PIECES = 7;

	public boolean lost;
	
	//current turn
	private int turn;
	private int cleared;
	
	//each square in the grid - 0 means empty - other values mean the type of piece it was placed
	private int[][] field;
	//top row+1 of each column
	//0 means empty
	private int[] top;
	
	//number of next piece
	protected int nextPiece;
	
	//all legal moves - first index is piece type - then a list of 2-length arrays
	public static int[][][] legalMoves = new int[N_PIECES][][];
	
	//indices for legalMoves
	public static final int ORIENT = 0;
	public static final int SLOT = 1;
	
	//possible orientations for a given piece type
	public static int[] pOrients = {1,2,4,4,4,2,2};
	

	public int getLandingHeight(int orient, int slot)
	{
		int height = top[slot]-pBottom[nextPiece][orient][0];
		return height;
	}
	
	//the next several arrays define the piece vocabulary in detail
	//width of the pieces [piece ID][orientation]
	public static int[][] pWidth = {
			{2},
			{1,4},
			{2,3,2,3},
			{2,3,2,3},
			{2,3,2,3},
			{3,2},
			{3,2}
	};
	//height of the pieces [piece ID][orientation]
	public static int[][] pHeight = {
			{2},
			{4,1},
			{3,2,3,2},
			{3,2,3,2},
			{3,2,3,2},
			{2,3},
			{2,3}
	};
	public static int[][][] pBottom = {
		{{0,0}},
		{{0},{0,0,0,0}},
		{{0,0},{0,1,1},{2,0},{0,0,0}},
		{{0,0},{0,0,0},{0,2},{1,1,0}},
		{{0,1},{1,0,1},{1,0},{0,0,0}},
		{{0,0,1},{1,0}},
		{{1,0,0},{0,1}}
	};
	public static int[][][] pTop = {
		{{2,2}},
		{{4},{1,1,1,1}},
		{{3,1},{2,2,2},{3,3},{1,1,2}},
		{{1,3},{2,1,1},{3,3},{2,2,2}},
		{{3,2},{2,2,2},{2,3},{1,2,1}},
		{{1,2,2},{3,2}},
		{{2,2,1},{2,3}}
	};
	
	//initialize legalMoves
	{
		//for each piece type
		for(int i = 0; i < N_PIECES; i++) {
			//figure number of legal moves
			int n = 0;
			for(int j = 0; j < pOrients[i]; j++) {
				//number of locations in this orientation
				n += COLS+1-pWidth[i][j];
			}
			//allocate space
			legalMoves[i] = new int[n][2];
			//for each orientation
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
	
	public int[] getTop() {
		return top;
	}
	
	public int[][] getField() {
		return field;
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
	
	//constructor
	public State() {
		lost = false;
		turn = 0;
		cleared = 0;
		
		field = new int[ROWS][COLS];
		top = new int[COLS];

		nextPiece = randomPiece();
	}

	//copy constructor
	public State(State other) {
		lost = other.lost;
		turn = other.turn;
		cleared = other.cleared;

		field = new int[ROWS][];
		for (int r = 0; r < ROWS; r++)
			field[r] = other.field[r].clone();

		top = other.top.clone();

		nextPiece = other.nextPiece;
	}
	
	//random integer, returns 0-6
	private int randomPiece() {
		return (int)(Math.random()*N_PIECES);
	}
	
	//gives legal moves for next piece
	public int[][] legalMoves() {
		return legalMoves[nextPiece];
	}
	
	//make a move based on the move index - its order in the legalMoves list
	public void makeMove(int move) {
		makeMove(legalMoves[nextPiece][move]);
	}
	
	public int getOrient(int move)
	{
		return legalMoves[nextPiece][move][ORIENT];
	}
	
	public int getSlot(int move)
	{
		return legalMoves[nextPiece][move][SLOT];
	}
	
	//make a move based on an array of orient and slot
	public void makeMove(int[] move) {
		makeMove(move[ORIENT],move[SLOT]);
	}
	
	//returns false if you lose - true otherwise
	public boolean makeMove(int orient, int slot) {
		turn++;
		//height if the first column makes contact
		int height = top[slot]-pBottom[nextPiece][orient][0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pWidth[nextPiece][orient];c++) {
			height = Math.max(height,top[slot+c]-pBottom[nextPiece][orient][c]);
		}
		
		//check if game ended
		if(height+pHeight[nextPiece][orient] >= ROWS) {
			lost = true;
			return false;
		}

		
		//for each column in the piece - fill in the appropriate blocks
		for(int i = 0; i < pWidth[nextPiece][orient]; i++) {
			
			//from bottom to top of brick
			for(int h = height+pBottom[nextPiece][orient][i]; h < height+pTop[nextPiece][orient][i]; h++) {
				field[h][i+slot] = nextPiece + 1;//turn;
			}
		}
		
		//adjust top
		for(int c = 0; c < pWidth[nextPiece][orient]; c++) {
			top[slot+c]=height+pTop[nextPiece][orient][c];
		}
		
		int rowsCleared = 0;
		
		//check for full rows - starting at the top
		for(int r = height+pHeight[nextPiece][orient]-1; r >= height; r--) {
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
	
	public String toString() {
		String s = "";
		//		System.out.print(field);

		s += "turn: " + turn + "  cleared: " + cleared +
			"  next: " + nextPiece + "  lost: " + lost + "\n";
		s += "field:\n";
		for (int r = ROWS-1; r >= 0; r--) {
			s += Arrays.toString(field[r]) + "\n";
		}

		s += "\ntop:\n" + Arrays.toString(top) + "\n";
		return s;
	}
}


