package edu.harvard.pass.orbiter.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSplitPane;

import edu.harvard.pass.PNode;
import edu.harvard.pass.filter.AncestryFilter;
import edu.harvard.util.filter.Filter;
import edu.harvard.util.filter.FilterFactory;
import edu.harvard.util.filter.FilterSet;
import edu.harvard.util.filter.SwitchableFilter;
import edu.harvard.util.gui.FilterListPanel;


/**
 * Node filter panel
 * 
 * @author Peter Macko
 */
@SuppressWarnings("serial")
public class NodeFilterPanel extends JPanel {
	
	private String title;
	private FilterFactory<PNode> filterFactory;
	private AncestryFilter ancestryFilter;
	
	private FilterSet<PNode> nodeFilterSet;
	private SwitchableFilter<PNode> nodeFilter;
	
	private FilterSet<PNode> stoppingConditionFilterSet;
	private SwitchableFilter<PNode> stoppingConditionFilter;
	
	private Listener listener;
	private JLabel titleLabel;
	private JSplitPane splitPane;
	
	private JPanel nodeFilterPanel;
	private JPanel nodeFilterTopPanel;
	private JCheckBox nodeFilterCheckBox;
	private JRadioButton nodeFilterAndRadioButton;
	private JRadioButton nodeFilterOrRadioButton;
	private FilterListPanel<PNode> nodeFilterListPanel;
	
	private JPanel stoppingConditionFilterPanel;
	private JPanel stoppingConditionFilterTopPanel;
	private JCheckBox stoppingConditionFilterCheckBox;	
	private JCheckBox stoppingConditionFilterIncludeStoppingNodesCheckBox;	
	private FilterListPanel<PNode> stoppingConditionFilterListPanel;


	/**
	 * Create an instance of class NodeFilterPanel
	 *
	 * @param title the title
	 * @param filterFactory the filter factory
	 */
	public NodeFilterPanel(String title, FilterFactory<PNode> filterFactory) {
		
		this.title = title;
		this.filterFactory = filterFactory;
		this.ancestryFilter = null;
		
		
		// Initialize the filters
		
		nodeFilterSet = new FilterSet<PNode>(FilterSet.Operator.AND);
		nodeFilter = new SwitchableFilter<PNode>(nodeFilterSet);
		
		stoppingConditionFilterSet = new FilterSet<PNode>();
		stoppingConditionFilter = new SwitchableFilter<PNode>(stoppingConditionFilterSet);
		
		
		// Initialize the GUI
		
		setLayout(new BorderLayout());
		this.listener = new Listener();

		if (this.title != null) {
			titleLabel = new JLabel(this.title);
			titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
			add(titleLabel, BorderLayout.NORTH);
		}
		
		
		// The node filter panel
		
		nodeFilterPanel = new JPanel(new BorderLayout());
		
		nodeFilterTopPanel = new JPanel(new GridBagLayout());
		nodeFilterTopPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		nodeFilterTopPanel.setOpaque(true);
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		
		int gridy = 0;
		
		JLabel nodeFilterLabel = new JLabel("Node Filter");
		nodeFilterLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		c.gridx = 0;
		c.gridy = gridy;
		c.weightx = 1;
		c.gridwidth = 3;
		nodeFilterTopPanel.add(nodeFilterLabel, c);
		c.weightx = 0;
		c.gridwidth = 1;

		gridy++;
	
		nodeFilterCheckBox = new JCheckBox("Enabled", nodeFilter.isEnabled());
		nodeFilterCheckBox.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		nodeFilterCheckBox.addActionListener(listener);
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		nodeFilterTopPanel.add(nodeFilterCheckBox, c);
		c.gridwidth = 1;

		gridy++;
		
		nodeFilterAndRadioButton = new JRadioButton("AND");
		nodeFilterAndRadioButton.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 25));
		nodeFilterAndRadioButton.addActionListener(listener);
		nodeFilterAndRadioButton.setSelected(nodeFilterSet.getOperator() == FilterSet.Operator.AND);
		c.gridx = 0;
		c.gridy = gridy;
		nodeFilterTopPanel.add(nodeFilterAndRadioButton, c);
		
		nodeFilterOrRadioButton = new JRadioButton("OR");
		nodeFilterOrRadioButton.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		nodeFilterOrRadioButton.addActionListener(listener);
		nodeFilterOrRadioButton.setSelected(nodeFilterSet.getOperator() == FilterSet.Operator.OR);
		c.gridx = 1;
		c.gridy = gridy;
		nodeFilterTopPanel.add(nodeFilterOrRadioButton, c);
		
		ButtonGroup nodeFilterAndOrButtonGroup = new ButtonGroup();
		nodeFilterAndOrButtonGroup.add(nodeFilterAndRadioButton);
		nodeFilterAndOrButtonGroup.add(nodeFilterOrRadioButton);

		gridy++;
		
		nodeFilterPanel.add(nodeFilterTopPanel, BorderLayout.NORTH);

		nodeFilterListPanel = new FilterListPanel<PNode>(null, nodeFilterSet, this.filterFactory);
		nodeFilterPanel.add(nodeFilterListPanel, BorderLayout.CENTER);
		
		
		// The stopping condition filter panel
		
		stoppingConditionFilterPanel = new JPanel(new BorderLayout());
		
		stoppingConditionFilterTopPanel = new JPanel(new GridBagLayout());
		stoppingConditionFilterTopPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		stoppingConditionFilterTopPanel.setOpaque(true);
		
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		
		gridy = 0;
		
		JLabel stoppingConditionFilterLabel = new JLabel("Query Stopping Condition Filter");
		stoppingConditionFilterLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		c.gridx = 0;
		c.gridy = gridy;
		c.weightx = 1;
		c.gridwidth = 3;
		stoppingConditionFilterTopPanel.add(stoppingConditionFilterLabel, c);
		c.weightx = 0;
		c.gridwidth = 1;

		gridy++;
		
		stoppingConditionFilterCheckBox = new JCheckBox("Enabled", stoppingConditionFilter.isEnabled());
		stoppingConditionFilterCheckBox.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		stoppingConditionFilterCheckBox.addActionListener(listener);
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		stoppingConditionFilterTopPanel.add(stoppingConditionFilterCheckBox, c);
		c.gridwidth = 1;

		gridy++;
		
		stoppingConditionFilterIncludeStoppingNodesCheckBox = new JCheckBox("Include stopping nodes", true);
		stoppingConditionFilterIncludeStoppingNodesCheckBox.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		stoppingConditionFilterIncludeStoppingNodesCheckBox.addActionListener(listener);
		c.gridx = 0;
		c.gridy = gridy;
		c.gridwidth = 3;
		stoppingConditionFilterTopPanel.add(stoppingConditionFilterIncludeStoppingNodesCheckBox, c);
		c.gridwidth = 1;

		gridy++;
		
		stoppingConditionFilterPanel.add(stoppingConditionFilterTopPanel, BorderLayout.NORTH);

		stoppingConditionFilterListPanel = new FilterListPanel<PNode>(null, stoppingConditionFilterSet, this.filterFactory);
		stoppingConditionFilterPanel.add(stoppingConditionFilterListPanel, BorderLayout.CENTER);
		
		
		// The split pane
		
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, nodeFilterPanel, stoppingConditionFilterPanel);
		splitPane.setDividerLocation(250);
		splitPane.setResizeWeight(0.5);
		splitPane.setOneTouchExpandable(true);
		
		setPreferredSize(new Dimension(200, 400));
		add(splitPane, BorderLayout.CENTER);
	}
	
	
	/**
	 * Get the node filter
	 * 
	 * @return the node filter
	 */
	public Filter<PNode> getNodeFilter() {
		return nodeFilter;
	}
	
	
	/**
	 * Get the query stopping condition filter
	 * 
	 * @return the filter
	 */
	public Filter<PNode> getStoppingConditionFilter() {
		return nodeFilter;
	}
	
	
	/**
	 * Clear all filter panels
	 */
	public void clear() {
		nodeFilterListPanel.clear();
		stoppingConditionFilterListPanel.clear();
	} 
	
	
	/**
	 * Set the ancestry filter
	 * 
	 * @param filter the new ancestry filter (null to clear)
	 */
	public void setAncestryFilter(AncestryFilter ancestryFilter) {
		
		if (this.ancestryFilter != null) {
			this.ancestryFilter.setTraversalConditionFilter(null);
		}
		
		this.ancestryFilter = ancestryFilter;
		
		if (this.ancestryFilter != null) {
			this.ancestryFilter.setTraversalConditionFilter(stoppingConditionFilter);
			this.ancestryFilter.setIncludingStoppingNodes(
					stoppingConditionFilterIncludeStoppingNodesCheckBox.isSelected());
		}
	}
	
	
	/**
	 * The listener
	 */
	private class Listener implements ActionListener {
		
		/**
		 * Action event handler
		 * 
		 * @param e the event
		 */
		@Override
		public void actionPerformed(ActionEvent e) {

			// Enable/Disable
			
			if (e.getSource() == nodeFilterCheckBox) {
				nodeFilter.setEnabled(nodeFilterCheckBox.isSelected());
			}
			
			if (e.getSource() == stoppingConditionFilterCheckBox) {
				stoppingConditionFilter.setEnabled(stoppingConditionFilterCheckBox.isSelected());
			}
			
			
			// And/Or
			
			if (e.getSource() == nodeFilterAndRadioButton || e.getSource() == nodeFilterOrRadioButton) {
				nodeFilterSet.setOperator(nodeFilterAndRadioButton.isSelected()
						? FilterSet.Operator.AND : FilterSet.Operator.OR);
			}
			
			
			// Include stopping nodes
			
			if (e.getSource() == stoppingConditionFilterIncludeStoppingNodesCheckBox) {
				if (ancestryFilter != null) {
					ancestryFilter.setIncludingStoppingNodes(
							stoppingConditionFilterIncludeStoppingNodesCheckBox.isSelected());
				}
			}
		}		
	}
}
