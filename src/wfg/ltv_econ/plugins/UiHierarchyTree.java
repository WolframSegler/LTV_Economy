package wfg.ltv_econ.plugins;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.ui.UIPanelAPI;

import rolflectionlib.util.RolfLectionUtil;
import wfg.wrap_ui.ui.Attachments;

public class UiHierarchyTree implements EveryFrameScript {
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    private final JFrame frame = new JFrame("LTV UI Hierarchy");

    public  UiHierarchyTree() {
        frame.add(new JScrollPane(tree));
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setVisible(true);

        JButton refreshButton = new JButton("Refresh Tree");
        refreshButton.addActionListener(e -> updateTree());

        frame.add(refreshButton, BorderLayout.NORTH);
    }

    public void advance(float amount) {}

    public boolean isDone() {
        return false;
    }

    public boolean runWhilePaused() {
        return true;
    }

    private void updateTree() {
        root.removeAllChildren();

        UIPanelAPI masterTab = Attachments.getInteractionCurrentTab();
        if (masterTab == null) {
            masterTab = Attachments.getCurrentTab();
        }
        if (masterTab == null) return;
        final List<?> listChildren = (List<?>) RolfLectionUtil.invokeMethod(
            "getChildrenNonCopy", masterTab);
        if (listChildren == null) return;

        for (Object child : listChildren) {
            if (child instanceof UIPanelAPI panel) {
                root.add(traverseUI(panel));
            }
        }

        treeModel.reload();
    }

    private DefaultMutableTreeNode traverseUI(UIPanelAPI panel) {
        final String className = panel.getClass().getName();

        DefaultMutableTreeNode node = new DefaultMutableTreeNode(className);

        // Recursively add children
        final List<?> children = (List<?>) RolfLectionUtil.invokeMethod(
            "getChildrenNonCopy", panel);
        if (children != null) {
            for (Object child : children) {
                if (child instanceof UIPanelAPI) {
                    node.add(traverseUI((UIPanelAPI) child));
                }
            }
        }

        return node;
    }
}