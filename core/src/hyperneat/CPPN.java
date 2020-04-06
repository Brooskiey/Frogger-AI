package hyperneat;

import java.util.List;

import static java.lang.Math.abs;

/**
 * This class will
 */
public class CPPN {

    /** Array holding the weights between any two nodes on the substrate*/
    private Substrate substrate;

    private Network CPPNFunction;

    private int inputSize;

    private int outputSize;

    private static final int SUBSTRATE_SIZE = 11;

    /**
     *
     * @param inputSize the size of the square matrix of the substrate
     */
    public CPPN(int inputSize, int outputSize){
        this.substrate = new Substrate(inputSize, outputSize, CPPN.SUBSTRATE_SIZE);
        //2 pairs of input points, 1 output weight
        this.CPPNFunction = new Network(4,1);
        this.generateNetwork();
    }

    /**
     * Creates a  copy of this CPPN
     * @return
     */
    @Override
    public CPPN clone(){
        CPPN clone = new CPPN(this.inputSize,this.outputSize);
        clone.substrate= this.substrate;
        clone.CPPNFunction = new Network(this.CPPNFunction);
        return clone;
    }

    /**
     * Takes any two points on an array
     *
     * @param xOne x coordinate of the first point on the substrate
     * @param yOne y coordinate of the first point on the substrate
     * @param xTwo x coordinate of the second point on the substrate
     * @param yTwo y coordinate of the second point on the substrate
     * @return the weight of the connection between these two points
     */
    private double outputWeight( int xOne, int yOne, int xTwo, int yTwo){
        float[] inputValues = { xOne, yOne, xTwo, yTwo };
        double[] outputArray = this.CPPNFunction.feedForward(inputValues);
        return outputArray[0];
    }

    /**
     * Function that will take the generated weights and create a Neural Network from them
     */
    private void generateNetwork(){
        //Create the node weights
        for(int i = 0; i < Coefficients.SUBSTRATE_SIZE.getValue(); i++){
            for(int j = 0; j < Coefficients.SUBSTRATE_SIZE.getValue(); j++){
                for(int k = 0; k < Coefficients.SUBSTRATE_SIZE.getValue(); k++){
                    for(int l = 0; l < Coefficients.SUBSTRATE_SIZE.getValue(); l++){
                        double output  = this.outputWeight(i,j,k,l);
                        if( abs(output) > Coefficients.MIN_WEIGHT.getValue() ){
                            this.substrate.setLinkWeight(i,j,k,l,output);
                        }else{
                            this.substrate.setLinkWeight(i,j,k,l,0);
                        }
                    }
                }
            }
        }//end nested loops

    }

    public void mutate(){
        this.CPPNFunction.mutate();
        this.generateNetwork();
    }


    public Network getCPPNetwork(){
        return this.CPPNFunction;
    }

    public double[] runSubstrate(float[] agentVision) {
        return this.substrate.feedForward(agentVision);
    }
}