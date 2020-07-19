package org.saswata;

import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection;
import org.apache.tinkerpop.gremlin.process.remote.RemoteConnection;
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

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

    Cluster cluster = clusterProvider(neptune);
    GraphTraversalSource g = graphProvider(cluster);

    final int BATCH_SIZE = 32;
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

  static Cluster clusterProvider(String neptune) {
    Cluster.Builder clusterBuilder = Cluster.build()
        .addContactPoint(neptune)
        .port(8182)
        .enableSsl(true)
        .keyCertChainFile("SFSRootCAG2.pem");

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

    int[] count = {0};
    t.forEachRemaining(e -> ++count[0]);
    // avoid printing to get more accurate query times

    long stop = Instant.now().toEpochMilli();
    long time = stop - start;
    return uid + "," + time + "," + count[0];
  }

}
