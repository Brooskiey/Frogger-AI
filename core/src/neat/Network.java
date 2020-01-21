package neat;

import java.util.*;

/**
 * Class which represents the "brain" of an organism. It is a connection of nodes via links in
 * which values are passed through and a certain value is chosen as the "correct" output.
 * @author Chance Simmons and Brandon Townsend
 * @version 20 January 2020
 */
public class Network {
    /**
     * A static mapping of innovation numbers. These help in identifying similar links across
     * multiple networks during crossover.
     */
    private static Map<Integer, String> innovationList = new HashMap<>();

    /** A list of all links in this network. */
    private List<Link> links;

    /** A list of all input nodes in this network. No new input nodes should be added over time.*/
    private Node[] inputNodes;

    /** A list of all output nodes in this network. No new output nodes should be added over time.*/
    private Node[] outputNodes;

    /** A list of all hidden nodes in this network. This part can grow over time. */
    private List<Node> hiddenNodes;

    /** A single bias node which should be connected to all non-input nodes. Helps with outputs. */
    private Node biasNode;

    /** Counter to keep track of the number of nodes there are in our network. */
    private int numNodes;

    /** Counter to keep track of the number of current layers there are in our network. */
    private int numLayers;

    /**
     * Our network constructor. Builds an initial fully connected network of input and output nodes.
     * @param inputNum The number of input nodes to have.
     * @param outputNum The number of output nodes to have.
     */
    public Network(int inputNum, int outputNum) {
        numNodes = 0;
        numLayers = 0;
        links = new ArrayList<>();
        inputNodes = new Node[inputNum];
        outputNodes = new Node[outputNum];
        hiddenNodes = new ArrayList<>();
        biasNode = new Node(-1, numLayers);
        biasNode.setOutputValue(1);
        numNodes++;

        for(int i = 0; i < inputNum; i++) {
            inputNodes[i] = new Node(i, numLayers);
            numNodes++;
        }

        numLayers++;

        for(int i = 0; i < outputNum; i++) {
            // Our initial output layer is 1 since it is the layer specifically behind our input.
            // If we add a node in the hidden layer, our output layer should grow.
            outputNodes[i] = new Node(inputNodes.length + i, numLayers);
            numNodes++;
        }

        // Links our input nodes to output nodes and attaches the bias node to each output node.
        generateNetwork();
    }

    /**
     * Copy constructor used to deep copy a network.
     * @param network The network to copy.
     */
    public Network(Network network) {
        this.numNodes = network.numNodes;
        this.numLayers = network.numLayers;
        this.links = new ArrayList<>();
        this.inputNodes = new Node[network.inputNodes.length];
        this.outputNodes = new Node[network.outputNodes.length];
        this.hiddenNodes = new ArrayList<>();
        this.biasNode = new Node(network.biasNode);
        // TODO perform deep copy where the links are not different for every node.
    }

    /**
     * Helper function to fully connect the initial network of just input and output nodes. Also
     * attaches the bias node to each output node.
     */
    private void generateNetwork() {
        for(Node input : inputNodes) {
            for(Node output : outputNodes) {
                // Math.random() * 2 - 1 generates a random number between -1 and 1.
                addLink(input, output, Math.random() * 2 - 1);
            }
        }
        for(Node output : outputNodes) {
            // Link the bias node and apply a random link weight. Comment out if using line below.
            addLink(biasNode, output, Math.random() * 2 - 1);
            // NOTE: If you would like to control the link weight of the bias node, please use
            // the created one in the coefficients enumeration and uncomment the line below.
            // addLink(biasNode, output, Coefficients.BIAS_NODE_LINK_WEIGHT.getValue());
        }
    }

    private Link getLink(Node input, Node output) {
        for(Link link : links) {
            if(link.getInputNodeID() == input.getId() && output.getId() == link.getOutputNode().getId()) {
                return link;
            }
        }
        return null;
    }

    /**
     * Adds a specified link between the two supplied nodes with a supplied weight. Does NOT
     * randomly add a link to the network. Will not create a link if the two nodes are already
     * connected.
     * @param input The input node to connect from.
     * @param output The output node to connect to.
     * @param weight The weight that should be given to the link.
     */
    private void addLink(Node input, Node output, double weight) {
        int inputID = input.getId();
        int outputID = output.getId();

        if(!input.isConnectedTo(output)) {
            links.add(new Link(getInnovationNumber(inputID, outputID), inputID, output, weight));
            input.getOutgoingLinks().add(new Link(getInnovationNumber(inputID, outputID), inputID,
                                                    output, weight));
        }
    }

    /**
     * Returns the innovation number of the link between the specified input node ID and output
     * node ID. Will return a new innovation number if there is no pre-existing link in our
     * innovation number list.
     * @param inputID The ID of the input node.
     * @param outputID The ID of the output node.
     * @return The innovation number of a pre-existing link or a brand new innovation number.
     */
    private int getInnovationNumber(int inputID, int outputID) {
        int innovationNumber = innovationList.size();
        String innovationSearch = inputID + " " + outputID;
        boolean found = false;
        for(Map.Entry<Integer, String> innovation : innovationList.entrySet()) {
            int key = innovation.getKey();
            String value = innovation.getValue();
            if(value.equals(innovationSearch)) {
                innovationNumber = key;
                found = true;
            }
        }
        if(!found) {
            innovationList.put(innovationNumber, innovationSearch);
        }
        return innovationNumber;
    }

    /**
     * Returns whether or not this network is fully connected. Links that are not enabled are
     * still counted in this total.
     * @return True if this network is fully connected, false otherwise.
     */
    public boolean isFullyConnected() {
        // Adding up the total links from input nodes to their outputs.
        int maxLinks = inputNodes.length * hiddenNodes.size();
        maxLinks += inputNodes.length * outputNodes.length;

        // Adding up the total links from hidden nodes to their outputs.
        for(Node node : hiddenNodes) {
            for (Node otherNode : hiddenNodes) {
                if(node.getLayer() < otherNode.getLayer()) {
                    maxLinks++;
                }
            }
        }
        maxLinks += hiddenNodes.size() * outputNodes.length;

        // Adding up the total links from bias nodes to their outputs.
        maxLinks += hiddenNodes.size() + outputNodes.length;

        return maxLinks == links.size();
    }

    /**
     * Activates every node in the network in a certain specified order. Should traverse from
     * input -> bias -> hidden -> output.
     * @param inputValues The values to be set as our input layer nodes' output values.
     * @return The output values in our output nodes after every node has been activated.
     */
    public double[] feedForward(float[] inputValues) {
        // Set the output values of our input nodes to the supplied input values.
        for(int i = 0; i < inputNodes.length; i++) {
            inputNodes[i].setOutputValue(inputValues[i]);
        }

        // Activate the nodes in order from input node -> bias -> hidden -> output.
        for(Node node : listNodesByLayer()) {
            node.activate();
        }

        // Write the output values to a double array to pass back as the decisions of this network.
        double[] outputs = new double[outputNodes.length];
        for(int i = 0; i < outputNodes.length; i++) {
            outputs[i] = (outputNodes[i].getOutputValue());
        }

        // Set all input values back to 0 for the next feed forward.
        for(Node node : listNodesByLayer()) {
            node.setInputValue(0);
        }

        return outputs;
    }

    /**
     * Mutates this network, either with only link weights possibly being modified or by adding
     * additional structure via new links or new nodes.
     */
    public void mutate() {
        // Mutation for link weight. Each link is either mutated or not each generation.
        for(Link link : links) {
            if(Math.random() < Coefficients.LINK_WEIGHT_MUT.getValue()) {
                link.mutateWeight();
            }
        }

        // Mutation for adding a link between two random, unlinked nodes.
        if(Math.random() < Coefficients.ADD_LINK_MUT.getValue()) {
            addLinkMutation();
        }

        // Mutation for adding a new node where a link previously was.
        if(Math.random() < Coefficients.ADD_NODE_MUT.getValue()) {
            addNodeMutation();
        }
    }

    /**
     * Adds a link between two randomly selected nodes.
     */
    private void addLinkMutation() {
        if(!isFullyConnected()) {
            Random random = new Random();
            List<Node> allNodes = listNodesByLayer();

            Node input, output;
            do {
                input = allNodes.get(random.nextInt(allNodes.size()));
                output = allNodes.get(random.nextInt(allNodes.size()));
            } while(isBadLink(input, output));

            if(output.getLayer() < input.getLayer()) {
                Node temp = input;
                input = output;
                output = temp;
            }

            addLink(input, output, Math.random() * 2 - 1);
        }
    }

    /**
     * Returns whether or not a link can be formed between two nodes. If the nodes are already
     * connected, it is a bad link and if both nodes are from the same layer, it is a bad link.
     * @param node1 One of the nodes on the link.
     * @param node2 The other node on the link.
     * @return True if the future link is bad, false otherwise.
     */
    private boolean isBadLink(Node node1, Node node2) {
        return node1.isConnectedTo(node2) || node1.getLayer() == node2.getLayer();
    }

    /**
     * Adds a new node to our network where a random link used to be. Two new links appear
     * connecting the old input and old output to the new node. The old link gets disabled so it
     * can no longer be used to feed forward (which would bypass our hidden node, therefore
     * reducing its ability to change the network's output).
     */
    private void addNodeMutation() {
        Random random = new Random();
        Link link;
        do {
            link = links.get(random.nextInt(links.size()));
        } while(link.getInputNodeID() == biasNode.getId());

        link.setEnabled(false);
        Node oldInput = getNode(link.getInputNodeID());
        int layer = (int) Math.ceil((oldInput.getLayer() + link.getOutputNode().getLayer()) / 2.0);

        // If the layer we're placing our new node was the previous output nodes layer, we
        // move all layers that are equal to or greater than the new layer down.
        if(layer == link.getOutputNode().getLayer()) {
            for (Node node : listNodesByLayer()) {
                if (node.getLayer() >= layer) {
                    node.incrementLayer();
                }
            }
            numLayers++;
        }

        // Actually add the node now so it avoids having it's own layer incremented.
        Node toAdd = new Node(numNodes++, layer);
        hiddenNodes.add(toAdd);

        // Now add links to either side of the new node. The link going from the old input to the
        // new node gets a weight of 1 while the link going from the new node to the old output
        // receives the weight of the now-disabled link.
        addLink(oldInput, toAdd, 1);
        addLink(toAdd, link.getOutputNode(), link.getWeight());

        // Finally connect our bias node with a weight of 0 to minimize the bias' initial impact.
        addLink(biasNode, toAdd, 0);
    }

    /**
     * Returns the node with the specified ID.
     * @param id The ID number to search by.
     * @return The node that corresponds to the ID number or null.
     */
    private Node getNode(int id) {
        for(Node node : listNodesByLayer()) {
            if(node.getId() == id) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns a list of all nodes in order by their layers.
     * @return A list of all nodes in order by their layers.
     */
    private List<Node> listNodesByLayer() {
        List<Node> nodes = new ArrayList<>(Arrays.asList(inputNodes));
        nodes.add(biasNode);
        for(int layer = 1; layer < numLayers; layer++) {
            for(Node node : hiddenNodes) {
                if(node.getLayer() == layer) {
                    nodes.add(node);
                }
            }
        }
        nodes.addAll(Arrays.asList(outputNodes));

        return nodes;
    }
}
