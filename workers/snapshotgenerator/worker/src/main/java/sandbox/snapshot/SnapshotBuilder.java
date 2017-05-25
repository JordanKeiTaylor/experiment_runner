package sandbox.snapshot;

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

public class SnapshotBuilder extends HashMap<EntityId, SnapshotEntity> {

    private static final String ENGINE_ATTRIBUTE_NAME = "engine";

    private long nextId = 1;

    public void createParticle(Coordinates position) {
        EntityId entityId = new EntityId(this.nextId++);

        SnapshotEntity entity = new SnapshotEntity("particle");
        entity.add(Position.class, new PositionData(position));

        WorkerAttribute particleAtom = new WorkerAttribute(Option.of(ENGINE_ATTRIBUTE_NAME));
        List<WorkerAttribute> particleAttributeList = new LinkedList<>();
        particleAttributeList.add(particleAtom);

        WorkerAttributeSet particleAttributeSet = new WorkerAttributeSet(particleAttributeList);

        List<WorkerAttributeSet> attributeSets = new LinkedList<>();
        attributeSets.add(particleAttributeSet);

        WorkerRequirementSet writeRequirementSet = new WorkerRequirementSet(attributeSets);

        List<WorkerAttributeSet> readAttributeSets = new LinkedList<>();
        readAttributeSets.add(particleAttributeSet);

        Map<Integer, WorkerRequirementSet> writeRequirementSets = new HashMap<>();
        writeRequirementSets.put(Position.COMPONENT_ID, writeRequirementSet);

        WorkerRequirementSet read = new WorkerRequirementSet(readAttributeSets);
        ComponentAcl write = new ComponentAcl(writeRequirementSets);

        entity.add(EntityAcl.class, new EntityAclData(Option.of(read), Option.of(write)));

        this.put(entityId, entity);
    }

    public void createSubscriber(Coordinates position) {
        EntityId entityId = new EntityId(this.nextId++);

        SnapshotEntity entity = new SnapshotEntity("subscriber");
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

        List<WorkerAttributeSet> attributeSets = new LinkedList<>();
        attributeSets.add(particleAttributeSet);

        WorkerRequirementSet writeRequirementSet = new WorkerRequirementSet(attributeSets);

        List<WorkerAttributeSet> readAttributeSets = new LinkedList<>();
        readAttributeSets.add(particleAttributeSet);

        Map<Integer, WorkerRequirementSet> writeRequirementSets = new HashMap<>();
        for (int componentId : writeList) {
            writeRequirementSets.put(componentId, writeRequirementSet);
        }

        ComponentAcl write = new ComponentAcl(writeRequirementSets);
        WorkerRequirementSet read = new WorkerRequirementSet(readAttributeSets);

        return new EntityAclData(Option.of(read), Option.of(write));
    }
}
