package sandbox.worker;

import improbable.collections.Option;
import improbable.worker.*;

import improbable.worker.Ops.AddComponent;
import improbable.worker.Ops.AuthorityChange;
import improbable.worker.Ops.RemoveComponent;

import sandbox.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class EngineWorker {

    private static final long TIMESTEP = 10; // 100 frames a second
    private static final float DELTA = 0.1f; // value passed to physics
    private static String workerId;
    private static Connection connection;
    private static Dispatcher dispatcher;
    private static long millisecondsPerFrame = 100; // opslist target process value
    private static double load;
    private static final Logger logger = new Logger();
    private static RequestId<EntityQueryRequest> statsRequestId;

    private static HashMap<EntityId, FireflyModel> fireflies = new HashMap<EntityId, FireflyModel>();
    private static HashMap<EntityId, LinksModel> links = new HashMap<EntityId, LinksModel>();

    public static void startWorker(String[] args) {
        initializeWorker(args);
        
        logger.warn("Engine worker starting");

        long start;
        long end;
        long lastStep = System.currentTimeMillis();
        long elapsedMillis;
        while (true) {
            start = System.currentTimeMillis();

            processOpList();

            end = System.currentTimeMillis();
            int updates = (int) Math.floor((end - lastStep) / TIMESTEP);
            if (updates > 0) {
                while (updates > 0) {
                    updates--;
                    lastStep += TIMESTEP;
                    updateFireflies(DELTA);
                }
                sendUpdates(connection);
            }

            end = System.currentTimeMillis();

            elapsedMillis = end - start;
            load = elapsedMillis / millisecondsPerFrame;
            try {
                Thread.sleep(Math.max(1, millisecondsPerFrame - elapsedMillis));
            } catch (InterruptedException ignored) {

            }
        }
    }

    private static boolean initializeWorker(String[] args) {
        Map<String, String> argsMap = mapArgs(args);
        workerId = argsMap.get("+workerId");
        if (workerId == null) {
            return true;
        } else {
            connect(argsMap);
            dispatcher = initializeDispatcher();
            return false;
        }
    }

    private static Map<String, String> mapArgs(String[] args) {
        Map<String, String> argsMap = new HashMap<>();
        for (String arg : args) {
            String[] kv = arg.split("=");
            if (kv.length == 2) {
                argsMap.put(kv[0], kv[1]);
            } else {
                argsMap.put(kv[0], "true");
            }
        }

        return argsMap;
    }

    private static void connect(Map<String, String> argsMap) {
        String hostname = argsMap.get("+receptionistIp");
        int port = Integer.parseInt(argsMap.get("+receptionistPort"));

        ConnectionParameters parameters = new ConnectionParameters();
        parameters.workerType = argsMap.get("+workerType");
        parameters.workerId = argsMap.get("+workerId");
        parameters.networkParameters.type = NetworkConnectionType.RakNet;
        parameters.networkParameters.useExternalIp = false;

        connection = Connection.connectAsync(hostname, port, parameters).get();

        if (!connection.isConnected()) {
            logger.warn("Failed to connect");
            System.exit(2);
        }

        logger.connect(connection, workerId);
    }

    private static Dispatcher initializeDispatcher() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.onDisconnect(disconnect -> System.exit(0));
        dispatcher.onMetrics(EngineWorker::sendMetricsUpdate);

        dispatcher.onAuthorityChange(Firefly.class, EngineWorker::authorityChange);
        dispatcher.onAddComponent(Firefly.class, EngineWorker::addFirefly);
        dispatcher.onComponentUpdate(Firefly.class, EngineWorker::updateFirefly);
        dispatcher.onRemoveComponent(Firefly.class, EngineWorker::removeFirefly);

        dispatcher.onAuthorityChange(Links.class, EngineWorker::authorityChange);
        dispatcher.onAddComponent(Links.class, EngineWorker::addLinks);
        dispatcher.onComponentUpdate(Links.class, EngineWorker::updateLinks);
        dispatcher.onRemoveComponent(Links.class, EngineWorker::removeLinks);

        return dispatcher;
    }

    private static void addLinkstoFirefly(LinksModel links, FireflyModel firefly) {
        links.links.stream().forEach(id -> firefly.addNeighbour(id));
    }

    private static void authorityChange(AuthorityChange op) {
    }

    private static void addLinks(AddComponent<LinksData, Links> op) {
        LinksModel neighbours = new LinksModel(op);

        //if firefly exists then add the links
        FireflyModel firefly = fireflies.get(op.entityId);
        if (firefly != null) {
            addLinkstoFirefly(neighbours, firefly);
        }

        links.put(op.entityId, neighbours);
    }

    private static void updateLinks(Ops.ComponentUpdate<Links.Update> op) {
    }

    private static void removeLinks(RemoveComponent op) {
    }

    private static void addFirefly(AddComponent<FireflyData, Firefly> op) {
        FireflyModel firefly = new FireflyModel(op);

        //add links to firefly if they already exist
        LinksModel neighbours = links.get(op.entityId);
        if (neighbours != null) {
            addLinkstoFirefly(neighbours, firefly);
        }

        fireflies.put(op.entityId, firefly);
    }

    private static void updateFirefly(Ops.ComponentUpdate<Firefly.Update> op) {
    }

    private static void removeFirefly(RemoveComponent op) {
    }

    private static void sendMetricsUpdate(Ops.Metrics metricsOp) {
        Metrics metrics = metricsOp.metrics;
        metrics.load = Option.of(EngineWorker.load);
        connection.sendMetrics(metrics);
    }

    private static void updateFireflies(float delta) {
        for (FireflyModel firefly : fireflies.values()) {
            firefly.update(delta);
        }
    }

    private static void sendUpdates(Connection connection) {
        for (FireflyModel firefly : fireflies.values()) {
            if (firefly.shouldUpdateSpatial) {
                firefly.sendUpdate(connection);
            }
        }
    }

    private static void processOpList() {
        try (OpList opList = connection.getOpList(0)) {
            dispatcher.process(opList);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}

class LinksModel {
    List<EntityId> links;

    public LinksModel(AddComponent<LinksData, Links> op) {
        links = op.data.getIds();
    }
}

class FireflyModel {
    final float clockTime;
    float currentTime;
    boolean illuminated;
    float currentIlluminationDuration;
    float illuminationTimeout = 10f;
    EntityId entityId;
    List<EntityId> neighbours = new ArrayList<EntityId>();

    public boolean shouldUpdateSpatial = false;

    public FireflyModel(AddComponent<FireflyData, Firefly> op) {
        entityId = op.entityId;

        clockTime = op.data.getClockTime();
        illuminated = op.data.getIlluminated();
        currentTime = op.data.getCurrentTime();
        currentIlluminationDuration = 0;
    }

    public void addNeighbour(EntityId id) {
        neighbours.add(id);
    }

    public void update(float delta) {
        currentTime += delta;

        shouldUpdateSpatial = true;

        if (illuminated) {
            currentIlluminationDuration += delta;
        }

        if (currentTime > clockTime) {
            illuminated = true;
            shouldUpdateSpatial = true;

            currentTime %= clockTime;
        } else {
            if (currentIlluminationDuration > illuminationTimeout) {
                illuminated = false;
                currentIlluminationDuration = 0;
                shouldUpdateSpatial = true;
            }
        }
    }

    public void sendUpdate(Connection connection) {
        Firefly.Update update = new Firefly.Update();

        update.setIlluminated(illuminated);
        update.setCurrentTime(currentTime);

        connection.sendComponentUpdate(this.entityId, update);

        shouldUpdateSpatial = false;
    }
}