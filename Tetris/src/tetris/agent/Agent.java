package tetris.agent;

import java.util.*;
import tetris.simulator.State;


public class Agent {

	//implement this function to have a working system
	//Inputs:
	//- s is the current state of the board
	//- legalMoves is a nx2 matrix of the n possible actions for the current piece.
	//	An action is the orientation & column to place the current piece
	//Outputs:
	//- index n of the action to execute in legalMoves
//	public int chooseAction(State s, int[][] legalMoves,double[] w) 
//	{	
//		double maxvalue = -1;
//		int maxind = -1;
//		for(int i = 0; i < legalMoves.length; i++)
//		{
//			State temp = new State(s);
//			temp.makeMove(i);
//			double value = valueFunction(temp,w);
//			if(value > maxvalue || maxind == -1)
//			{
//				maxind = i;
//				maxvalue = value;
//			}
//		}
//		return maxind;
//		
//		
//		
////		return (int)(Math.random()*legalMoves.length); 
//	}
	
	
	public int chooseAction(State s, int[][] legalMoves,double[] w) 
	{	
		double maxvalue = -1;
		int maxind = -1;
		for(int i = 0; i < legalMoves.length; i++)
		{
			State temp = new State(s);
			temp.makeMove(i);
			double value = valueFunction8(temp,s,s.getOrient(i),s.getSlot(i),w);
			if(value > maxvalue || maxind == -1)
			{
				maxind = i;
				maxvalue = value;
			}
		}
		return maxind;
		
		
		
//		return (int)(Math.random()*legalMoves.length); 
	}
	
	
	public double valueFunction8(State s2,State s1,int orient, int slot, double[] w)
	{
		double value = 0;
		
		int[] holesandrowholes = this.countHolesAndRowHoles(s2);
		
		value += w[0]*((double)landingHeight(s1, orient, slot));
		value += w[1]*((double)this.getCleared(s1, s2));
		value += w[2]*((double)this.getRowTransitions(s2));
		value += w[3]*((double)this.getColTransitions(s2));
		value += w[4]*((double)holesandrowholes[0]);
		value += w[5]*((double)this.cumulativeWells(s2));
		value += w[6]*((double)this.holeDepth(s2));
		value += w[7]*((double)holesandrowholes[1]);

		return value;
		
	}
	
	
	/**
	 * Get the value of a state s given a weight vector w
	 * Using the features given by :http://www.stanford.edu/~bvr/pubs/tetrischapter.pdf
	 * @param s - board state 
	 * @param w - weight vector
	 * @return - the value of the state
	 */
	public double valueFunction(State s, double[] w)
	{
		double value = 0;
		int[] top = s.getTop();
		
		//Max column height		
		value += w[0]*getMax(top);
		
		//number of holes
		value += w[1]*countHoles(s);
		
		//individual column heights
		for(int i = 0; i < State.COLS; i++)
			value += w[i+2]*top[i];
		
		//differences of column heights
		for(int i = 0; i < State.COLS-1; i++)
			value += w[i+2+State.COLS]*(Math.abs(top[i]-top[i+1]));
					
		//constant value
		value += 1*w[w.length-1];
		
		return value;
		
	}
	
	public int holeDepth(State s)
	{
		int[][] brd = s.getField(); //indexed from the lower left [row][col]
		int[] top = s.getTop();
		int holecount = 0;
		for(int i = 0; i < State.COLS; i++)
		{
			if(top[i] == 0)
				continue;
			
			boolean count = false;
			
			for(int j = 0; j < 0; j++)
			{
				if(brd[j][i] != 0 && count)
					holecount++;
				if(brd[j][i] == 0)
					count = true;
			}
		}
		return holecount;
	}
	
//	public int rowHole(State s)
//	{
//		Set<Integer> set = new HashSet<>();
//		int[][] brd = s.getField(); //indexed from the lower left [row][col]
//		int[] top = s.getTop();
//		int holecount = 0;
//		for(int i = 0; i < State.COLS; i++)
//		{
//			if(top[i] == 0)
//				continue;
//			
//			for(int j = top[i] - 1; j >= 0; j--)
//				if(brd[j][i] == 0)
//					holecount++;
//		}
//		return holecount;
//	}
	
	
	public int cumulativeWells(State s)
	{
		int[] top = s.getTop();
		int total = 0;
		for(int i = 0; i < top.length; i++)
		{
			int welldepth = 0;
			if(i == 0)
			{
				if(top[i] < top[i+1])
					welldepth = top[i+1] - top[i];
			}
			else if(i == top.length-1)
			{
				if(top[i] < top[i-1])
					welldepth = top[i-1] - top[i];
			}
			else
			{
				if(top[i] < top[i-1] && top[i] < top[i+1])
					welldepth = Math.min(top[i+1] - top[i], top[i-1] - top[i]);
			}
			total += .5*welldepth*(1+welldepth);
		}
		return total;
	}
	
	public int getColTransitions(State s)
	{
		int[] top = s.getTop();
		int[][] brd = s.getField(); //indexed from the lower left [row][col]
		int tcount = 0;
		for(int i = 0; i < State.COLS; i++)
		{
			if(top[i] == 0)
				continue;
			for(int j = 0; j < top[i]; j++)
			{
				if(brd[j][i] == 0)
				{
					if(j == 0)
						tcount++;
				}
				else
				{
					if(j > 0)
					{
						if(brd[j-1][i] == 0)
						{
							tcount++;
							continue;
						}
					}
					if(j < top[i] - 1)
					{
						if(brd[j+1][i] == 0)
						{
							tcount++;
							continue;
						}
					}
				}
			}
				
		}
		return tcount;
		
	}
	
	public int getRowTransitions(State s)
	{
		int top = getMax(s.getTop());
		int[][] brd = s.getField(); //indexed from the lower left [row][col]
		int tcount = 0;
		for(int i = 0; i < top; i++)
		{
			for(int j = 0; j < State.COLS; j++)
			{
				if(brd[i][j] == 0)
				{
					if(j == 0 || j == State.COLS -1)
						tcount++;
				}
				else
				{
					if(j > 0)
					{
						if(brd[i][j-1] == 0)
						{
							tcount++;
							continue;
						}
					}
					if(j < State.COLS - 1)
					{
						if(brd[i][j+1] == 0)
						{
							tcount++;
							continue;
						}
					}
				}
			}
				
		}
		return tcount;
		
	}
	
	public int getCleared(State s1, State s2)
	{
		return s2.getRowsCleared() - s1.getRowsCleared();
	}
	
	public int landingHeight(State s1, int orient, int slot)
	{
		return s1.getLandingHeight(orient, slot);
	}
	
	/**
	 * Get the maximum value of an integer array
	 * @param array
	 * @return max value
	 */
	public int getMax(int[] array)
	{
		int maxval = -1;
		for(int i = 0; i < array.length; i++)
			if(maxval == -1 || array[i] > maxval)
				maxval = array[i];
		return maxval;
	}
	
	/**
	 * Counts the number of holes in the State s
	 * @param s - board state
	 * @return the number of holes
	 */
	public int[] countHolesAndRowHoles(State s)
	{
		Set<Integer> set = new HashSet<>();
		int[][] brd = s.getField(); //indexed from the lower left [row][col]
		int[] top = s.getTop();
		int holecount = 0;
		for(int i = 0; i < State.COLS; i++)
		{
			if(top[i] == 0)
				continue;
			
			for(int j = top[i] - 1; j >= 0; j--)
				if(brd[j][i] == 0)
				{
					set.add(j);
					holecount++;
				}
		}
		int[] ret = {holecount, set.size()};
		return ret;
	}
	
	public int countHoles(State s)
	{
		int[][] brd = s.getField(); //indexed from the lower left [row][col]
		int[] top = s.getTop();
		int holecount = 0;
		for(int i = 0; i < State.COLS; i++)
		{
			if(top[i] == 0)
				continue;
			
			for(int j = top[i] - 1; j >= 0; j--)
				if(brd[j][i] == 0)
				{
					holecount++;
				}
		}
		return holecount;
	}
	
	
	
	
}
