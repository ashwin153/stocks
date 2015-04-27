package com.ashwin.fri.stocks.neural;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NeuralNetwork implements Serializable {
	
	private static final long serialVersionUID = -4747208554472473154L;

	private Neuron[][] _neurons;
	
	public NeuralNetwork(int... nodes) {
		_neurons = new Neuron[nodes.length - 1][];
		for(int i = 0; i < _neurons.length; i++) {
			_neurons[i] = new Neuron[nodes[i+1]];
			for(int j = 0; j < _neurons[i].length; j++)
				_neurons[i][j] = new Neuron(nodes[i]);
		}
	}
	
	public int getNumberOfInputs() {
		return _neurons[0][0].getWeights().size() - 1;
	}
	
	public void backpropagate(List<Double> inputs, List<Double> target, double rate) {
		// Step 1: Apply to the inputs to the network and determine the output of
		// each neuron in the network. Save these outputs into a forward matrix.
		Double[][] outputs = new Double[_neurons.length+1][];
		outputs[0] = inputs.toArray(new Double[inputs.size()]);	
		for(int i = 0; i < _neurons.length; i++) {
			outputs[i+1] = new Double[_neurons[i].length];
			for(int j = 0; j < _neurons[i].length; j++)
				outputs[i+1][j] = _neurons[i][j].getActionPotential(Arrays.asList(outputs[i]));
		}
		
		// Step 2: Propagate errors back down through the network and change the weights.
		Double[][] errors = new Double[_neurons.length][];
		for(int i = errors.length - 1; i >= 0; i--) {
			errors[i] = new Double[_neurons[i].length];
			for(int j = 0; j < errors[i].length; j++) {
				if(i == errors.length - 1) {
					// If the neuron is an output node, then the error is based on the target
					// values specified in the method parameters.
					errors[i][j] = outputs[i+1][j] * (1 - outputs[i+1][j]) * (target.get(j) - outputs[i+1][j]);
				} else {
					double sigma = 0.0;
					for(int k = 0; k < _neurons[i+1].length; k++)
						sigma += _neurons[i+1][k].getWeights().get(j) * errors[i+1][k];
					errors[i][j] = outputs[i+1][j] * (1 - outputs[i+1][j]) * sigma;
				}
				
				List<Double> delta   = new ArrayList<Double>();
				List<Double> weights = _neurons[i][j].getWeights();
				for(int k = 0; k < weights.size(); k++)
					delta.add((k == weights.size() - 1) ? 
							weights.get(k) + rate * errors[i][j] :
							weights.get(k) + rate * errors[i][j] * outputs[i][k]);
				_neurons[i][j].setWeights(delta);
			}
		}
	}
	
	/**
	 * Executes the entire neural net and returns the output of the
	 * top most layer in the net.
	 * 
	 * @param inputs
	 * @return
	 */
	public List<Double> execute(List<Double> inputs) {
		return execute(_neurons.length - 1, inputs);
	}
	
	/**
	 * Executes the neural net up to the specified layer and returns
	 * the output of the top most layer.
	 * 
	 * @param layer
	 * @param inputs
	 * @return
	 */
	private List<Double> execute(int layer, List<Double> inputs) {
		List<Double> in  = (layer == 0) ? inputs : execute(layer - 1, inputs);
		List<Double> out = new ArrayList<Double>();
		
		for(int i = 0; i < _neurons[layer].length; i++)
			out.add(_neurons[layer][i].getActionPotential(in));
		return out;
	}
	
}