package tetris.agent;

import tetris.simulator.State;


public class AgentReinforce {
	
	public static int totalFeatures = 22;

	//implement this function to have a working system
	//Inputs:
	//- s is the current state of the board
	//- legalMoves is a nx2 matrix of the n possible actions for the current piece.
	//	An action is the orientation & column to place the current piece
	//Outputs:
	//- index n of the action to execute in legalMoves
	public int chooseAction(State s, int[][] legalMoves, double[] w) 
	{	
		
		double maxvalue = -1;
		int maxind = -1;
		for(int i = 0; i < legalMoves.length; i++)
		{
			State temp = new State(s);
			temp.makeMove(i);
			double value = valueFunction(temp,w);
			if(value > maxvalue || maxind == -1)
			{
				maxind = i;
				maxvalue = value;
			}
		}
		
		return maxind;
		
		//return sampleMaxActionFromDistribution(s, s.legalMoves(), w);

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
	
	/*
	 * Get feature vector for current state
	 * 
	 * */
	
	public double[] getF(State s){
		double[] f = new double[totalFeatures];
		int[] top = s.getTop();
		
		//Max column height		
		f[0] = getMax(top);
		
		//number of holes
		f[1] = countHoles(s);
		
		//individual column heights
		for(int i = 0; i < State.COLS; i++)
			f[i+2] = top[i];
		
		//differences of column heights
		for(int i = 0; i < State.COLS-1; i++)
			f[i+2+State.COLS] = (Math.abs(top[i]-top[i+1]));
					
		return f;
	}
	
	/*
	 * Sample an action form a Boltzmann distribution
	 * 
	 * */
	public int sampleActionFromDistribution(State s, int[][] legalMoves, double[] w){
		
		double[] f = getF(s);
		double unnormalizedSumOfProbs = 0D;
		double[] unnormalizedProbs = new double[legalMoves.length];
		
		for(int i = 0; i < legalMoves.length; i++)
		{
			State temp = new State(s);
			temp.makeMove(i);
			f = getF(temp);
			unnormalizedProbs[i]=getUnnormalizedProbability(w,f);
			unnormalizedSumOfProbs += unnormalizedProbs[i]; 
		}
		
		//pick random number uniformly
		double rand = unnormalizedSumOfProbs*Math.random();
		
		//check the action that corresponds to the random number
		int selectedAction = 0;
		double accumulatedProb=0;
		for(int i = 0; i < legalMoves.length; i++)
		{
			accumulatedProb += unnormalizedProbs[i];
			if(rand <= accumulatedProb)
			{
				selectedAction = i;
				break;
			}
		}
		
		return selectedAction;
	}
	
	
	public int sampleMaxActionFromDistribution(State s, int[][] legalMoves, double[] w){
		
		double[] f = getF(s);
		int maxAction = (int)Math.random()*legalMoves.length;
		double maxProb = 0D;
		double currentProb = 0D;
		
		for(int i = 0; i < legalMoves.length; i++)
		{
			State temp = new State(s);
			temp.makeMove(i);
			f = getF(temp);
			currentProb = getUnnormalizedProbability(w,f);
			if(currentProb > maxProb){
				maxAction = i;
				maxProb = currentProb;
			} 
		}
		
		return maxAction;
	}
	
	
	/*
	 * Get probability of distribution for a given paramater
	 * 
	 * */
	public double getUnnormalizedProbability(double[] w, double[] f){
		
		//Boltzmann Distribution
		double dotProduct = 0;
		for(int i=0; i<w.length; i++)
		{
			dotProduct += w[i]*f[i];
		}
		return Math.exp(dotProduct);
	}
	
	/*
	 * Compute expected feature vector for next state (over possible actions)
	 * 
	 * */
	
	public double[] expectedFeatureOnNextStep(State s, int[][] legalMoves, double[] w){
		
		int numfeatures = w.length;
		double[] f = getF(s);
		double unnormalizedSumOfProbs = 0D;
		double[] unnormalizedProbs = new double[legalMoves.length];
		
		double[] fExp = new double[numfeatures];
		
		//for each action
		for(int k=0; k < s.legalMoves().length; k++){
			
			State temp = new State(s);
			temp.makeMove(k);
			double[] tempF = getF(temp);
			double currentProb = getUnnormalizedProbability(w, tempF);
			
			for(int l=0; l<fExp.length; l++){
				fExp[l] += tempF[l]*currentProb;
			}
			
			unnormalizedSumOfProbs += currentProb;
		}
		for(int l=0; l<fExp.length; l++){
			fExp[l] /= unnormalizedSumOfProbs;
		}
		
		return fExp;
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
					holecount++;
		}
		return holecount;
	}
	
	
	
}
