package net.explorviz.trace.service.reduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link SpanReducer} implementation that reduces spans of a {@link CallTree}.
 */
public class SimpleLoopReducer implements SpanReducer {

  @Override
  public CallTree reduce(final CallTree tree) { // NOPMD
    final Map<Integer, List<CallTreeNode>> leavesPerLevel = new HashMap<>(tree.size() / 2);
    tree.bfs(n -> {
      if (n.isLeaf()) {
        final int l = n.getLevel();
        if (leavesPerLevel.containsKey(l)) {
          leavesPerLevel.get(l).add(n);
        } else {
          final List<CallTreeNode> leaves = new ArrayList<>();
          leaves.add(n);
          leavesPerLevel.put(l, leaves);
        }
      }
    });

    final Map<Integer, List<CallTreeNode>> reducible =
        this.findReducibleNodesPerLevel(leavesPerLevel);
    // final boolean reduced = false;
    // Keep track of nodes already in the new tree, isomorphism from original to reduced tree
    final HashMap<CallTreeNode, CallTreeNode> addedNodes = new HashMap<>(tree.size());
    CallTreeNode newRoot = null;
    for (final int level : leavesPerLevel.keySet()) {
      final List<CallTreeNode> leafOnLevel = leavesPerLevel.get(level);
      final List<CallTreeNode> reducibleOnLevel = reducible.get(level);
      for (final CallTreeNode leaf : leafOnLevel) {

        if (!reducibleOnLevel.contains(leaf)) {

          final CallTreeNode newNode = new CallTreeNode(leaf.getSpanDynamic());
          addedNodes.put(leaf, newNode);
          CallTreeNode child = newNode;
          CallTreeNode current = leaf.getParent();
          while (true) {
            if (current == null) {
              // Is root
              newRoot = child;
              break;
            } else {
              if (addedNodes.containsKey(current)) {
                // Already in new tree, just add child and break loop
                final CallTreeNode parent = addedNodes.get(current);
                parent.addChild(child);
                break;
              }
              // Create new node for parent and add child
              final CallTreeNode parent = new CallTreeNode(current.getSpanDynamic());
              addedNodes.put(current, parent);
              parent.addChild(child);
              // Proceed with next parent

              current = current.getParent();
              child = parent;
            }
          }
        }

      }
    }


    return new CallTree(newRoot);

  }

  private Map<Integer, List<CallTreeNode>> findReducibleNodesPerLevel( // NOPMD
      final Map<Integer, List<CallTreeNode>> leavesPerLevel) {

    final Map<Integer, List<CallTreeNode>> reducibleNodes = new HashMap<>();
    // For all leaves u,v on the same level, check if path to LCA(u,v) contains the same hashcodes.
    // If so, they can be reduced.
    for (final int level : leavesPerLevel.keySet()) {
      // Contains the leaves that can be removed on this level
      final List<CallTreeNode> reducibleNodesOnLevel = new ArrayList<>();
      final List<CallTreeNode> nodes = leavesPerLevel.get(level);
      for (int i = 0; i < nodes.size(); i++) {
        final CallTreeNode u = nodes.get(i);
        if (reducibleNodesOnLevel.contains(u)) {
          continue;
        }
        for (int j = i + 1; j < nodes.size(); j++) {
          final CallTreeNode v = nodes.get(j);
          if (reducibleNodesOnLevel.contains(v)) {
            continue;
          }
          final List<CallTreeNode>[] pathsToLca = this.sameLevelPathToLca(u, v);
          final List<CallTreeNode> uToLca = pathsToLca[0];
          final List<CallTreeNode> vToLca = pathsToLca[1];

          // Check if both paths are equal w.r.t. the hashcodes of referenced methods
          boolean isEqualPath = true;
          for (int k = 0; k < uToLca.size() && isEqualPath; k++) {
            isEqualPath = vToLca.get(k).getSpanDynamic().getHashCode().equals(
                uToLca.get(k).getSpanDynamic().getHashCode());
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


  private List<CallTreeNode>[] sameLevelPathToLca(final CallTreeNode u, final CallTreeNode v) {
    if (u.getLevel() != v.getLevel()) {
      throw new IllegalArgumentException("Can only calculate for nodes on same level");
    }
    final List<CallTreeNode> lcau = new ArrayList<>();
    final List<CallTreeNode> lcav = new ArrayList<>();

    CallTreeNode cu = u;
    CallTreeNode cv = v;

    do {
      lcau.add(cu);
      lcav.add(cv);
      cu = cu.getParent();
      cv = cv.getParent();
    } while (!cu.equals(cv));

    return new List[] {lcau, lcav};
  }

}
