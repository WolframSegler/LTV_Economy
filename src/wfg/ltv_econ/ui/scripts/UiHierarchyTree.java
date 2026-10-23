package wfg.ltv_econ.ui.scripts;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import rolflectionlib.ui.UiUtil;
import wfg.native_ui.internal.ui.core.UICustomWrapper;
import wfg.native_ui.internal.ui.core.UIVanillaWrapper;
import wfg.native_ui.ui.Attachments;
import wfg.native_ui.ui.MethodFields;
import wfg.native_ui.ui.core.UIContainerAPI;

public final class UiHierarchyTree implements EveryFrameScript {
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
    private final JTree tree = new JTree(treeModel);
    private final JFrame frame = new JFrame("UI Hierarchy");

    public UiHierarchyTree() {
        frame.add(new JScrollPane(tree));
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setVisible(true);

        final JButton refreshButton = new JButton("Refresh Tree");
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

        final UIPanelAPI masterTab = Attachments.getCurrentTab();
        if (masterTab == null) return;
        final List<UIComponentAPI> listChildren = MethodFields.getChildrenNonCopy(masterTab);

        root.setUserObject("[ROOT] - " + masterTab.getClass().getName());

        for (Object child : listChildren) {
            if (child instanceof UIPanelAPI panel) {
                root.add(traverseUI(panel));
            }
        }

        treeModel.reload();
    }

    /** Root is unchecked. To check the root too, use traverseUI(root, root.getParent().getPosition()). */
    private final DefaultMutableTreeNode traverseUI(UIComponentAPI comp) {
        return traverseUI(comp, null);
    }

    private final DefaultMutableTreeNode traverseUI(UIComponentAPI comp, PositionAPI expectedPosParent) {
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(describe(comp, expectedPosParent));

        if (comp instanceof CustomPanelAPI customPanel && customPanel.getPlugin() instanceof UICustomWrapper wrapper) {
            node.add(traverseUI(wrapper.wrapped(), expectedPosParent));
            return node;
        }

        if (comp instanceof UIVanillaWrapper wrapper) {
            node.add(traverseUI(wrapper.wrapped(), expectedPosParent));
            return node;
        }

        if (comp instanceof UIContainerAPI container) {
            final PositionAPI containerPos = container.getPosition();
            for (UIComponentAPI child : container.getChildrenCopy()) {
                node.add(traverseUI(child, containerPos));
            }
            return node;
        }

        if (comp instanceof UIPanelAPI panel && UiUtil.uiComponentClass.isInstance(comp)) {
            final PositionAPI panelPos = panel.getPosition();
            for (UIComponentAPI child : MethodFields.getChildrenNonCopy(panel)) {
                node.add(traverseUI(child, panelPos));
            }
            return node;
        }

        return node;
    }

    private String describe(UIComponentAPI comp, PositionAPI expectedPosParent) {
        final PositionAPI pos = comp.getPosition();

        String prefix = "";
        if (comp instanceof CustomPanelAPI cp && cp.getPlugin() instanceof UICustomWrapper) {
            prefix = "[W_c] ";
        } else if (comp instanceof UIVanillaWrapper) {
            prefix = "[W_v] ";
        }

        final String parentCheck;
        if (expectedPosParent == null) {
            parentCheck = "";
        } else {
            final PositionAPI actual = MethodFields.getPosParent(pos);
            if (actual == expectedPosParent) {
                parentCheck = "";
            } else if (actual == null) {
                parentCheck = " [POS_PARENT_NULL]";
            } else {
                parentCheck = String.format(
                    " [!!POS_PARENT_MISMATCH actual=@[x:%.0f,y:%.0f %.0fx%.0f] expected=@[x:%.0f,y:%.0f %.0fx%.0f]]",
                    actual.getX(), actual.getY(), actual.getWidth(), actual.getHeight(),
                    expectedPosParent.getX(), expectedPosParent.getY(),
                    expectedPosParent.getWidth(), expectedPosParent.getHeight());
            }
        }

        return String.format("%s%s [x:%.0f,y:%.0f %.0fx%.0f]%s",
            prefix, comp.getClass().getSimpleName(),
            pos.getX(), pos.getY(), pos.getWidth(), pos.getHeight(),
            parentCheck);
    }
}