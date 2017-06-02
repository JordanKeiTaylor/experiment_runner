package sandbox.snapshot;

import improbable.collections.Option;
import improbable.math.Coordinates;
import improbable.math.Vector3f;
import improbable.worker.Snapshot;
import sandbox.snapshot.GraphGenerator;
import sandbox.snapshot.SnapshotBuilder;
import java.util.Random;

public class SnapshotGenerator {
    static Random random = new Random();
    static SnapshotBuilder snapshot = new SnapshotBuilder();
    static GraphGenerator graphGenerator;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Unexpected number of arguments. Usage: " +
                    "spatial local worker launch snapshotgenerator default path/to/my/snapshotfile.snapshot");
        } else {
            buildGraphSnapshot(args[0]);
        }
    }

    static void buildGraphSnapshot(String output) {
        GraphGenerator.letsGo();

        snapshot.createProvider(new Coordinates(0.0f,0.0f,0.0f));

        Option<String> errorOpt = Snapshot.save(output, GraphGenerator.entities);
        if (errorOpt.isPresent()) {
            throw new RuntimeException("Error saving snapshot: " + errorOpt.get());
        } else {
            System.out.println("Wrote snapshot file to \"" + output + "\"  - wrote ("+GraphGenerator.entities.size()+" entities)");
        }
    }

    static void buildSnapshot(String output) {
        generateCluster(null, 200, 4, 4);

        snapshot.createProvider(new Coordinates(0.0f,0.0f,0.0f));

        Option<String> errorOpt = Snapshot.save(output, snapshot);
        if (errorOpt.isPresent()) {
            throw new RuntimeException("Error saving snapshot: " + errorOpt.get());
        } else {
            System.out.println("Wrote snapshot file to \"" + output + "\"  - wrote ("+snapshot.size()+" entities)");
        }
    }

    static Coordinates randomCoordinates(double spread) {
        return new Coordinates(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread);
    }

    static Coordinates addCoordinates(Coordinates coord1, Coordinates coord2) {
        return new Coordinates(coord1.getX() + coord2.getX(), coord1.getY() + coord2.getY(), coord1.getZ() + coord2.getZ());
    }

    static void generateCluster(Coordinates center, double spread, int numberOfEntities, int depth) {
        if (depth <= 0) {
            return;
        }

        for (int i = 0; i < numberOfEntities; i++) {
            //create entitity
            Coordinates position = (center == null) ? randomCoordinates(spread) : addCoordinates(randomCoordinates(spread), center);
            snapshot.createSubscriber(position);;

            //create new cluster around entity
            generateCluster(position, spread /=1.3, numberOfEntities, depth-1);
        }
    }
}
