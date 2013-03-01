package edu.harvard.util.graph.algorithm;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.FloydWarshallShortestPaths;

import edu.harvard.util.Cancelable;
import edu.harvard.util.graph.BaseEdge;
import edu.harvard.util.graph.BaseGraph;
import edu.harvard.util.graph.BaseNode;
import edu.harvard.util.graph.GraphDirection;
import edu.harvard.util.graph.PerNodeComparableAttribute;
import edu.harvard.util.job.Job;
import edu.harvard.util.job.JobCanceledException;
import edu.harvard.util.job.JobException;
import edu.harvard.util.job.JobObserver;


/**
 * The algorithm for computing an approximation to the Betweenness Centrality.
 * 
 * More info: http://en.wikipedia.org/wiki/Centrality#Betweenness_centrality
 * 
 * @author Peter Macko
 */
public class BetweennessCentrality implements Cancelable {
	
	public static final String ATTRIBUTE_NAME = "Betweenness Centrality";
	public static final String ATTRIBUTE_NAME_INVERTED = "Betweenness Centrality -- Inverted";
	public static final String ATTRIBUTE_NAME_UNDIRECTED = "Betweenness Centrality -- Undirected";

	private BaseGraph graph;
	private GraphDirection direction;
	private String name;
	
	private transient volatile boolean forceCancel = false;
	
	
	/**
	 * Create an instance of BetweennessCentrality for a specific graph
	 * 
	 * @param graph the provenance graph
	 */
	public BetweennessCentrality(BaseGraph graph) {
		this(graph, GraphDirection.DIRECTED);
	}
	
	
	/**
	 * Create an instance of BetweennessCentrality for a specific graph
	 * 
	 * @param graph the provenance graph
	 * @param direction whether to use directed, undirected, or inverted shortest path search (default = DIRECTED)
	 */
	public BetweennessCentrality(BaseGraph graph, GraphDirection direction) {
		
		this.graph = graph;
		this.direction = direction;
		
		switch (direction) {
		case DIRECTED: name = ATTRIBUTE_NAME; break;
		case UNDIRECTED: name = ATTRIBUTE_NAME_UNDIRECTED; break;
		case INVERTED: name = ATTRIBUTE_NAME_INVERTED; break;
		default: throw new IllegalStateException("Invalid graph direction");
		}
	}

	
	/**
	 * Return the graph
	 * 
	 * @return the graph
	 */
	public BaseGraph getGraph() {
		return graph;
	}
	
	
	/**
	 * Return the attribute name
	 * 
	 * @return the attribute name
	 */
	public String getAttributeName() {
		return name;
	}
	
	
	/**
	 * Compute the metric
	 * 
	 * @return the per-node attribute graph overlay
	 */
	public PerNodeComparableAttribute<Double> compute() {
		try {
			return compute(null);
		}
		catch (JobCanceledException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Compute the metric
	 * 
	 * @param observer the job observer (can be null)
	 * @return the per-node attribute graph overlay
	 * @throws JobCanceledException if cancelled
	 */
	public PerNodeComparableAttribute<Double> compute(JobObserver observer) throws JobCanceledException {
		
		Graph<BaseNode, BaseEdge> g = graph.getBaseJGraphTAdapter(direction);
		Collection<BaseNode> nodes = graph.getBaseNodes();
		forceCancel = false;
		
		String name;
		switch (direction) {
		case DIRECTED: name = ATTRIBUTE_NAME; break;
		case UNDIRECTED: name = ATTRIBUTE_NAME_UNDIRECTED; break;
		case INVERTED: name = ATTRIBUTE_NAME_INVERTED; break;
		default: throw new IllegalStateException("Invalid graph direction");
		}
		
		PerNodeComparableAttribute<Double> result = new PerNodeComparableAttribute<Double>(name);

		
		// Initialize the shortest path algorithm
		
		if (observer != null) {
			observer.makeIndeterminate();
		}

		FloydWarshallShortestPaths<BaseNode, BaseEdge> shortestPaths =
				new FloydWarshallShortestPaths<BaseNode, BaseEdge>(g);
		
		int nodeCount = 0;
		if (observer != null) {
			observer.setRange(0, nodes.size());
		}
		
		
		// Get the shortest paths for each node
		
		// Note: This does not account for the possible existence of multiple shortest
		// paths between two nodes
		
		int n = graph.getBaseNodes().size();
		boolean directed = direction != GraphDirection.UNDIRECTED;
		double step = 1.0 / (directed ? ((n - 1) * (n - 2)) : ((n - 1) * (n - 2)) / 2.0);
		
		double[] intermediate = new double[graph.getMaxNodeIndexBase() + 1];
		for (int i = 0; i < intermediate.length; i++) intermediate[i] = Double.MIN_VALUE;
		
		for (BaseNode node : nodes) {
			
			if (forceCancel) {
				throw new JobCanceledException();
			}
			
			List<GraphPath<BaseNode, BaseEdge>> paths = shortestPaths.getShortestPaths(node);
			for (GraphPath<BaseNode, BaseEdge> p : paths) {
				
				Iterator<BaseEdge> i = p.getEdgeList().iterator();
				BaseNode last = p.getStartVertex();
				
				if (!i.hasNext()) continue;
				
				for (BaseEdge e = i.next(); i.hasNext(); e = i.next()) {
					
					BaseNode n1 = e.getBaseFrom();
					BaseNode n2 = e.getBaseTo();
					
					BaseNode other;
					if (n1 == last) other = n2;
					else if (n2 == last) other = n1;
					else if (n1 == p.getEndVertex()) other = n2;
					else if (n2 == p.getEndVertex()) other = n1;
					else {
						System.err.println("Internal Error: not an endpoint");
						System.err.println("Path from " + p.getStartVertex() + " to " + p.getEndVertex() + ":");
						Iterator<BaseEdge> j = p.getEdgeList().iterator();
						for (BaseEdge x = j.next(); j.hasNext(); e = j.next()) {
							System.err.println("    " + x + (e == x ? "  <== HERE, last=" + last : ""));
						}
						throw new InternalError();
					}
					
					if (other == p.getEndVertex() || other == p.getStartVertex()) break;
					last = other;
					
					double d = intermediate[other.getIndex()];
					
					if (d < 0) {
						d = step;
					}
					else {
						d = d + step;
					}
					
					intermediate[other.getIndex()] = d;
				}
			}
			
			
			// Update the observer
			
			nodeCount++;
			if (observer != null) {
				observer.setProgress(nodeCount);
			}
		}
		
		for (BaseNode node : nodes) {
			double d = intermediate[node.getIndex()] ;
			if (d > 0) result.set(node, d);
		}
		
		return result;
	}


	/**
	 * Cancel the computation
	 */
	@Override
	public void cancel() {
		forceCancel = true;
	}
	
	
	/**
	 * Create the job for this task that computes the metric and adds it to 
	 * the graph
	 * 
	 * @return the job
	 */
	public Job createJob() {
		return new Job() {
			
			protected JobObserver callback = null;
			
			@Override
			public void setJobObserver(JobObserver callback) {
				this.callback = callback;
			}
			
			@Override
			public void run() throws JobException {
				PerNodeComparableAttribute<Double> r = compute(callback);
				if (r == null) throw new InternalError();
				if (r != null) graph.addPerNodeAttributeOverlay(r);
			}
			
			@Override
			public boolean isMinor() {
				return false;
			}
			
			@Override
			public boolean isCancelable() {
				return true;
			}
			
			@Override
			public String getName() {
				return "Computing " + name;
			}
			
			@Override
			public void cancel() {
				BetweennessCentrality.this.cancel();
			}
		};
	}
}
