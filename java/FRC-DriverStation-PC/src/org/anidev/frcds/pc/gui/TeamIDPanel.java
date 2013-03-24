package org.anidev.frcds.pc.gui;

import javax.swing.JPanel;
import java.awt.Dimension;
import javax.swing.JLabel;
import java.awt.Component;
import javax.swing.JFormattedTextField;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

public class TeamIDPanel extends JPanel {
	/**
	 * Create the panel.
	 */
	public TeamIDPanel() {
		setSize(new Dimension(170,40));
		GridBagLayout gridBagLayout=new GridBagLayout();
		gridBagLayout.columnWidths=new int[] {0,0};
		gridBagLayout.rowHeights=new int[] {0};
		gridBagLayout.columnWeights=new double[] {0.0,0.0};
		gridBagLayout.rowWeights=new double[] {0.0};
		setLayout(gridBagLayout);

		JLabel teamIDLabel=new JLabel("Team ID");
		teamIDLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		GridBagConstraints gbc_teamIDLabel=new GridBagConstraints();
		gbc_teamIDLabel.gridx=0;
		gbc_teamIDLabel.insets=new Insets(0,0,0,5);
		gbc_teamIDLabel.anchor=GridBagConstraints.WEST;
		gbc_teamIDLabel.gridy=0;
		add(teamIDLabel,gbc_teamIDLabel);

		NumberFormat format=NumberFormat.getIntegerInstance();
		format.setMinimumIntegerDigits(1);
		format.setMaximumIntegerDigits(4);
		format.setGroupingUsed(false);

		JFormattedTextField teamIDField=new JFormattedTextField(format);
		teamIDField.setColumns(4);
		GridBagConstraints gbc_teamIDField=new GridBagConstraints();
		gbc_teamIDField.weightx=1.0;
		gbc_teamIDField.fill=GridBagConstraints.HORIZONTAL;
		gbc_teamIDField.gridx=1;
		gbc_teamIDField.gridy=0;

		teamIDField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				KeyboardFocusManager.getCurrentKeyboardFocusManager()
						.clearGlobalFocusOwner();
			}
		});

		add(teamIDField,gbc_teamIDField);
	}
}
