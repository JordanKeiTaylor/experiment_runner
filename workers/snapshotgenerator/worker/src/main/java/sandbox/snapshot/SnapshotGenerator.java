package sandbox.snapshot;

import improbable.collections.Option;
import improbable.math.Coordinates;
import improbable.math.Vector3f;
import improbable.worker.Snapshot;
import sandbox.snapshot.SnapshotBuilder;

public class SnapshotGenerator {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Unexpected number of arguments. Usage: " +
                    "spatial local worker launch snapshotgenerator default path/to/my/snapshotfile.snapshot");
        } else {
            buildSnapshot(args[0]);
        }
    }


    static void buildSnapshot(String output) {
        SnapshotBuilder snapshot = new SnapshotBuilder();

        snapshot.createParticle( new Coordinates(0.0f, 0.0f, 0.0f), new Vector3f(-1.4f,0.0f,1.1f) );

        Option<String> errorOpt = Snapshot.save(output, snapshot);
        if (errorOpt.isPresent()) {
            throw new RuntimeException("Error saving snapshot: " + errorOpt.get());
        } else {
            System.out.println("Wrote snapshot file to \"" + output + "\"  - wrote ("+snapshot.size()+" entities)");
        }
    }

}
