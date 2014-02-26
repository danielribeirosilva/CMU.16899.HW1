package tetris;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tetris.simulator.*;
import tetris.agent.*;

import java.util.Random;

public class MainReinforce {

	public static void main(String[] args) {
		double[] w = train();
		for(int i = 0; i < w.length; i++)
			System.out.println(w[i]);
		State sFinal = runGraphics(w);
		System.out.println("You have completed "+sFinal.getRowsCleared()+" rows.");
	}
	
	//run the tetris game and save image of the board at each turn, returns the final state
	public static State recordVideo(double[] w)
	{
		int delay = 50;
		
		State s = new State();
		Visualizer v = new Visualizer(s);
		AgentReinforce a = new AgentReinforce();
		while(!s.hasLost()) {
			s.makeMove(a.chooseAction(s,s.legalMoves(),w));
			v.draw();
			v.drawNext(0,0);
			v.save(s.getTurnNumber() + ".png");
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		v.dispose();
		return s;
	}
	
	
	//runs the tetris game until the game is over and returns the final state
	public static State run(double[] w)
	{
		State s = new State();
		AgentReinforce a = new AgentReinforce();
		while(!s.hasLost()) {
			s.makeMove(a.chooseAction(s,s.legalMoves(),w));
		}
		return s;
	}
	
	public static double[] train()
	{
		PrintWriter printer = null;
		try {
	        File output = new File("ouput.txt");
	        printer = new PrintWriter(output);
	    }
	    catch(FileNotFoundException e) {
	        System.err.println("File not found. Please scan in new file.");
	    }
		
		int numGrad = 1;
		int numIter = 100;
		int numfeatures = 22;
		double gamma = 0.98;
		double learningRate = 0.01;
		double killingRate = 1.0;
		double[] w = new double[numfeatures];
		
	
		//iterations for gradient descent
		for(int i = 0; i < numIter; i++)
		{
			
			System.out.println("\nStep "+(i+1));
			
			//initialize variables
			State s = new State();
			double[] delta = new double[numfeatures];
			double[] avgDelta = new double[numfeatures];
			double[] z = new double[numfeatures];
			AgentReinforce agt = new AgentReinforce();
			int t = 1;
			
			//number of estimates for gradient at given w
			for(int n=0; n<numGrad; n++){
				
				z = new double[numfeatures];
				delta = new double[numfeatures];
				s = new State();
				t = 1;
				
				while(!s.lost){
					
					//sample action from current probability distribution
					//int action = agt.sampleActionFromDistribution(s, s.legalMoves(), w);
					int action = agt.sampleMaxActionFromDistribution(s, s.legalMoves(), w);
					
					//compute current f
					double[] f = agt.getF(s);
					
					//compute expected f for next state
					double[] fExp = agt.expectedFeatureOnNextStep(s, s.legalMoves(), w);
					
					//get reward for next step
					State nextState = new State(s);
					nextState.makeMove(action);
					double survivalReward = nextState.lost ? 0D : 1D;
					double heightReward = Math.exp(-agt.getMax(nextState.getTop()));
					double reward = heightReward + survivalReward;
					
					//update z
					for(int k=0; k<z.length; k++){
						z[k] = gamma*z[k] + f[k] - fExp[k]; 
					}
					
					//update delta
					for(int k=0; k<delta.length; k++){
						delta[k] = delta[k] + (1D/(double)t) * ( reward*z[k] - delta[k] );
					}
					
					t++;
					s.makeMove(action);
					
				} //tetris player died
				System.out.println("total t before death: "+t);
				
				for(int h=0; h<avgDelta.length; h++){
					avgDelta[h] += delta[h]; 
				}
				
			}
			
			for(int h=0; h<avgDelta.length; h++){
				avgDelta[h] /= (double)numGrad; 
			}
			
			//gradient descent update
			//System.out.println("w:");
			for(int k=0; k<w.length; k++){
				w[k] += learningRate*avgDelta[k];
				//System.out.print(w[k]+" ");
			}
			
			
			learningRate *= killingRate;
		}
		
		
		return w;
		
	}
	
	public static double testRun(double[] w)
	{
		State s = new State();
		AgentReinforce a = new AgentReinforce();
		while(!s.hasLost()) 
		{
			s.makeMove(a.chooseAction(s,s.legalMoves(),w));
		}
		return s.getRowsCleared();
	}
	
	public static List<Sample> getSamples(double[] wmean, double[] wstdev, int numSamples)
	{
		List<Sample> list = new ArrayList<Sample>();
		Random rand = new Random();
		for(int i = 0; i < numSamples; i++)
		{
			double[] w = new double[wmean.length];
			for(int j = 0; j < w.length; j++)
				w[j] = rand.nextGaussian()*wstdev[j]+wmean[j];
			list.add(new Sample(w));
		}
		return list;
	}
	
	
	//runs and displays the tetris game until the game is over and returns the final state
	public static State runGraphics(double[] w)
	{
		int delay = 40;
		
		State s = new State();
		Visualizer v = new Visualizer(s);
		AgentReinforce a = new AgentReinforce();
		while(!s.hasLost()) {
			s.makeMove(a.chooseAction(s,s.legalMoves(),w));
			v.draw();
			v.drawNext(0,0);
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		v.dispose();
		return s;
	}
	
	//allows a human player to play using the 4 arrow keys
	// left: move the piece left
	// right: move the piece right
	// top: change orientation
	// down: drop piece
	// there is no time limit for choosing where to place the next piece
	public static State runHumanPlayer()
	{
		int delay = 100;
		State s = new State();
		Visualizer v = new Visualizer(s);
		v.draw();
		v.drawNext(0,0);
		while(!s.hasLost()) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		v.dispose();
		return s;
	}
}


