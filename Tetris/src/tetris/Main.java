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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.*;
import java.util.*;

public class Main {
	
	public static void main(String[] args) {
		//double[] w = train();
		double[] w = {-47.43428264281537 , 26.126322713902507 , -57.65203670790028 , -109.31064691871963 , -229.7157867858535 , -68.43036599510476 , -10.017167508019858 , -159.8210878410291 };
		runGraphics(w);
		//for(int i = 0; i < w.length; i++)
			//System.out.println(w[i]);
		//State sFinal = runGraphics(w);
		//System.out.println("You have completed "+sFinal.getRowsCleared()+" rows.");
	}
	
	//run the tetris game and save image of the board at each turn, returns the final state
	public static State recordVideo(double[] w)
	{
		int delay = 50;
		
		State s = new State();
		Visualizer v = new Visualizer(s);
		Agent a = new Agent();
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
		Agent a = new Agent();
		while(!s.hasLost()) {
			s.makeMove(a.chooseAction(s,s.legalMoves(),w));
		}
		return s;
	}
	
	public static double getSampleScoreConcurrent(Sample s, int numruns){
		ExecutorService	threadPool = Executors.newFixedThreadPool(8);
		List<Future<Double>> list = new ArrayList<Future<Double>>();
		double avgScore = 0D;
		
		for(int i = 0; i < numruns; i++){
			Callable<Double> worker = new SampleScoreThread(s);
			Future<Double> submit = threadPool.submit(worker);
			list.add(submit);
		}
		
		for(Future<Double> future : list){
			try {
				avgScore += future.get().doubleValue();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		threadPool.shutdown();
		avgScore = avgScore/(double)(numruns);
		return avgScore;
	}
	
	public static class SampleScoreThread implements Callable<Double>{
		Sample sample;
		
		public SampleScoreThread(Sample s){
			this.sample = s;
		}
		
		public Double call(){
			Double rVal = new Double(testRun(this.sample.w));
			return rVal;
		}
	}
	
	public static double getSampleScore(Sample s, int numruns)
	{
		double score = 0;
		for(int i = 0; i < numruns; i++)
			score += testRun(s.w);
		score /= numruns;
		return score;
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
		
		int numIter = 50;
		int numSamples = 100;

		int numfeatures = 8;
		int numrunspersample = 2;
		
		//number of samples to select
		int numSelect = 10;
		
		double[] wmean = new double[numfeatures];
		double[] wstdev = new double[numfeatures];
		for(int i = 0; i < numfeatures; i++)
		{
			wmean[i] = 0;
			wstdev[i] = 100;
		}
		
		double[] wbest = new double[numfeatures];
		
		for(int i = 0; i < numIter; i++)
		{
			//Get samples from distributions
			List<Sample> samples = getSamples(wmean,wstdev,numSamples);
			
			//Run tetris for each sample
			for(int j = 0; j < numSamples; j++)
				samples.get(j).score = getSampleScore(samples.get(j), numrunspersample);
//				samples.get(j).score = getSampleScoreConcurrent(samples.get(j),numrunspersample);
			
			//Compute new means and stdevs based on best samples
			Collections.sort(samples);
			Collections.reverse(samples);
			for(int j = 0; j < numfeatures; j++)
			{
				wmean[j] = 0;
				wstdev[j] = 0;
				
				//compute new means
				for(int k = 0; k < numSelect; k++)
					wmean[j] += samples.get(k).w[j];
				wmean[j] /=numSelect;
				
				//compute new standard deviations
				for(int k = 0; k < numSelect; k++)
					wstdev[j] += Math.pow(samples.get(k).w[j]-wmean[j],2);
				wstdev[j] /=numSelect;
				wstdev[j] += Math.max(5-i/10, 0); // add noise
				wstdev[j] = Math.sqrt(wstdev[j]);

			}
			wbest = samples.get(0).w;
			printer.println("Current Score: " + samples.get(0).score);
			for(int l = 0; l < samples.get(0).w.length; l++)
			{
				printer.println(samples.get(0).w[l]);
				System.out.print(samples.get(0).w[l] + "  ");
			}
			System.out.println();
			System.out.println("Current Score: " + samples.get(0).score);

		}
		printer.close();
		
		return wbest;
		
	}
	
	public static double testRun(double[] w)
	{
		State s = new State();
		Agent a = new Agent();
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
		int delay = 30;
		
		State s = new State();
		Visualizer v = new Visualizer(s);
		Agent a = new Agent();
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

class Sample implements Comparable<Sample>
{
	double[] w;
	double score;
	public Sample(double[] w)
	{
		this.w = w;
		this.score = -1;
	}
	@Override
	public int compareTo(Sample arg0) 
	{
		
		return Double.compare(score, arg0.score);
	}
}
