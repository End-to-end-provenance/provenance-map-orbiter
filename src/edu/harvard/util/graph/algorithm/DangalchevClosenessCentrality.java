package edu.harvard.util.graph.algorithm;

import java.util.Collection;
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
 * The algorithm for computing Dangalchev's version of the closeness centrality.
 * 
 * More info: http://en.wikipedia.org/wiki/Centrality#Closeness_centrality
 * 
 * @author Peter Macko
 */
public class DangalchevClosenessCentrality implements Cancelable {
	
	public static final String ATTRIBUTE_NAME = "Dangalchev's Closeness Centrality";
	public static final String ATTRIBUTE_NAME_INVERTED = "Dangalchev's Closeness Centrality -- Inverted";
	public static final String ATTRIBUTE_NAME_UNDIRECTED = "Dangalchev's Closeness Centrality -- Undirected";

	private BaseGraph graph;
	private GraphDirection direction;
	private String name;
	
	private transient volatile boolean forceCancel = false;
	
	
	/**
	 * Create an instance of DangalchevClosenessCentrality for a specific graph
	 * 
	 * @param graph the provenance graph
	 */
	public DangalchevClosenessCentrality(BaseGraph graph) {
		this(graph, GraphDirection.DIRECTED);
	}
	
	
	/**
	 * Create an instance of DangalchevClosenessCentrality for a specific graph
	 * 
	 * @param graph the provenance graph
	 * @param direction whether to use directed, undirected, or inverted shortest path search (default = DIRECTED)
	 */
	public DangalchevClosenessCentrality(BaseGraph graph, GraphDirection direction) {
		
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
		
		for (BaseNode node : nodes) {
			
			if (forceCancel) {
				throw new JobCanceledException();
			}
			
			double sum = 0;
			
			List<GraphPath<BaseNode, BaseEdge>> paths = shortestPaths.getShortestPaths(node);
			for (GraphPath<BaseNode, BaseEdge> p : paths) {
				sum += Math.pow(2, -p.getEdgeList().size());
			}
			
			result.set(node, sum);
			
			
			// Update the observer
			
			nodeCount++;
			if (observer != null) {
				observer.setProgress(nodeCount);
			}
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
				DangalchevClosenessCentrality.this.cancel();
			}
		};
	}
}
