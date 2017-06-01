package sandbox.worker;

import improbable.collections.Option;
import improbable.worker.*;

import improbable.worker.Ops.AddComponent;
import improbable.worker.Ops.AuthorityChange;
import improbable.worker.Ops.RemoveComponent;

import sandbox.*;

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
                    //physics.applyPhysics(PHYSICS_DELTA);
                }
                //physics.sendUpdates(connection);
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

        dispatcher.onAuthorityChange(Position.class, EngineWorker::authorityChange);
        dispatcher.onAddComponent(Position.class, EngineWorker::addParticle);
        dispatcher.onComponentUpdate(Position.class, EngineWorker::updateParticle);
        dispatcher.onRemoveComponent(Position.class, EngineWorker::removeParticle);

        return dispatcher;
    }

    private static void authorityChange(AuthorityChange op) {
    }

    private static void addParticle(AddComponent<PositionData, Position> op) {
    }

    private static void updateParticle(Ops.ComponentUpdate<Position.Update> op) {
    }

    private static void removeParticle(RemoveComponent op) {
    }    

    private static void sendMetricsUpdate(Ops.Metrics metricsOp) {
        Metrics metrics = metricsOp.metrics;
        metrics.load = Option.of(EngineWorker.load);
        connection.sendMetrics(metrics);
    }


    private static void processOpList() {
        try (OpList opList = connection.getOpList(0)) {
            dispatcher.process(opList);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
