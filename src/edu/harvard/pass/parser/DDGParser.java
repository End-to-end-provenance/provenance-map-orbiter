package edu.harvard.pass.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import edu.harvard.pass.PEdge;
import edu.harvard.pass.PMeta;
import edu.harvard.pass.PNode;
import edu.harvard.pass.PObject;
import edu.harvard.util.ParserException;
import edu.harvard.util.ParserFormatException;
import edu.harvard.util.Utils;


/**
 * Reads a textual description of a DDG and constructs a graph for it
 * 
 * @author Barbara Lerner
 * @author Peter Macko
 */
public class DDGParser implements Parser {
	
	private File file;
	private BufferedReader in;
	private int numPins;
	private ParserHandler handler;
	
	
	/**
	 * Create an instance of class DDGParser
	 */
	public DDGParser() {
	}
	
	
	/**
	 * Initialize the parser given an input URI. If the file has an ambiguous type (such as *.xml),
	 * the method should check whether the file has a proper format that can be handled by the parser.
	 * 
	 * @param uri the input URI
	 * @throws ParserException on error
	 * @throws ParserFormatException if the file does not have the appropriate format
	 */
	public void initialize(URI uri) throws ParserException {
		
		if (!accepts(uri)) throw new ParserFormatException();
		
		in = null;
		numPins = 0;
	}
	

	/**
	 * Parse an object identified by the given URI
	 * 
	 * @param uri the input URI
	 * @param handler the callback for parser events
	 * @throws ParserException on error
	 */
	public void parse(URI uri, ParserHandler handler) throws ParserException {
		
		if (!"file".equals(uri.getScheme())) throw new ParserFormatException();
		
		file = new File(uri);
		
		
		// Initialize the provenance metadata
		
		PMeta meta = new PMeta();
		
		meta.addObjectAttributeCode("Name", PObject.Attribute.NAME);
		meta.addObjectAttributeCode("Type", PObject.Attribute.TYPE);
		meta.addNodeAttributeCode("Label", PNode.Attribute.LABEL);
		
		meta.addObjectExtType("Data", PObject.Type.ARTIFACT);
		meta.addObjectExtType("Exception", PObject.Type.ARTIFACT);
		
		meta.addObjectExtType("Start", PObject.Type.PROCESS);
		meta.addObjectExtType("VStart", PObject.Type.PROCESS);
		
		meta.addObjectExtType("Interm", PObject.Type.PROCESS);
		meta.addObjectExtType("VInterm", PObject.Type.PROCESS);
		meta.addObjectExtType("Leaf", PObject.Type.PROCESS);
		meta.addObjectExtType("VLeaf", PObject.Type.PROCESS);

		meta.addObjectExtType("Finish", PObject.Type.PROCESS);
		meta.addObjectExtType("VFinish", PObject.Type.PROCESS);
		
		meta.addEdgeLabel("DF", PEdge.Type.DATA);
		meta.addEdgeLabel("CF", PEdge.Type.CONTROL);

		handler.setMeta(meta);
		
		
		// Parse
		
		try {
			in = new BufferedReader(new FileReader(file));
			this.handler = handler;
			
			this.handler.beginParsing();
			addNodesAndEdges();
			this.handler.endParsing();
			
			in.close();
		}
		catch (IOException e) {
			in = null;
			this.handler = null;
			throw new ParserException("I/O Error", e);
		}
		
		in = null;
		this.handler = null;
	}
	
	
	/**
	 * Determine whether the parser accepts the given URI
	 * 
	 * @param uri the input URI
	 * @return true if it accepts the input
	 */
	public boolean accepts(URI uri) {
		
		if (!"file".equals(uri.getScheme())) return false;
		File file = new File(uri);
		String ext = Utils.getExtension(file);
		
		if ("ddg".equals(ext)) return true;
		if ("txt".equals(ext)) return true;
		
		return false;
	}
	

	/**
	 * Adds the nodes and edges from the DDG to the graph
	 * 
	 * @throws IOException if there is a problem reading the file
	 * @throws ParserException on parser error
	 */
	private void addNodesAndEdges() throws IOException, ParserException {
		String nextLine = in.readLine();
		if (nextLine == null) {
			throw new IOException("Number of pins is missing from the file.");
		}
		numPins = Integer.parseInt(nextLine);
		nextLine = in.readLine();
		while (nextLine != null) {
			parseDeclaration(nextLine);
			nextLine = in.readLine();
		}
	}

	
	/**
	 * Parse a declaration
	 * 
	 * @param nextLine the next line in the DDG file
	 * @throws ParserException on parser error
	 */
	private void parseDeclaration(String nextLine) throws ParserException {
		if (!nextLine.equals("")) {
			String[] tokens = nextLine.split(" ");
			if (tokens[0].equals("CF") || tokens[0].equals("DF")) {
				parseEdge(tokens);
			}
			else {
				parseNode(tokens);
			}
		}
	}
	

	/**
	 * Parse a node declaration
	 * 
	 * @param tokens the tokens on the current line
	 * @throws ParserException on parser error
	 */
	private void parseNode(String[] tokens) throws ParserException {
		handler.loadTripleAttribute(tokens[1], "TYPE", tokens[0]);
		handler.loadTripleAttribute(tokens[1], "NAME", constructName(tokens, false));
		handler.loadTripleAttribute(tokens[1], "LABEL", constructName(tokens, false));
	}

	
	/**
	 * Construct a name of a node
	 * 
	 * @param tokens the tokens on the current node declaration line
	 * @param simple true to ignore the type name
	 * @return the node name
	 */
	private String constructName(String[] tokens, boolean simple) {
		StringBuilder str = new StringBuilder();
		for (int i = 2; i < tokens.length; i++) {
			str.append(tokens[i] + " ");
		}
		if (!simple && isMultipleNodePIN(tokens[0])){
			str.append(tokens[0]);
		}
		return str.toString();
	}

	
	/**
	 * Determine if the node is a multiple node PIN
	 * 
	 * @param type the node type
	 * @return true if it is the multiple node PIN
	 */
	private boolean isMultipleNodePIN(String type) {
		if (type.equals("Start") || type.equals("Interm") || type.equals("Finish")) {
			return true;
		}

		if (type.equals("VStart") || type.equals("VInterm") || type.equals("VFinish")) {
			return true;
		}
		
		return false;
	}


	/**
	 * Extract a UID
	 * 
	 * @param idToken the ID token
	 * @return the UID
	 */
	@SuppressWarnings("unused")
	private int extractUID(String idToken) {
		int uid = Integer.parseInt(idToken.substring(1));
		if (idToken.charAt(0) == 'd') {
			uid = uid + numPins;
		}
		return uid;
	}


	/**
	 * Parse an edge declaration
	 * 
	 * @param tokens the tokens on the current line
	 * @throws ParserException on parser error
	 */
	private void parseEdge(String[] tokens) throws ParserException {
		handler.loadTripleAncestry(tokens[1], tokens[0], tokens[2]);
	}
}
