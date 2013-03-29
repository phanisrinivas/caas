package org.kisst.cordys.caas.helper

import org.apache.ivy.core.module.descriptor.IncludeRule;

import groovy.transform.ToString;

@ToString(includePackage=false,includeNames=false,excludes="file,parsedMetadata,dependencies")
class CAPPackage {
    def file
    def name
    def parsedMetadata
    def Map<String, Dependency> dependencies = new LinkedHashMap<String, Dependency>();

    def getMetadata = {
        assert file.exists()

        def zipFile = new java.util.zip.ZipFile(file)

        zipFile.entries().each {
            if (it.name == "manifest.xml") {
                def tmp = zipFile.getInputStream(it).text;
                parsedMetadata = new XmlParser().parseText(tmp);

                def ns = new groovy.xml.Namespace("http://schemas.cordys.com/ApplicationPackage")

                //Read the package name
                name = parsedMetadata.'@name';

                parsedMetadata[ns.Header][ns.ApplicationPackageDependencies][ns.ApplicationPackage].each {
                    def dep = new Dependency(id: it.'@id', name: it.'@name', version: it[ns.BuildTimeVersion].text())
                    dependencies.put(dep.name, dep)
                }

                return true
            }
        }

        parsedMetadata
    }

    /**
     * This method will sort the packages based on the dependencies of that package. 
     * @param source
     * @return
     */
    static Map<String, CAPPackage> fixOrder(Map<String, CAPPackage> source)
    {
        def DirectedGraph<CAPPackage> dg = new DirectedGraph<CAPPackage>();

        //Add the packages to the direct graph
        source.values().each {
            dg.addNode(it);
        }

        //Now add the dependency connections
        source.values().each { capPackage ->
            capPackage.dependencies.values().each {
                if (source.containsKey(it.name))
                {
                    //The dependency is one that we want to load, so we need to add the connection
                    dg.addEdge(capPackage, source.get(it.name))
                }
            }
        }

        //The graph is built up, so now we can do the sort
        def List<CAPPackage> sorted = TopologicalSort.sort(dg);

        //Note: this list needs to be reversed, since the last one in the list needs to be loaded first.
        sorted = sorted.reverse();

        def Map<String, CAPPackage> retVal = new LinkedHashMap<String, CAPPackage>();

        sorted.each {
            retVal.put(it.name, it);
        }

        retVal
    }
}

@ToString(includePackage=false,includeNames=false,excludes="id")
class Dependency {
    def id
    def name
    def version
}


class DirectedGraph<T> implements Iterable<T>
{
    /**
     * A map from nodes in the graph to sets of outgoing edges. Each set of edges is represented by a map from edges to doubles.
     */
    private final Map<T, Set<T>> mGraph = new HashMap<T, Set<T>>();

    /**
     * Adds a new node to the graph. If the node already exists, this function is a no-op.
     * 
     * @param node The node to add.
     * @return Whether or not the node was added.
     */
    public boolean addNode(T node)
    {
        /* If the node already exists, don't do anything. */
        if (mGraph.containsKey(node))
            return false;

        /* Otherwise, add the node with an empty set of outgoing edges. */
        mGraph.put(node, new HashSet<T>());
        return true;
    }

    /**
     * Given a start node, and a destination, adds an arc from the start node to the destination. If an arc already exists, this
     * operation is a no-op. If either endpoint does not exist in the graph, throws a NoSuchElementException.
     * 
     * @param start The start node.
     * @param dest The destination node.
     * @throws NoSuchElementException If either the start or destination nodes do not exist.
     */
    public void addEdge(T start, T dest)
    {
        /* Confirm both endpoints exist. */
        if (!mGraph.containsKey(start) || !mGraph.containsKey(dest))
            throw new NoSuchElementException("Both nodes must be in the graph: " + start.toString() + " and " + dest.toString());

        /* Add the edge. */
        mGraph.get(start).add(dest);
    }

    /**
     * Removes the edge from start to dest from the graph. If the edge does not exist, this operation is a no-op. If either
     * endpoint does not exist, this throws a NoSuchElementException.
     * 
     * @param start The start node.
     * @param dest The destination node.
     * @throws NoSuchElementException If either node is not in the graph.
     */
    public void removeEdge(T start, T dest)
    {
        /* Confirm both endpoints exist. */
        if (!mGraph.containsKey(start) || !mGraph.containsKey(dest))
            throw new NoSuchElementException("Both nodes must be in the graph.");

        mGraph.get(start).remove(dest);
    }

    /**
     * Given two nodes in the graph, returns whether there is an edge from the first node to the second node. If either node does
     * not exist in the graph, throws a NoSuchElementException.
     * 
     * @param start The start node.
     * @param end The destination node.
     * @return Whether there is an edge from start to end.
     * @throws NoSuchElementException If either endpoint does not exist.
     */
    public boolean edgeExists(T start, T end)
    {
        /* Confirm both endpoints exist. */
        if (!mGraph.containsKey(start) || !mGraph.containsKey(end))
            throw new NoSuchElementException("Both nodes must be in the graph.");

        return mGraph.get(start).contains(end);
    }

    /**
     * Given a node in the graph, returns an immutable view of the edges leaving that node as a set of endpoints.
     * 
     * @param node The node whose edges should be queried.
     * @return An immutable view of the edges leaving that node.
     * @throws NoSuchElementException If the node does not exist.
     */
    public Set<T> edgesFrom(T node)
    {
        /* Check that the node exists. */
        Set<T> arcs = mGraph.get(node);
        if (arcs == null)
            throw new NoSuchElementException("Source node does not exist.");

        return Collections.unmodifiableSet(arcs);
    }

    /**
     * Returns an iterator that can traverse the nodes in the graph.
     * 
     * @return An iterator that traverses the nodes in the graph.
     */
    public Iterator<T> iterator()
    {
        return mGraph.keySet().iterator();
    }

    /**
     * Returns the number of nodes in the graph.
     * 
     * @return The number of nodes in the graph.
     */
    public int size()
    {
        return mGraph.size();
    }

    /**
     * Returns whether the graph is empty.
     * 
     * @return Whether the graph is empty.
     */
    public boolean isEmpty()
    {
        return mGraph.isEmpty();
    }
}

class TopologicalSort
{
    /**
     * Given a directed acyclic graph, returns a topological sorting of the nodes in the graph. If the input graph is not a DAG,
     * throws an IllegalArgumentException.
     *
     * @param g A directed acyclic graph.
     * @return A topological sort of that graph.
     * @throws IllegalArgumentException If the graph is not a DAG.
     */
    public static <T> List<T> sort(DirectedGraph<T> g)
    {
        // Construct the reverse graph from the input graph.
        DirectedGraph<T> gRev = reverseGraph(g);

        // Maintain two structures - a set of visited nodes (so that once we've added a node to the list, we don't label it
        // again), and a list of nodes that actually holds the topological ordering.
        List<T> result = new ArrayList<T>();
        Set<T> visited = new HashSet<T>();

        // We'll also maintain a third set consisting of all nodes that have been fully expanded. If the graph contains a cycle,
        // then we can detect this by noting that a node has been explored but not fully expanded.
        Set<T> expanded = new HashSet<T>();

        // Fire off a DFS from each node in the graph.
        for (T node : gRev)
            explore(node, gRev, result, visited, expanded);

        // Hand back the resulting ordering.
        return result;
    }

    /**
     * Recursively performs a DFS from the specified node, marking all nodes encountered by the search.
     *
     * @param node The node to begin the search from.
     * @param g The graph in which to perform the search.
     * @param ordering A list holding the topological sort of the graph.
     * @param visited A set of nodes that have already been visited.
     * @param expanded A set of nodes that have been fully expanded.
     */
    private static <T> void explore(T node, DirectedGraph<T> g, List<T> ordering, Set<T> visited, Set<T> expanded)
    {
        //Check whether we've been here before. If so, we should stop the search.
        if (visited.contains(node))
        {
            // There are two cases to consider. First, if this node has already been expanded, then it's already been assigned a
            // position in the final topological sort and we don't need to explore it again. However, if it hasn't been expanded,
            // it means that we've just found a node that is currently being explored, and therefore is part of a cycle. In that
            // case, we should report an error.
            if (expanded.contains(node))
                return;
            throw new IllegalArgumentException("Graph contains a cycle.");
        }

        // Mark that we've been here
        visited.add(node);

        // Recursively explore all of the node's predecessors.
        for (T predecessor : g.edgesFrom(node))
            explore(predecessor, g, ordering, visited, expanded);

        // Having explored all of the node's predecessors, we can now add this node to the sorted ordering.
        ordering.add(node);

        // Similarly, mark that this node is done being expanded.
        expanded.add(node);
    }

    /**
     * Returns the reverse of the input graph.
     *
     * @param g A graph to reverse.
     * @return The reverse of that graph.
     */
    private static <T> DirectedGraph<T> reverseGraph(DirectedGraph<T> g)
    {
        DirectedGraph<T> result = new DirectedGraph<T>();

        // Add all the nodes from the original graph.
        for (T node : g)
            result.addNode(node);

        // Scan over all the edges in the graph, adding their reverse to the reverse graph.
        for (T node : g)
            for (T endpoint : g.edgesFrom(node))
                result.addEdge(endpoint, node);

        return result;
    }
}