package net.explorviz.trace.service.reduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleLoopReducer implements SpanReducer {

  @Override
  public CallTree reduce(final CallTree tree) {
    Map<Integer, List<CallTreeNode>> leavesPerLevel = new HashMap<>(tree.size() / 2);
    tree.bfs(n -> {
      if (n.isLeaf()) {
        int l = n.getLevel();
        if (leavesPerLevel.containsKey(l)) {
          leavesPerLevel.get(l).add(n);
        } else {
          List<CallTreeNode> leaves = new ArrayList<>();
          leaves.add(n);
          leavesPerLevel.put(l, leaves);
        }
      }
    });

    Map<Integer, List<CallTreeNode>> reducible = findReducibleNodesPerLevel(leavesPerLevel);

    // Keep track of nodes already in the new tree, isomorphism from original to reduced tree
    HashMap<CallTreeNode, CallTreeNode> addedNodes = new HashMap<>(tree.size());
    CallTreeNode newRoot = null;
    for (int level : leavesPerLevel.keySet()) {
      List<CallTreeNode> leafOnLevel = leavesPerLevel.get(level);
      List<CallTreeNode> reducibleOnLevel = reducible.get(level);
      for (CallTreeNode leaf : leafOnLevel) {

        if (!reducibleOnLevel.contains(leaf)) {
          CallTreeNode newNode = new CallTreeNode(leaf.getSpanDynamic());
          addedNodes.put(leaf, newNode);
          CallTreeNode child = newNode;
          CallTreeNode current = leaf.getParent();
          while (true) {
            if (addedNodes.containsKey(current)) {
              // Already in new tree, just add child and break loop
              CallTreeNode parent = addedNodes.get(current);
              parent.addChild(child);
              break;
            }
            // Create new node for parent and add child
            CallTreeNode parent = new CallTreeNode(current.getSpanDynamic());
            addedNodes.put(current, parent);
            parent.addChild(child);
            // Proceed with next parent
            if (current.isRoot()) {
              newRoot = parent;
            } else {
              current = current.getParent();
              child = parent;
            }
          }
        }

      }
    }

    return new CallTree(newRoot);
  }

  private Map<Integer, List<CallTreeNode>> findReducibleNodesPerLevel(
      Map<Integer, List<CallTreeNode>> leavesPerLevel) {

    Map<Integer, List<CallTreeNode>> reducibleNodes = new HashMap<>();
    // For all leaves u,v on the same level, check if path to LCA(u,v) contains the same hashcodes.
    // If so, they can be reduced.
    for (int level : leavesPerLevel.keySet()) {
      // Contains the leaves that can be removed on this level
      List<CallTreeNode> reducibleNodesOnLevel = new ArrayList<>();
      List<CallTreeNode> nodes = leavesPerLevel.get(level);
      for (int i = 0; i < nodes.size(); i++) {
        CallTreeNode u = nodes.get(i);
        if (reducibleNodesOnLevel.contains(u)) {
          continue;
        }
        for (int j = i + 1; j < nodes.size(); j++) {
          CallTreeNode v = nodes.get(j);
          if (reducibleNodesOnLevel.contains(v)) {
            continue;
          }
          List<CallTreeNode>[] pathsToLca = sameLevelPathToLca(u, v);
          List<CallTreeNode> uToLca = pathsToLca[0];
          List<CallTreeNode> vToLca = pathsToLca[1];

          // Check if both paths are equal w.r.t. the hashcodes of referenced methods
          boolean isEqualPath = true;
          for (int k = 0; k < uToLca.size() && isEqualPath; k++) {
            isEqualPath = vToLca.get(k).getSpanDynamic().getHashCode().equals(
                uToLca.get(k).getSpanDynamic().getHashCode()
            );
          }

          // v can be reduced
          if (isEqualPath) {
            reducibleNodesOnLevel.add(v);
          }
        }
      }
      reducibleNodes.put(level, reducibleNodesOnLevel);
    }
    return reducibleNodes;
  }


  private List<CallTreeNode>[] sameLevelPathToLca(CallTreeNode u, CallTreeNode v) {
    if (u.getLevel() != v.getLevel()) {
      throw new IllegalArgumentException("Can only calculate for nodes on same level");
    }
    List<CallTreeNode> lcau = new ArrayList<>();
    List<CallTreeNode> lcav = new ArrayList<>();

    CallTreeNode cu = u;
    CallTreeNode cv = v;

    do {
      lcau.add(cu);
      lcav.add(cv);
      cu = cu.getParent();
      cv = cv.getParent();
    } while (cu != cv);

    return new List[] {lcau, lcav};
  }

}
