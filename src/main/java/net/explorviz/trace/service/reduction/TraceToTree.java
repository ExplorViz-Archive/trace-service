package net.explorviz.trace.service.reduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import net.explorviz.avro.SpanDynamic;
import net.explorviz.avro.Trace;

public class TraceToTree {

  public static CallTree fromTrace(Trace trace) {
    HashMap<String, CallTreeNode> knownNodes = new HashMap<>();
    List<CallTreeNode> orphans = new ArrayList<>();
    CallTreeNode root = null;

    for (SpanDynamic sd : trace.getSpanList()) {
      String sid = sd.getSpanId();
      CallTreeNode ctn = new CallTreeNode(sd);
      knownNodes.put(sid, ctn);

      Iterator<CallTreeNode> it = orphans.iterator();
      while (it.hasNext()) {
        CallTreeNode orphan = it.next();
        if (orphan.getSpanDynamic().getParentSpanId().equals(sid)) {
          ctn.addChild(orphan);
          orphans.remove(orphan);
        }
      }

      String pid = sd.getParentSpanId();
      if (pid.isBlank()) {
        root = ctn;
        continue;
      }
      CallTreeNode parent = knownNodes.get(pid);
      if (parent != null) {
        parent.addChild(ctn);
      } else {
        orphans.add(ctn);
      }
    }

    if (root == null) {
      throw new IllegalArgumentException("Invalid trace: No root");
    }
    if (orphans.size() > 0) {
      throw new IllegalArgumentException("Invalid trace: Not connected");
    }

    return new CallTree(root);
  }

}
