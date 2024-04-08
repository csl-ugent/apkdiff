package Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ClusteringAlgorithm.Clustering;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.IntegerComponentNameProvider;
import org.jgrapht.io.MatrixExporter;

import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class GraphUtil {

	public static Graph<String, DefaultWeightedEdge> createClassGraph(CallGraph callGraph){
		Graph<String, DefaultWeightedEdge> g = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
		for (Iterator<Edge> it = callGraph.iterator(); it.hasNext();) {
			Edge edge = it.next();
			String srcName = edge.src().getDeclaringClass().getName();
			String tgtName = edge.tgt().getDeclaringClass().getName();
			if(srcName.contains("dummy") || tgtName.contains("dummy")) continue;

			if(!g.containsVertex(srcName)) g.addVertex(srcName);
			if(!g.containsVertex(tgtName)) g.addVertex(tgtName);

			DefaultWeightedEdge e = (DefaultWeightedEdge) g.getEdge(srcName, tgtName);
			if(e == null){
				e = (DefaultWeightedEdge) g.addEdge(srcName, tgtName);
				g.setEdgeWeight(e, 0);
			}
			//add 1 to weight (when new weight = 0)
			g.setEdgeWeight(e, 1 + g.getEdgeWeight(e));
		}
		return g;
	}

	public static void removeJavaClasses(Graph<String, DefaultWeightedEdge> g){
		//cannot modify set while iterating over it, find vertices first, then delete them
		ArrayList<String> verticesToDelete = new ArrayList<String>();
		for(String v: g.vertexSet()){
			if(v.startsWith("java.") || v.startsWith("javax.")){
				verticesToDelete.add(v);
			}
		}
		for(String v: verticesToDelete){
			g.removeVertex(v);
		}
	}


	public static void exportDOT(String path, Graph g){
		IntegerComponentNameProvider<String> vertexIdProvider = new IntegerComponentNameProvider();
		ComponentNameProvider<String> vertexLabelProvider = name -> name;
		DOTExporter exporter = new DOTExporter(vertexIdProvider, vertexLabelProvider, null);
		new File(path).mkdirs();
		try {
			exporter.exportGraph(g, new FileWriter(path + File.separator + "graph.dot"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void exportDOTWithColors(String outputPath, Clustering<String> clustering){
		// !!!
		// THIS FUNCTION ASSUMES graph.dot EXISTS AND IS CORRECT
		// ALWAYS CALL exportDOT FIRST

		//src http://graphviz.org/doc/info/colors.html
		String[] possibleColors = new String[]{"red", "blue", "brown", "green", "purple", "pink", "orange", "cyan", "darkgreen",
				"blue4", "white", "yellow", "tan", "steelblue", "violet", "springgreen", "sienna1", "gold", "aquamarine",
				"salmon", "seagreen", "red4", "slateblue", "maroon4", "navy", "greenyellow", "orangered", "olivedrab", "mediumblue"};
		try {
			File inputFile = new File(outputPath + File.separator + "graph.dot");
			Scanner myReader;
			myReader = new Scanner(inputFile);
			FileWriter myWriter = new FileWriter(outputPath + File.separator + "graphWithColors.dot");
			String newLine = System.getProperty("line.separator");
			boolean endOfVertexDefinitonFound = false;
		    while (myReader.hasNextLine()) {
		    	String data = myReader.nextLine() + newLine;
		    	if(data.startsWith("strict")){
		    		//first line of dot format
		    		myWriter.write(data);
		    		myWriter.write("  {" + newLine);
		    		myWriter.write("    node [style=filled]" + newLine);
		    		continue;
		    	}
		    	if(data.startsWith("}")){
		    		//last line
		    		myWriter.write(data);
		    		continue;
		    	}
		    	if(data.indexOf('[') != -1){
		    		//vertex definition
		    		String color = "grey";
		    		String label = data.substring(data.indexOf("\"") + 1);
		    		label = label.substring(0, label.indexOf("\""));
		    		List<Set<String>> clusters = clustering.getClusters();
		    		for(int i = 0; i < clustering.getNumberClusters(); i++){
		    			Set<String> cluster = clusters.get(i);
		    			if(cluster.contains(label)){
		    				if(i >= possibleColors.length){
		    					System.out.println("[WARNING]: Not enough supported colors! --> assigning grey");
		    					color = "grey";
		    				}
		    				else{
		    					color = possibleColors[i];
		    				}
		    				break;
		    			}
		    		}
		    		myWriter.write("\t" + data.replace("];", ", fillcolor="+color+"];"));
		    	}
		    	else{
		    		if(!endOfVertexDefinitonFound){
		    			endOfVertexDefinitonFound = true;
		    			myWriter.write("  }" + newLine);
		    		}
		    		//regular edge definition
		    		myWriter.write(data);
		    	}
		    }
		    myReader.close();
		    myWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void exportAdjacencyMatrix(Graph g) {
		//default format is adjacency
		MatrixExporter exp = new MatrixExporter();
		File file = new File("test.txt");
		try {
			exp.exportGraph(g, file);;
		} catch (ExportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
