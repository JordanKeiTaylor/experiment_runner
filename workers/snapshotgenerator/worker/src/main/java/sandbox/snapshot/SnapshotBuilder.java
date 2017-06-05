package sandbox.snapshot;

import improbable.*;
import improbable.collections.Option;
import improbable.math.Coordinates;
import improbable.math.Vector3f;
import improbable.worker.EntityId;
import improbable.worker.SnapshotEntity;
import sandbox.*;
import subscriber.Connection;
import subscriber.ConnectionData;
import subscriber.Connection;
import subscriber.ConnectionData;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SnapshotBuilder extends HashMap<EntityId, SnapshotEntity> {

    private static final String ENGINE_ATTRIBUTE_NAME = "engine";
    private static final String THEIA_ATTRIBUTE_NAME = "theia";

    private long nextId = 200;

    private Random random = new Random();

    public void createSubscriber(Coordinates position) {
        EntityId entityId = new EntityId(this.nextId++);

        SnapshotEntity entity = new SnapshotEntity("subscriber");
        entity.add(Position.class, new PositionData(position));
        entity.add(Visualise.class, new VisualiseData());
        entity.add(Firefly.class, newFireflyComponent());

        int[] writeList = {
                Position.COMPONENT_ID,
                Firefly.COMPONENT_ID
        };

        entity.add(EntityAcl.class, createAcl(ENGINE_ATTRIBUTE_NAME, writeList));
        this.put(entityId, entity);
    }

    public FireflyData newFireflyComponent() {
        float clockTime = random.nextFloat() * 100f;
        float currentTime = random.nextFloat() * clockTime;

        return new FireflyData(clockTime, currentTime, false);
    }

    public void createProvider(Coordinates position) {
        EntityId entityId = new EntityId(this.nextId++);

        SnapshotEntity entity = new SnapshotEntity("provider");
        entity.add(Position.class, new PositionData(position));

        int[] writeList = {
                Position.COMPONENT_ID
        };

        entity.add(EntityAcl.class, createAcl(ENGINE_ATTRIBUTE_NAME, writeList));
        this.put(entityId, entity);
    }

    private EntityAclData createAcl(String attributeName, int[] writeList) {
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
}
