package sandbox.snapshot;

import java.io.*;
import java.util.*;
import improbable.collections.Option;
import improbable.math.Coordinates;
import improbable.math.Vector3f;
import improbable.worker.Snapshot;
import sandbox.snapshot.SnapshotBuilder;
import improbable.*;
import improbable.collections.Option;
import improbable.math.Coordinates;
import improbable.math.Vector3f;
import improbable.worker.EntityId;
import improbable.worker.SnapshotEntity;
import sandbox.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class GraphGenerator {
    private static final String ENGINE_ATTRIBUTE_NAME = "engine";
    private static final String THEIA_ATTRIBUTE_NAME = "theia";

    private static int NUMBER_NODES = 100;
    private static double AREA_X_LENGTH = 100;
    private static double AREA_Y_LENGTH = 10;
    private static double AREA_Z_LENGTH = 100;

    private static ArrayList<Node> nodes;
    private static ArrayList<Edge> edges;

    public static HashMap<EntityId, SnapshotEntity> entities = new HashMap<EntityId, SnapshotEntity>();

    static private long nextId = 2000;

    public static void letsGo() {
        if (NUMBER_NODES < 2) {
            System.out.println("Cannot create a network containing less than 2 nodes.");
            return;
        }

        nodes = createNodes();
        edges = createTree();

        for (Node node : nodes) {
            createNode(node, edges.stream().filter(edge -> edge.nodeAId == node.id).map(Edge::getNodeBId).collect(Collectors.toCollection(ArrayList<EntityId>::new)));
        }

        for (Edge edge : edges) {
            System.out.println(edge);
        }
    }

    static void createNode(Node node, List<EntityId> edges) {
        EntityId entityId = new EntityId(node.id);

        SnapshotEntity entity = new SnapshotEntity("subscriber");
        entity.add(Position.class, new PositionData(new Coordinates(node.x, node.y, node.z)));
        entity.add(Links.class, new LinksData(edges));

        entity.add(Visualise.class, new VisualiseData());

        int[] writeList = {
                Position.COMPONENT_ID
        };

        entity.add(EntityAcl.class, createAcl(ENGINE_ATTRIBUTE_NAME, writeList));

        entities.put(new EntityId(node.id), entity);
    }

    private static EntityAclData createAcl(String attributeName, int[] writeList) {
        WorkerAttribute particleAtom = new WorkerAttribute(Option.of(attributeName));
        List<WorkerAttribute> particleAttributeList = new LinkedList<>();
        particleAttributeList.add(particleAtom);
        WorkerAttributeSet particleAttributeSet = new WorkerAttributeSet(particleAttributeList);

        WorkerAttribute theia = new WorkerAttribute(Option.of(THEIA_ATTRIBUTE_NAME));
        List<WorkerAttribute> theiaAttributeList = new LinkedList<>();
        theiaAttributeList.add(theia);
        WorkerAttributeSet theiaAttributeSet = new WorkerAttributeSet(theiaAttributeList);

        List<WorkerAttributeSet> attributeSets = new LinkedList<>();
        attributeSets.add(particleAttributeSet);
        WorkerRequirementSet writeRequirementSet = new WorkerRequirementSet(attributeSets);

        List<WorkerAttributeSet> visualiseSets = new LinkedList<>();
        visualiseSets.add(theiaAttributeSet);
        WorkerRequirementSet visualiseRequirementSet = new WorkerRequirementSet(visualiseSets);

        List<WorkerAttributeSet> readAttributeSets = new LinkedList<>();
        readAttributeSets.add(particleAttributeSet);
        readAttributeSets.add(theiaAttributeSet);

        Map<Integer, WorkerRequirementSet> writeRequirementSets = new HashMap<>();
        for (int componentId : writeList) {
            writeRequirementSets.put(componentId, writeRequirementSet);
        }

        writeRequirementSets.put(Visualise.COMPONENT_ID, visualiseRequirementSet);

        ComponentAcl write = new ComponentAcl(writeRequirementSets);
        WorkerRequirementSet read = new WorkerRequirementSet(readAttributeSets);

        return new EntityAclData(Option.of(read), Option.of(write));
    }

    public static ArrayList<Node> createNodes() {
        ArrayList<Node> nodes = new ArrayList<Node>();
        double xMax = AREA_X_LENGTH/2;
        double xMin = -xMax;
        double yMax = AREA_Y_LENGTH/2;
        double yMin = -yMax;
        double zMax = AREA_Z_LENGTH/2;
        double zMin = -zMax;

        for (int i = 0; i < NUMBER_NODES; i++) {
            double x = randomDoubleInRange(xMin, xMax);
            double y = randomDoubleInRange(yMin, yMax);
            double z = randomDoubleInRange(zMin, zMax);
            nodes.add(new Node(i, x, y, z));
        }

        return nodes;
    }

    public static double randomDoubleInRange(double min, double max) {
        return min + (max - min) * Math.random();
    }

    public static ArrayList<Edge> createTree() {
        ArrayList<Node> nodesNotInTree = new ArrayList<Node>();
        nodesNotInTree.addAll(nodes);
        nodesNotInTree.remove(nodesNotInTree.get(0));

        ArrayList<Node> nodesInTree = new ArrayList<Node>();
        nodesInTree.add(nodes.get(0));

        ArrayList<Edge> edges = new ArrayList<Edge>();
        double maxDimension = getMaxDimension();

        while (nodesNotInTree.size() > 0) {
            double minDist = maxDimension;
            Node nodeInTree = null;
            Node nodeNotInTree = null;

            for (Node nodeA : nodesInTree) {
                for (Node nodeB : nodesNotInTree) {
                    double dist = nodeA.distanceTo(nodeB);
                    if (dist < minDist) {
                        minDist = dist;
                        nodeInTree = nodeA;
                        nodeNotInTree = nodeB;
                    }
                }
            }

            nodesNotInTree.remove(nodeNotInTree);
            nodesInTree.add(nodeNotInTree);
            edges.add(new Edge(nodeInTree.id, nodeNotInTree.id, minDist));
        }

        return edges;
    }

    private static double getMaxDimension() {
        return Math.max(AREA_X_LENGTH, Math.max(AREA_Y_LENGTH, AREA_Z_LENGTH));
    }
}


class Node {

    public int id;
    public double x;
    public double y;
    public double z;

    public Node(int id, double x, double y, double z) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double distanceTo(Node otherNode) {
        double dx = Math.abs(otherNode.x - x);
        double dy = Math.abs(otherNode.y - y);
        double dz = Math.abs(otherNode.z - z);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}


class Edge {

    public int nodeAId;
    public int nodeBId;
    public double length;

    public Edge(int nodeAId, int nodeBId, double length) {
        this.nodeAId = nodeAId;
        this.nodeBId = nodeBId;
        this.length = length;
    }

    @Override
    public String toString() {
        return nodeAId + ", " + nodeBId;
    }

    public EntityId getNodeBId() {
        return new EntityId(nodeBId);
    }
}