package org.saswata;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.driver.ser.Serializers;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.driver.SigV4WebSocketChannelizer;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.stream.Stream;


public class Main {
  public static void main(String[] args) {
    String neptune = args[0];
    String accountsSampleLoc = args[1];
    String timesDump = args[2];
    final int BATCH_SIZE = 32;

    Cluster cluster = clusterProvider(neptune, BATCH_SIZE);
    GraphTraversalSource g = graphProvider(cluster);

    runSuite(g, accountsSampleLoc, timesDump, BATCH_SIZE);

    cluster.close();
  }

  static void runSuite(GraphTraversalSource g, String accountsSampleLoc, String timesDump, final int BATCH_SIZE) {

    try (Stream<String> in = Files.lines(Paths.get(accountsSampleLoc));
         PrintWriter out = new PrintWriter(new FileWriter(timesDump));
         BatchExecutor batchExecutor = new BatchExecutor(out, BATCH_SIZE)) {

      in.forEach(acc -> batchExecutor.submit(() -> runQuery(g, acc.trim())));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static Cluster clusterProvider(String neptune, int BATCH_SIZE) {
    // disable DNS cache, to enable neptune dns load balancing on ro instances
    java.security.Security.setProperty("networkaddress.cache.ttl", "0");
    java.security.Security.setProperty("networkaddress.cache.negative.ttl", "0");

    Cluster.Builder clusterBuilder = Cluster.build()
        .addContactPoint(neptune) // add more ro contact points for load balancing
        .port(8182)
        .enableSsl(true)
        .channelizer(SigV4WebSocketChannelizer.class)
        .serializer(Serializers.GRAPHBINARY_V1D0)
        .maxInProcessPerConnection(1) // ensure no contention for connections per batch
        .minInProcessPerConnection(1)
        .maxSimultaneousUsagePerConnection(1)
        .minSimultaneousUsagePerConnection(1)
        .minConnectionPoolSize(BATCH_SIZE)
        .maxConnectionPoolSize(BATCH_SIZE);

    return clusterBuilder.create();
  }

  static GraphTraversalSource graphProvider(Cluster cluster) {
    RemoteConnection connection = DriverRemoteConnection.using(cluster);
    return AnonymousTraversalSource.traversal().withRemote(connection);
  }

  static final String[] edgeLabels = {"has_customer", "has_payer_account", "has_sfid"};

  static String runQuery(GraphTraversalSource g, String uid) {
    long start = Instant.now().toEpochMilli();

    GraphTraversal<Vertex, Object> t =
        g.withSideEffect("Neptune#repeatMode", "CHUNKED_DFS").V(uid).
            repeat(__.out(edgeLabels).simplePath()).
            until(__.outE(edgeLabels).limit(1).count().is(0)).
            repeat(__.both(edgeLabels).simplePath()).
            emit(__.hasLabel("account")).dedup().id();

    // avoid printing to get more accurate query times
    //t.forEachRemaining(System.out::println);
    int count = t.toSet().size();

    long stop = Instant.now().toEpochMilli();
    long time = stop - start;
    return uid + "," + time + "," + count;
  }

}
